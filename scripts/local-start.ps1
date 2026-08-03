#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Levanta los servicios locales con Docker Compose (comando oficial de arranque).
.DESCRIPTION
  Usa docker-compose.local.yml con --env-file .env.local.
  Servicios core: postgres, backend-java, frontend-react, mailpit.
  Perfiles opcionales (docker-compose.local.yml):
    observability  - Prometheus, Loki, Tempo, Alloy, Grafana
    monitoring     - alias legacy de observability
    backup         - backup-sidecar (cron diario de pg_dump)
    public-link    - tunel publico HTTPS (cloudflared)
    https          - Caddy (HTTPS local autosigned)
  Pre-flight: valida docker, el archivo compose y los perfiles antes de levantar.
.PARAMETER Profile
  Perfiles opcionales (separados por coma): observability, monitoring, backup, public-link, https o "all".
.PARAMETER Build
  Reconstruye imagenes antes de levantar.
.PARAMETER Detach
  Modo detached (background). Default: $true.
.PARAMETER Verify
  Ejecuta local-verify.ps1 al terminar (health + smoke test).
.EXAMPLE
  .\scripts\local-start.ps1
  .\scripts\local-start.ps1 -Build
  .\scripts\local-start.ps1 -Profile observability,backup
  .\scripts\local-start.ps1 -Profile all -Verify
#>

param(
  [string]$Profile = "",
  [switch]$Build,
  [switch]$Detach = $true,
  [switch]$Verify
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$EnvFile = Join-Path $ROOT ".env.local"
$ErrorActionPreference = 'Stop'

$KnownProfiles = @('observability', 'monitoring', 'backup', 'public-link', 'https')

function Set-EnvValueFromFile {
  param([string]$Key, [string]$FilePath)

  if (-not (Test-Path $FilePath)) {
    return
  }

  $pattern = "^$([regex]::Escape($Key))=(.*)$"
  foreach ($line in Get-Content -Path $FilePath) {
    if ($line -match $pattern) {
      Set-Item -Path "env:$Key" -Value $Matches[1]
      return
    }
  }
}

# ------------------------------------------------------------------------
# Pre-flight: docker disponible
# ------------------------------------------------------------------------
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Write-Error "docker no esta instalado o no esta en el PATH."
  exit 1
}

# ------------------------------------------------------------------------
# Pre-flight: archivo compose
# ------------------------------------------------------------------------
if (-not (Test-Path $ComposeFile)) {
  Write-Error "No se encuentra $ComposeFile"
  exit 1
}

# ------------------------------------------------------------------------
# Pre-flight: validar perfiles solicitados
# ------------------------------------------------------------------------
$ProfileList = @()
if (-not [string]::IsNullOrWhiteSpace($Profile)) {
  $ProfileList = @($Profile.Split(',') | ForEach-Object { $_.Trim() }) | Where-Object { $_ -ne "" }
  if ($ProfileList -contains 'all') {
    $ProfileList = @('observability', 'backup', 'public-link', 'https')
  }
  $unknown = $ProfileList | Where-Object { $_ -notin $KnownProfiles }
  if ($unknown) {
    Write-Error "Perfil(es) invalido(s): $($unknown -join ', '). Perfiles validos: $($KnownProfiles -join ', '), all"
    exit 1
  }
  Write-Host "=== Perfiles: $($ProfileList -join ', ') ===" -ForegroundColor Cyan
}

# ------------------------------------------------------------------------
# Pre-flight: docker compose config valido
# Nota: en Compose v5 los --profile deben ir ANTES del subcomando config
# ------------------------------------------------------------------------
$configArgs = @('compose', '--env-file', $EnvFile, '-f', $ComposeFile)
foreach ($p in $ProfileList) { $configArgs += '--profile'; $configArgs += $p }
$configArgs += 'config'
$configArgs += '--quiet'
& docker @configArgs 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
  Write-Error "docker compose config fallo. Revisa $ComposeFile y .env.local"
  exit 1
}
Write-Host "  [OK] docker compose config valido (perfiles: $($ProfileList -join ', '))" -ForegroundColor Green

# ------------------------------------------------------------------------
# Restaurar secretos desde Windows Credential Manager
# ------------------------------------------------------------------------
$RestoreScript = Join-Path $PSScriptRoot "restore-local-secrets.ps1"
if (Test-Path $RestoreScript) {
  Write-Host "=== Restaurando secretos desde Windows Credential Manager ===" -ForegroundColor Cyan
  $global:LASTEXITCODE = 0
  & $RestoreScript
  if ($LASTEXITCODE -ne 0) {
    Write-Warning "Algunos secretos no estan en Credential Manager."
    Write-Host "Ejecuta .\scripts\store-local-secrets.ps1 para guardarlos" -ForegroundColor Yellow
  }
} else {
  Write-Warning "restore-local-secrets.ps1 no encontrado, secretos no seran restaurados"
}
# ------------------------------------------------------------------------

# Docker Compose da precedencia a variables ya presentes en el shell por sobre
# --env-file. Sincroniza valores no secretos que cambian por tunel/perfil para
# evitar que un Env: antiguo reemplace lo definido en .env.local.
foreach ($key in @(
  "APP_WHATSAPP_CLOUD_API_WEBHOOK_PUBLIC_URL",
  "APP_FRONTEND_PUBLIC_BASE_URL",
  "APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL",
  "APP_BOOKING_RESCHEDULE_PUBLIC_BASE_URL",
  "APP_BOOKING_CANCELLATION_PUBLIC_BASE_URL",
  "APP_BOOKING_PAYMENT_CHECKOUT_PUBLIC_BASE_URL",
  "APP_AI_AGENTS_ENABLED",
  "APP_AI_AGENTS_AUTO_REPLY_ENABLED",
  "APP_AI_AGENTS_AUDIT_ENABLED",
  "APP_AI_AGENTS_SAFE_MODE_ENABLED",
  "APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED",
  "APP_OPENAI_ENABLED"
)) {
  Set-EnvValueFromFile -Key $key -FilePath $EnvFile
}

# ------------------------------------------------------------------------
# Advertencia: GRAFANA_ADMIN_PASSWORD requerido con perfil observability
# ------------------------------------------------------------------------
if ($ProfileList -contains 'observability' -or $ProfileList -contains 'monitoring') {
  $grafanaPassword = ""
  if (Test-Path $EnvFile) {
    $grafanaPassword = (Get-Content $EnvFile | Where-Object { $_ -match '^GRAFANA_ADMIN_PASSWORD=' }) -replace '^GRAFANA_ADMIN_PASSWORD=', ''
  }
  if ([string]::IsNullOrWhiteSpace($grafanaPassword)) {
    Write-Warning "GRAFANA_ADMIN_PASSWORD no esta definido en .env.local. Grafana quedara con password vacio."
  }
}

# ------------------------------------------------------------------------
# Levantar
# ------------------------------------------------------------------------
$cmd = "docker compose"

if (Test-Path $EnvFile) {
  $cmd += " --env-file `"$EnvFile`""
} else {
  Write-Warning ".env.local no encontrado, usando defaults"
}

$cmd += " -f `"$ComposeFile`""

foreach ($p in $ProfileList) {
  $cmd += " --profile $p"
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

if ($LASTEXITCODE -ne 0) {
  Write-Error "Error al levantar servicios (exit code: $LASTEXITCODE)"
  exit 1
}

Write-Host ""
Write-Host "  Servicios:" -ForegroundColor Cyan
docker compose -f $ComposeFile ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
Write-Host ""
Write-Host "  Frontend: http://localhost:5173" -ForegroundColor Green
Write-Host "  Backend:  http://localhost:8080" -ForegroundColor Green
Write-Host "  API Docs: http://localhost:8080/swagger-ui/index.html" -ForegroundColor Green

if ($Verify) {
  Write-Host ""
  & "$PSScriptRoot\local-verify.ps1"
  if ($LASTEXITCODE -ne 0) {
    exit 1
  }
}

exit 0
