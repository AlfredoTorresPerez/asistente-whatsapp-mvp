#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Verifica el estado del entorno local (health + smoke test).
.DESCRIPTION
  Comprueba:
    - Contenedores core saludables (postgres, backend, frontend, mailpit)
    - Contenedores de perfiles opcionales si estan corriendo (observability,
      backup, public-link, https)
    - Backend /actuator/health = UP
    - Frontend HTTP 200
    - Login y API basica
.PARAMETER TimeoutSeconds
  Timeout por cada healthcheck. Default: 30s
.EXAMPLE
  .\scripts\local-verify.ps1
  .\scripts\local-verify.ps1 -TimeoutSeconds 60
#>

param(
  [int]$TimeoutSeconds = 30
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ROOT "docker-compose.local.yml"
$ErrorActionPreference = 'Stop'

$failed = $false
$startTime = Get-Date

function Write-Step { param([string]$Msg) Write-Host "`n$Msg" -ForegroundColor Cyan }
function Write-OK   { param([string]$Msg) Write-Host "  [OK] $Msg" -ForegroundColor Green }
function Write-Fail { param([string]$Msg) Write-Host "  [FAIL] $Msg" -ForegroundColor Red; $script:failed = $true }

Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  VERIFICACION DEL ENTORNO LOCAL          ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Cyan

# ── 1. Verificar que Docker compose file existe ────────────
if (-not (Test-Path $ComposeFile)) {
  Write-Fail "No se encuentra $ComposeFile"
  exit 1
}

# ── 2. Verificar contenedores Docker ───────────────────────
Write-Step "1/4 - Verificando contenedores Docker..."

# Core: deben estar corriendo y (si definen healthcheck) healthy
$coreContainers = @("asistente-postgres", "asistente-backend", "asistente-frontend", "asistente-mailpit")
# Opcionales: se verifican solo si estan corriendo (perfil activo)
$optionalContainers = @(
  "asistente-prometheus", "asistente-loki", "asistente-tempo", "asistente-alloy", "asistente-grafana",
  "asistente-backup-sidecar", "asistente-public-tunnel", "asistente-caddy"
)

foreach ($name in $coreContainers) {
  $state = docker inspect --format='{{.State.Status}}' $name 2>$null
  if ($LASTEXITCODE -ne 0 -or $state -ne "running") {
    Write-Fail "Contenedor core $name no esta corriendo"
    continue
  }
  $health = docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $name 2>$null
  if ($health -eq "healthy" -or $health -eq "none") {
    Write-OK "$name = running (health: $health)"
  } elseif ($health -eq "starting") {
    Write-Fail "$name = starting (aun no listo)"
  } else {
    Write-Fail "$name = $health"
  }
}

$runningOptional = @()
foreach ($name in $optionalContainers) {
  $state = docker inspect --format='{{.State.Status}}' $name 2>$null
  if ($LASTEXITCODE -ne 0 -or $state -ne "running") {
    continue
  }
  $runningOptional += $name
  $health = docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $name 2>$null
  if ($health -eq "healthy" -or $health -eq "none") {
    Write-OK "$name = running (health: $health)"
  } else {
    Write-Fail "$name = $health"
  }
}
if ($runningOptional.Count -eq 0) {
  Write-Host "  [--] Sin contenedores opcionales activos (perfiles no levantados)" -ForegroundColor DarkGray
}

# ── 3. Backend health endpoint ──────────────────────────────
Write-Step "2/4 - Verificando backend..."

$timeoutSec = $TimeoutSeconds
try {
  $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec $timeoutSec
  if ($health.status -eq "UP") {
    Write-OK "Backend /actuator/health = UP"
  } else {
    Write-Fail "Backend health = $($health.status)"
  }
} catch {
  Write-Fail "Backend health endpoint no responde: $_"
}

# ── 4. Frontend HTTP 200 ────────────────────────────────────
Write-Step "3/4 - Verificando frontend..."

try {
  $resp = Invoke-WebRequest -Uri "http://localhost:5173" -Method Get -TimeoutSec $timeoutSec -UseBasicParsing
  if ($resp.StatusCode -eq 200) {
    Write-OK "Frontend HTTP 200"
  } else {
    Write-Fail "Frontend HTTP $($resp.StatusCode)"
  }
} catch {
  Write-Fail "Frontend no responde: $_"
}

# ── 5. Login + API smoke ───────────────────────────────────
Write-Step "4/4 - Smoke test (login + API)..."
try {
  $login = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post `
    -Body '{"email":"admin@demo.cl","password":"Cambiar123!"}' `
    -ContentType "application/json" -TimeoutSec $timeoutSec
  if ($login.accessToken) {
    Write-OK "Login exitoso (token obtenido)"
  } else {
    Write-Fail "Login no devolvio accessToken"
    $failed = $true
  }

  $headers = @{ Authorization = "Bearer $($login.accessToken)" }
  $biz = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/company" -Headers $headers -Method Get -TimeoutSec $timeoutSec
  if ($biz.id) {
    Write-OK "GET /api/v1/company OK (id=$($biz.id))"
  } else {
    Write-Fail "/api/v1/company no devolvio id"
  }
} catch {
  Write-Fail "Smoke test fallo: $_"
}

# ── Resumen ─────────────────────────────────────────────────
$elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds)
Write-Host ""

if ($failed) {
  Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Yellow
  Write-Host "║  VERIFICACION COMPLETADA CON FALLOS      ║" -ForegroundColor Yellow
  Write-Host "║  ($elapsed s)                             ║" -ForegroundColor Yellow
  Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Yellow
  exit 1
} else {
  Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
  Write-Host "║  VERIFICACION COMPLETADA: TODO OK        ║" -ForegroundColor Green
  Write-Host "║  ($elapsed s)                             ║" -ForegroundColor Green
  Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
  exit 0
}
