#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Detiene los servicios locales de Docker Compose (comando oficial de detencion).
.DESCRIPTION
  Detiene contenedores sin eliminar volumenes (datos persistentes).
  Incluye los perfiles opcionales (observability, backup, public-link, https)
  para que ningun contenedor del compose local quede corriendo.
  Usa --env-file .env.local si existe.
.PARAMETER Volumes
  Elimina tambien los volumenes (borra la base de datos).
.EXAMPLE
  .\scripts\local-stop.ps1
  .\scripts\local-stop.ps1 -Volumes
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

$cmd += " -f `"$ComposeFile`""

# Todos los perfiles opcionales: down debe alcanzar tambien sus contenedores
foreach ($profile in @('observability', 'monitoring', 'backup', 'public-link', 'https')) {
  $cmd += " --profile $profile"
}

$cmd += " down"

if ($Volumes) {
  $cmd += " -v"
  Write-Host "=== Deteniendo servicios y eliminando volumenes ===" -ForegroundColor Yellow
  Write-Host "ADVERTENCIA: Se eliminara la base de datos y todos sus datos." -ForegroundColor Red
} else {
  Write-Host "=== Deteniendo servicios (volumenes preservados) ===" -ForegroundColor Cyan
}

Write-Host "  Comando: $cmd" -ForegroundColor Gray
Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
  Write-Host "  [OK] Servicios detenidos" -ForegroundColor Green
} else {
  Write-Error "Error al detener servicios (exit code: $LASTEXITCODE)"
  exit 1
}
