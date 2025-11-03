$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Write-Host "Stopping and removing containers and volumes..."
docker compose down -v | Out-Host

Write-Host "Starting fresh and reloading data..."
& "$PSScriptRoot\start.ps1"
exit $LASTEXITCODE
