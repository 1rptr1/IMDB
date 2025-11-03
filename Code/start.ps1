# Hardcoded configuration
$DataDir = "d:/IMDB"
$DbHost = "localhost"
$DbPort = 5432
$DbName = "imdb"
$DbUser = "postgres"
$DbPassword = "postgres"

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Push-Location $PSScriptRoot
try {
  $dockerVersion = & docker --version
  $composeVersion = & docker compose version
  & docker compose up -d
  $max = 120
  $i = 0
  while ($i -lt $max) {
    $ok = Test-NetConnection -ComputerName $DbHost -Port $DbPort -WarningAction SilentlyContinue -InformationLevel Quiet
    if ($ok) { break }
    Start-Sleep -Seconds 2
    $i++
  }
  if (-not $ok) { throw "Database port not reachable on $($DbHost):$DbPort" }
  & mvn -q -DskipTests package
  if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
  }
  $jar = Join-Path $PSScriptRoot "target/imdb-sync-0.1.0-shaded.jar"
  if (-not (Test-Path $jar)) {
    $candidates = Get-ChildItem -Path (Join-Path $PSScriptRoot 'target') -Filter 'imdb-sync-*.jar' -ErrorAction SilentlyContinue | Sort-Object Length -Descending
    if ($candidates -and $candidates.Count -ge 1) {
      $jar = $candidates[0].FullName
    } else {
      throw "Expected jar not found: $jar"
    }
  }
  $env:DB_HOST = $DbHost
  $env:DB_PORT = "$DbPort"
  $env:DB_NAME = $DbName
  $env:DB_USER = $DbUser
  $env:DB_PASSWORD = $DbPassword
  if (-not $env:IMDB_DATA_DIR) { $env:IMDB_DATA_DIR = $DataDir }
  $javaArgs = @('-Duser.timezone=UTC', '-jar', $jar)
  & java $javaArgs
  if ($LASTEXITCODE -ne 0) { throw "Java loader failed with exit code $LASTEXITCODE" }
  & "$PSScriptRoot\apply-indexes.ps1"
} finally {
  Pop-Location
}
