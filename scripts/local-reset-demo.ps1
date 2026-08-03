#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Regenera los datos demo del ambiente local desde cero.
.DESCRIPTION
  Reconstruye la base de datos local (postgres) para que las migraciones
  Flyway vuelvan a aplicar los seeds demo y el LocalDataInitializer refresque
  las fechas de las reservas de ejemplo. No toca codigo, node_modules, target
  ni dist. No borra backups ni observabilidad.
  Requiere confirmacion: elimina el volumen de postgres (datos locales).
.PARAMETER Force
  Omite confirmaciones.
.EXAMPLE
  .\scripts\local-reset-demo.ps1
  .\scripts\local-reset-demo.ps1 -Force
#>

param(
  [switch]$Force
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$EnvFile = Join-Path $ROOT ".env.local"
$VolumeName = "asistente_postgres-data"
$ErrorActionPreference = 'Stop'

function Confirm-Step {
  param([string]$Message)
  if ($Force) { return $true }
  $response = Read-Host "$Message (s/N)"
  return $response -eq 's'
}

Write-Host "==============================================" -ForegroundColor Yellow
Write-Host "  REGENERACION DE DATOS DEMO LOCALES" -ForegroundColor Yellow
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "Esto elimina el volumen $VolumeName (base de datos local) y la" -ForegroundColor Red
Write-Host "recrea aplicando las migraciones Flyway (seeds demo) y el" -ForegroundColor Red
Write-Host "LocalDataInitializer (fechas de reservas de ejemplo)." -ForegroundColor Red
Write-Host "No se eliminan: codigo, node_modules, target, dist, backups ni" -ForegroundColor Red
Write-Host "observabilidad." -ForegroundColor Red
Write-Host ""

if (-not (Confirm-Step "Confirmas la regeneracion de datos demo?")) {
  Write-Host "Regeneracion cancelada." -ForegroundColor Yellow
  exit 0
}

if (-not (Test-Path $ComposeFile)) {
  Write-Error "No se encuentra $ComposeFile"
  exit 1
}

# ── 1. Detener postgres y backend ───────────────────────────
Write-Host "`n>>> 1/5 - Deteniendo postgres y backend..." -ForegroundColor Cyan
docker compose --env-file $EnvFile -f $ComposeFile stop postgres backend-java 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
  Write-Warning "No se pudieron detener los servicios (pueden no estar corriendo)."
}
Write-Host "  [OK] Servicios detenidos" -ForegroundColor Green

# ── 2. Eliminar contenedor postgres ─────────────────────────
Write-Host "`n>>> 2/5 - Eliminando contenedor postgres..." -ForegroundColor Cyan
docker compose --env-file $EnvFile -f $ComposeFile rm -f postgres 2>&1 | Out-Null
Write-Host "  [OK] Contenedor postgres eliminado" -ForegroundColor Green

# ── 3. Eliminar volumen de datos ────────────────────────────
Write-Host "`n>>> 3/5 - Eliminando volumen $VolumeName..." -ForegroundColor Cyan
docker volume rm $VolumeName 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 1) {
  Write-Warning "El volumen pudo no existir (base nueva)."
}
Write-Host "  [OK] Volumen eliminado (o inexistente)" -ForegroundColor Green

# ── 4. Levantar postgres y esperar healthy ──────────────────
Write-Host "`n>>> 4/5 - Levantando postgres (Flyway aplicara seeds al iniciar backend)..." -ForegroundColor Cyan
docker compose --env-file $EnvFile -f $ComposeFile up -d postgres 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
  Write-Error "No se pudo levantar postgres"
  exit 1
}

$deadline = (Get-Date).AddSeconds(90)
do {
  Start-Sleep -Seconds 3
  $health = docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' asistente-postgres 2>$null
} while ($health -ne "healthy" -and $health -ne "none" -and (Get-Date) -lt $deadline)
Write-Host "  [OK] postgres listo (health: $health)" -ForegroundColor Green

# ── 5. Reiniciar backend (Flyway + LocalDataInitializer) ───
Write-Host "`n>>> 5/5 - Reiniciando backend (migraciones + datos demo)..." -ForegroundColor Cyan
docker compose --env-file $EnvFile -f $ComposeFile restart backend-java 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
  Write-Error "No se pudo reiniciar backend"
  exit 1
}

$deadline = (Get-Date).AddSeconds(180)
do {
  Start-Sleep -Seconds 5
  $health = docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' asistente-backend 2>$null
} while ($health -ne "healthy" -and (Get-Date) -lt $deadline)

if ($health -ne "healthy") {
  Write-Warning "El backend no llego a healthy en 180s. Revisa docker logs asistente-backend"
  Write-Warning "Accion: ejecuta .\scripts\local-verify.ps1 o docker logs asistente-backend"
} else {
  Write-Host "  [OK] backend healthy (datos demo regenerados)" -ForegroundColor Green
}

# ── Verificar ────────────────────────────────────────────────
Write-Host ""
Write-Host "=== Verificando el entorno ===" -ForegroundColor Cyan
& "$PSScriptRoot\local-verify.ps1"
if ($LASTEXITCODE -ne 0) {
  Write-Warning "Algunas verificaciones fallaron. Accion: ejecuta .\scripts\diagnose-local.ps1"
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  DATOS DEMO REGENERADOS" -ForegroundColor Green
Write-Host "  Frontend: http://localhost:5173" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
exit 0
