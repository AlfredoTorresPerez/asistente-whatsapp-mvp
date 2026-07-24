#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Reset completo: limpia artefactos, reconstruye y levanta servicios.
.DESCRIPTION
  Ejecuta en orden:
    1. Detiene servicios Docker (con -v: borra volumenes)
    2. Limpia artefactos locales (node_modules, target, dist, etc.)
    3. Reinstala dependencias
    4. Reconstruye backend + frontend
    5. Levanta servicios con Docker
.PARAMETER CleanDockerVolumes
  Incluye limpieza de volumenes Docker (borra base de datos).
.PARAMETER Force
  Omite confirmaciones.
.EXAMPLE
  .\scripts\local-reset.ps1
  .\scripts\local-reset.ps1 -CleanDockerVolumes
#>

param(
  [switch]$CleanDockerVolumes,
  [switch]$Force
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ErrorActionPreference = 'Stop'

function Confirm-Step {
  param([string]$Message)
  if ($Force) { return $true }
  $response = Read-Host "$Message (s/N)"
  return $response -eq 's'
}

Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Yellow
Write-Host "║  RESET COMPLETO DEL ENTORNO LOCAL        ║" -ForegroundColor Yellow
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Yellow
Write-Host ""

if (-not (Confirm-Step "Confirmas el reset completo?")) {
  Write-Host "Reset cancelado." -ForegroundColor Yellow
  exit 0
}

# ── 1. Detener Docker ──────────────────────────────────────
Write-Host "`n>>> 1/5 - Deteniendo servicios Docker..." -ForegroundColor Cyan
$composeFile = Join-Path $ROOT "docker-compose.local.yml"
$downCmd = "docker compose -f `"$composeFile`" down"
if ($CleanDockerVolumes) {
  $downCmd += " -v"
}
Invoke-Expression $downCmd
Write-Host "  [OK] Servicios detenidos" -ForegroundColor Green

# ── 2. Limpiar artefactos ──────────────────────────────────
Write-Host "`n>>> 2/5 - Limpiando artefactos locales..." -ForegroundColor Cyan
& "$ROOT\scripts\clean-local.ps1" @($CleanDockerVolumes ? '-CleanDockerVolumes' : @()) @($Force ? '-Force' : @())
Write-Host "  [OK] Artefactos limpios" -ForegroundColor Green

# ── 3. Setup (instalar + compilar + build) ─────────────────
Write-Host "`n>>> 3/5 - Reinstalando y reconstruyendo..." -ForegroundColor Cyan
& "$ROOT\scripts\local-setup.ps1" @($Force ? '-Force' : @())
if ($LASTEXITCODE -ne 0) {
  Write-Error "Setup fallo. Revisa los errores arriba."
  exit 1
}
Write-Host "  [OK] Setup completado" -ForegroundColor Green

# ── 4. Levantar Docker ─────────────────────────────────────
Write-Host "`n>>> 4/5 - Levantando servicios Docker..." -ForegroundColor Cyan
& "$ROOT\scripts\local-start.ps1"
if ($LASTEXITCODE -ne 0) {
  Write-Error "Error al levantar servicios."
  exit 1
}
Write-Host "  [OK] Servicios levantados" -ForegroundColor Green

# ── 5. Verificar salud ─────────────────────────────────────
Write-Host "`n>>> 5/5 - Verificando salud de servicios..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
& "$ROOT\scripts\local-verify.ps1"
if ($LASTEXITCODE -ne 0) {
  Write-Warning "Algunas verificaciones fallaron. Revisa los detalles arriba."
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  RESET COMPLETADO                         ║" -ForegroundColor Green
Write-Host "║  http://localhost:5173                    ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
