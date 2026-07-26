#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Levanta los servicios locales con Docker Compose.
.DESCRIPTION
  Usa docker-compose.local.yml con --env-file .env.local.
  Servicios: postgres, backend-java, frontend-react.
  Opcional: --profile whatsapp, --profile monitoring, --profile https.
.PARAMETER Profile
  Perfil opcional de Docker Compose (whatsapp, monitoring, https).
.PARAMETER Build
  Reconstruye imagenes antes de levantar.
.PARAMETER Detach
  Modo detached (background). Default: $true.
.EXAMPLE
  .\scripts\local-start.ps1
  .\scripts\local-start.ps1 -Profile whatsapp
  .\scripts\local-start.ps1 -Build
#>

param(
  [string]$Profile = "",
  [switch]$Build,
  [switch]$Detach = $true
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$EnvFile = Join-Path $ROOT ".env.local"
$ErrorActionPreference = 'Stop'

# ------------------------------------------------------------------------
# Restaurar secretos desde Windows Credential Manager
# ------------------------------------------------------------------------
$RestoreScript = Join-Path $PSScriptRoot "restore-local-secrets.ps1"
if (Test-Path $RestoreScript) {
  Write-Host "=== Restaurando secretos desde Windows Credential Manager ===" -ForegroundColor Cyan
  & $RestoreScript
  if ($LASTEXITCODE -ne 0) {
    Write-Warn "Algunos secretos no estan en Credential Manager."
    Write-Host "Ejecuta .\scripts\store-local-secrets.ps1 para guardarlos" -ForegroundColor Yellow
  }
} else {
  Write-Warn "restore-local-secrets.ps1 no encontrado, secretos no seran restaurados"
}
# ------------------------------------------------------------------------

if (-not (Test-Path $ComposeFile)) {
  Write-Error "No se encuentra $ComposeFile"
  exit 1
}

$cmd = "docker compose"

if (Test-Path $EnvFile) {
  $cmd += " --env-file `"$EnvFile`""
} else {
  Write-Warn ".env.local no encontrado, usando defaults"
}

$cmd += " -f `"$ComposeFile`""

if ($Profile) {
  $cmd += " --profile $Profile"
}

if ($Build) {
  $cmd += " up --build"
} else {
  $cmd += " up"
}

if ($Detach) {
  $cmd += " -d"
}

Write-Host "=== Levantando servicios ===" -ForegroundColor Cyan
Write-Host "  Comando: $cmd" -ForegroundColor Gray

Invoke-Expression $cmd

if ($LASTEXITCODE -eq 0) {
  Write-Host ""
  Write-Host "  Servicios:" -ForegroundColor Cyan
  docker compose -f $ComposeFile ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
  Write-Host ""
  Write-Host "  Frontend: http://localhost:5173" -ForegroundColor Green
  Write-Host "  Backend:  http://localhost:8080" -ForegroundColor Green
  Write-Host "  API Docs: http://localhost:8080/swagger-ui/index.html" -ForegroundColor Green
} else {
  Write-Error "Error al levantar servicios (exit code: $LASTEXITCODE)"
  exit 1
}
