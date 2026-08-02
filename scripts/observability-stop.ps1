#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Detiene el stack de observabilidad local (Prometheus, Loki, Tempo, Alloy, Grafana).
.DESCRIPTION
  Detiene los contenedores de observabilidad sin eliminar volumenes (datos persistentes).
.PARAMETER Volumes
  Elimina tambien los volumenes de datos de observabilidad.
.EXAMPLE
  .\scripts\observability-stop.ps1
  .\scripts\observability-stop.ps1 -Volumes
#>

param(
  [switch]$Volumes
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$EnvFile = Join-Path $ROOT ".env.local"
$ErrorActionPreference = 'Stop'

if (-not (Test-Path $ComposeFile)) {
  Write-Error "No se encuentra $ComposeFile"
  exit 1
}

$cmd = "docker compose"
if (Test-Path $EnvFile) {
  $cmd += " --env-file `"$EnvFile`""
}
$cmd += " -f `"$ComposeFile`" --profile observability"

if ($Volumes) {
  $cmd += " down -v"
  Write-Host "=== Deteniendo observabilidad y eliminando volumenes ===" -ForegroundColor Yellow
  Write-Host "ADVERTENCIA: Se eliminaran los datos de Prometheus, Loki, Tempo y Grafana." -ForegroundColor Red
} else {
  $cmd += " stop prometheus loki tempo alloy grafana"
  Write-Host "=== Deteniendo observabilidad (volumenes preservados) ===" -ForegroundColor Cyan
}

Write-Host "  Comando: $cmd" -ForegroundColor Gray
Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
  Write-Host "  [OK] Observabilidad detenida" -ForegroundColor Green
} else {
  Write-Error "Error al detener observabilidad (exit code: $LASTEXITCODE)"
  exit 1
}
