$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Write-Host "Stopping imdb-postgres container..."
docker stop imdb-postgres | Out-Host
