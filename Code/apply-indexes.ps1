$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$indexes = Join-Path $scriptDir 'scripts\indexes.sql'
if (-not (Test-Path $indexes)) { throw "Indexes file not found: $indexes" }

& "$scriptDir\query.ps1" -Db imdb -File $indexes
exit $LASTEXITCODE
