#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Levanta el stack de observabilidad local (Prometheus, Loki, Tempo, Alloy, Grafana).
.DESCRIPTION
  Usa docker-compose.local.yml con --env-file .env.local y --profile observability.
  Verifica que GRAFANA_ADMIN_PASSWORD exista en .env.local antes de levantar.
.PARAMETER Build
  Reconstruye imagenes antes de levantar.
.PARAMETER Services
  Servicios a levantar (default: prometheus loki tempo alloy grafana).
.EXAMPLE
  .\scripts\observability-start.ps1
  .\scripts\observability-start.ps1 -Build
#>

param(
  [switch]$Build,
  [string]$Services = "prometheus loki tempo alloy grafana"
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$EnvFile = Join-Path $ROOT ".env.local"
$ErrorActionPreference = 'Stop'

if (-not (Test-Path $ComposeFile)) {
  Write-Error "No se encuentra $ComposeFile"
  exit 1
}

# ── Validar GRAFANA_ADMIN_PASSWORD ───────────────────────────
$grafanaPassword = ""
if (Test-Path $EnvFile) {
  $grafanaPassword = (Get-Content $EnvFile | Where-Object { $_ -match '^GRAFANA_ADMIN_PASSWORD=' }) -replace '^GRAFANA_ADMIN_PASSWORD=', ''
}
if ([string]::IsNullOrWhiteSpace($grafanaPassword)) {
  Write-Error "GRAFANA_ADMIN_PASSWORD no esta definido en .env.local. Agregalo antes de levantar el stack."
  exit 1
}

$cmd = "docker compose --env-file `"$EnvFile`" -f `"$ComposeFile`" --profile observability up"
if ($Build) {
  $cmd += " --build"
}
$cmd += " -d $Services"

Write-Host "=== Levantando stack de observabilidad ===" -ForegroundColor Cyan
Write-Host "  Comando: $cmd" -ForegroundColor Gray
Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
  Write-Host ""
  Write-Host "  Servicios:" -ForegroundColor Cyan
  docker compose -f $ComposeFile ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
  Write-Host ""
  Write-Host "  Grafana:    http://localhost:3000 (admin / GRAFANA_ADMIN_PASSWORD)" -ForegroundColor Green
  Write-Host "  Prometheus: http://localhost:9090" -ForegroundColor Green
  Write-Host "  Loki:       http://localhost:3100" -ForegroundColor Green
  Write-Host "  Tempo:      http://localhost:3200" -ForegroundColor Green
  Write-Host "  Verificacion: .\scripts\observability-verify.ps1" -ForegroundColor Green
} else {
  Write-Error "Error al levantar el stack (exit code: $LASTEXITCODE)"
  exit 1
}
