# Script para ejecutar toda la suite E2E del Asistente de Negocios WhatsApp
# Uso:
#   .\run-all.ps1              # Ejecuta all-chromium
#   .\run-all.ps1 -Smoke      # Solo smoke tests
#   .\run-all.ps1 -All        # Todas las suites

param(
  [switch]$Smoke,
  [switch]$All
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$E2eDir = Split-Path -Parent $ScriptDir
$ProjectDir = Split-Path -Parent $E2eDir

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Asistente de Negocios - Suite E2E" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Directorio: $ProjectDir"
Write-Host ""

Set-Location -LiteralPath $ProjectDir

# Detener frontend container si está corriendo
$container = docker ps --format '{{.Names}}' 2>$null | Select-String 'asistente-whatsapp-frontend'
if ($container) {
  Write-Host "[INFO] Deteniendo contenedor asistente-whatsapp-frontend..." -ForegroundColor Yellow
  docker stop asistente-whatsapp-frontend 2>$null
  Write-Host "[INFO] Contenedor detenido. Playwright usará su propio dev server." -ForegroundColor Yellow
}

$project = if ($Smoke) { 'smoke' } elseif (-not $All) { 'all-chromium' } else { $null }

if ($project) {
  Write-Host "[EXEC] npx playwright test --project=$project" -ForegroundColor Green
  npx playwright test --project=$project
} else {
  Write-Host "[EXEC] npx playwright test" -ForegroundColor Green
  npx playwright test
}

Write-Host ""
Write-Host "[REPORT] Generando reporte consolidado..." -ForegroundColor Green
npx ts-node "$ScriptDir/generate-report.ts"

Write-Host ""
Write-Host "[DONE] Reporte disponible en: e2e/reports/" -ForegroundColor Green
Write-Host "       HTML: e2e/reports/html-report/index.html" -ForegroundColor Green
Write-Host "       JSON: e2e/reports/test-results.json" -ForegroundColor Green
Write-Host "       MD:   e2e/reports/test-status-report.md" -ForegroundColor Green
