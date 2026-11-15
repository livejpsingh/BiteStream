Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Starting BiteStream Observability & Logging Stack       " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

docker compose up -d

Write-Host "`nObservability Stack Started Successfully!" -ForegroundColor Green
Write-Host "  - Grafana:     http://localhost:3000 (admin / admin)" -ForegroundColor Yellow
Write-Host "  - Prometheus:  http://localhost:9090" -ForegroundColor Yellow
Write-Host "  - Loki:        http://localhost:3100" -ForegroundColor Yellow
