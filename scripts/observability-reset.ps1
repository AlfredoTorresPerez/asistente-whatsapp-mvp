#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Reset del stack de observabilidad local: detiene y borra datos (dashboards, series, trazas, logs).
.DESCRIPTION
  Detiene los servicios de observabilidad y elimina los volumenes de datos:
  grafana-data, prometheus-data, loki-data, tempo-data.
  Requiere confirmacion (o -Force). No toca postgres ni el backend.
.PARAMETER Force
  Omite la confirmacion.
.EXAMPLE
  .\scripts\observability-reset.ps1
  .\scripts\observability-reset.ps1 -Force
#>

param(
  [switch]$Force
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$EnvFile = Join-Path $ROOT ".env.local"
$ErrorActionPreference = 'Stop'

if (-not $Force) {
  $response = Read-Host "Se eliminaran los datos de Prometheus, Loki, Tempo y Grafana. Confirmas? (s/N)"
  if ($response -ne 's') {
    Write-Host "Reset cancelado." -ForegroundColor Yellow
    exit 0
  }
}

if (-not (Test-Path $ComposeFile)) {
  Write-Error "No se encuentra $ComposeFile"
  exit 1
}

$cmd = "docker compose"
if (Test-Path $EnvFile) {
  $cmd += " --env-file `"$EnvFile`""
}
$cmd += " -f `"$ComposeFile`" --profile observability down -v"

Write-Host "=== Reset de observabilidad ===" -ForegroundColor Yellow
Write-Host "  Comando: $cmd" -ForegroundColor Gray
Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
  Write-Host "  [OK] Observabilidad reseteada. Levantala de nuevo con:" -ForegroundColor Green
  Write-Host "       .\scripts\observability-start.ps1 -Build" -ForegroundColor Cyan
} else {
  Write-Error "Error en el reset (exit code: $LASTEXITCODE)"
  exit 1
}
