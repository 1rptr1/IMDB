$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
param(
  [string]$Db = 'imdb',
  [string]$Query,
  [string]$File
)
$env:PGPASSWORD = 'postgres'
if ([string]::IsNullOrWhiteSpace($Query) -and [string]::IsNullOrWhiteSpace($File)) {
  docker exec -it imdb-postgres psql -U postgres -d $Db
  exit $LASTEXITCODE
}
if (-not [string]::IsNullOrWhiteSpace($Query)) {
  docker exec imdb-postgres psql -U postgres -d $Db -v ON_ERROR_STOP=1 -c $Query
  exit $LASTEXITCODE
}
if (-not (Test-Path $File)) { throw "File not found: $File" }
Get-Content -Raw -Path $File | docker exec -i imdb-postgres psql -U postgres -d $Db -v ON_ERROR_STOP=1 -f - | Out-Host
exit $LASTEXITCODE
