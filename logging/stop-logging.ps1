Write-Host "Stopping BiteStream Observability & Logging Stack..." -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

docker compose down

Write-Host "Observability Stack Stopped." -ForegroundColor Green
