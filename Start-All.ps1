param(
  [string]$ApiUrl = "http://localhost:3001",
  [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

Write-Host "== IMDB: Start All ==" -ForegroundColor Cyan
Write-Host "API URL for frontend build:`t$ApiUrl"

# Ensure Docker is available
try {
  docker --version | Out-Null
} catch {
  Write-Error "Docker is not installed or not on PATH. Install Docker Desktop and try again."
  exit 1
}

# Compose build (pass VITE_API_URL to Frontend build)
$composeArgs = @("compose", "build", "--build-arg", "VITE_API_URL=$ApiUrl")
if ($Rebuild) { $composeArgs += "--no-cache" }
Write-Host "Building images..." -ForegroundColor Yellow
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { Write-Error "docker compose build failed"; exit 1 }

# Compose up
Write-Host "Starting services (db, backend, frontend)..." -ForegroundColor Yellow
& docker compose up -d
if ($LASTEXITCODE -ne 0) { Write-Error "docker compose up failed"; exit 1 }

# Determine a host IPv4 address (best-effort)
$ip = try {
  Get-NetIPAddress -AddressFamily IPv4 -PrefixOrigin Dhcp -ErrorAction Stop | Where-Object { $_.IPAddress -notlike '169.*' } | Select-Object -First 1 -ExpandProperty IPAddress
} catch { $null }
if (-not $ip) {
  $ip = try {
    Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop | Where-Object { $_.IPAddress -notlike '169.*' } | Select-Object -First 1 -ExpandProperty IPAddress
  } catch { $null }
}

Write-Host ""; Write-Host "=== Services ===" -ForegroundColor Green
Write-Host "- PostgreSQL:`thost=localhost port=5432" 
Write-Host "- Backend:`t  http://localhost:3001/"
Write-Host "- Frontend:`t http://localhost:8080/"

if ($ip) {
  Write-Host ""; Write-Host "=== External Access (same network) ===" -ForegroundColor Green
  Write-Host "- Backend:`t  http://$ip:3001/"
  Write-Host "- Frontend:`t http://$ip:8080/"
}

Write-Host ""; Write-Host "Done. Use 'docker compose logs -f' to tail logs, 'docker compose down' to stop." -ForegroundColor Cyan
