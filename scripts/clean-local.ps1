#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Limpia artefactos regenerables del proyecto.
.DESCRIPTION
  Elimina node_modules, target, dist, cobertura, cachés, logs y archivos
  temporales. NO elimina datos fuente, migraciones, pruebas ni .env.example.
  Requiere confirmación antes de eliminar volúmenes Docker.
.PARAMETER CleanDockerVolumes
  Incluye limpieza de volúmenes Docker (requiere confirmación adicional).
.PARAMETER Force
  Omite confirmaciones (solo para CI).
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

Write-Host "=== Limpieza de artefactos regenerables ===" -ForegroundColor Cyan
Write-Host ""

# ── Frontend ──────────────────────────────────────────────
$frontendNodeModules = Join-Path $ROOT "frontend-react" "node_modules"
if (Test-Path $frontendNodeModules) {
  if (Confirm-Step "Eliminar node_modules del frontend?") {
    Remove-Item $frontendNodeModules -Recurse -Force
    Write-Host "  [OK] node_modules eliminado" -ForegroundColor Green
  }
}

$frontendDist = Join-Path $ROOT "frontend-react" "dist"
if (Test-Path $frontendDist) {
  Remove-Item $frontendDist -Recurse -Force
  Write-Host "  [OK] dist eliminado" -ForegroundColor Green
}

$frontendCoverage = Join-Path $ROOT "frontend-react" "coverage"
if (Test-Path $frontendCoverage) {
  Remove-Item $frontendCoverage -Recurse -Force
  Write-Host "  [OK] coverage eliminado" -ForegroundColor Green
}

# ── Backend ────────────────────────────────────────────────
$backendTarget = Join-Path $ROOT "backend-java" "target"
if (Test-Path $backendTarget) {
  Remove-Item $backendTarget -Recurse -Force
  Write-Host "  [OK] target eliminado" -ForegroundColor Green
}

# ── Logs ───────────────────────────────────────────────────
Get-ChildItem $ROOT -Filter "*.log" -File | Remove-Item -Force
Write-Host "  [OK] archivos .log eliminados" -ForegroundColor Green

$backendLogs = Join-Path $ROOT "backend-java" "logs"
if (Test-Path $backendLogs) {
  Remove-Item "$backendLogs\*" -Recurse -Force
  Write-Host "  [OK] backend-java/logs limpiados" -ForegroundColor Green
}

# ── Playwright ─────────────────────────────────────────────
$playwrightReport = Join-Path $ROOT "frontend-react" "playwright-report"
if (Test-Path $playwrightReport) {
  Remove-Item $playwrightReport -Recurse -Force
  Write-Host "  [OK] playwright-report eliminado" -ForegroundColor Green
}

$testResults = Join-Path $ROOT "frontend-react" "test-results"
if (Test-Path $testResults) {
  Remove-Item $testResults -Recurse -Force
  Write-Host "  [OK] test-results eliminado" -ForegroundColor Green
}

$e2eScreenshots = Join-Path $ROOT "frontend-react" "e2e" "screenshots"
if (Test-Path $e2eScreenshots) {
  Remove-Item $e2eScreenshots -Recurse -Force
  Write-Host "  [OK] e2e/screenshots eliminado" -ForegroundColor Green
}

# ── Vite ───────────────────────────────────────────────────
$viteCache = Join-Path $ROOT "frontend-react" ".vite"
if (Test-Path $viteCache) {
  Remove-Item $viteCache -Recurse -Force
  Write-Host "  [OK] .vite eliminado" -ForegroundColor Green
}

# ── TypeScript ─────────────────────────────────────────────
Get-ChildItem $ROOT -Filter "*.tsbuildinfo" -File -Recurse | Remove-Item -Force
Write-Host "  [OK] archivos .tsbuildinfo eliminados" -ForegroundColor Green

# ── Docker volumes ─────────────────────────────────────────
if ($CleanDockerVolumes) {
  Write-Host ""
  Write-Host "=== LIMPIEZA DE VOLUMENES DOCKER ===" -ForegroundColor Yellow
  Write-Host "ADVERTENCIA: Esto eliminara la base de datos local y todos sus datos." -ForegroundColor Red
  if (Confirm-Step "Confirmas eliminar los volumenes Docker?") {
    docker compose -p asistente -f (Join-Path $ROOT "docker-compose.local.yml") down -v 2>$null
    Write-Host "  [OK] volumenes Docker eliminados" -ForegroundColor Green
  }
}

# ── Lock files orphan ──────────────────────────────────────
$npmLock = Join-Path $ROOT "frontend-react" "package-lock.json"
if (Test-Path $npmLock) {
  Remove-Item $npmLock -Force
  Write-Host "  [OK] package-lock.json huerfano eliminado" -ForegroundColor Green
}

$yarnLock = Join-Path $ROOT "frontend-react" "yarn.lock"
if (Test-Path $yarnLock) {
  Remove-Item $yarnLock -Force
  Write-Host "  [OK] yarn.lock huerfano eliminado" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Limpieza completada ===" -ForegroundColor Cyan
