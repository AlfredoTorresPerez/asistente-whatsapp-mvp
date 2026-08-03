# =============================================================================
# RESTORE BACKEND CHECK - PostgreSQL + Backend (PowerShell 7+)
# Uso: .\scripts\restore-backend-check.ps1 -DbName <bd_restaurada> [-Port 8081]
#      .\scripts\restore-backend-check.ps1 -DbName <bd> -ExpectFailure [-FailurePattern "flyway"]
# Levanta una instancia temporal del backend (contenedor asistente-backend-restore-verify,
# puerto 8081) apuntando a la base restaurada, espera /actuator/health=UP y ejecuta
# operaciones funcionales: login, GET /api/v1/company y mensaje entrante simulado
# (escritura real contra la base restaurada). Al terminar elimina el contenedor.
# Con -ExpectFailure valida que el arranque FALLA (ej. migracion incompatible) y
# revisa el patron en los logs; exit 0 si falla como se espera.
# =============================================================================
param(
  [Parameter(Mandatory = $true)][string]$DbName,
  [int]$Port = 8081,
  [int]$TimeoutSeconds = 180,
  [switch]$ExpectFailure,
  [string]$FailurePattern = "flyway"
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "docker-compose.local.yml"
$EnvFile = Join-Path $Root ".env.local"
$OverrideFile = Join-Path $env:TEMP "backend-restore-verify.override.yml"
$ContainerName = "asistente-backend-restore-verify"
$HealthUrl = "http://localhost:$Port/actuator/health"

Write-Host "Iniciando instancia de backend contra la base restaurada '$DbName' (puerto $Port)..." -ForegroundColor Cyan

@"
services:
  backend-restore-verify:
    profiles: ["core"]
    image: asistente-backend-java:latest
    container_name: $ContainerName
    environment:
      APP_DB_URL: jdbc:postgresql://postgres:5432/$DbName
      SPRING_PROFILES_ACTIVE: local,local-safe
      APP_TRACING_ENABLED: "false"
      APP_WHATSAPP_CHANNEL_PROVIDER: SIMULATED
      APP_WHATSAPP_CLOUD_API_ENABLED: "false"
      APP_EMAIL_MIRROR_ENABLED: "false"
      APP_OPENAI_ENABLED: "false"
      APP_BOOKING_PAYMENT_PROVIDER: SIMULATED
      APP_LOGGING_INCLUDE_MESSAGE_BODY: "false"
      SERVER_PORT: "$Port"
    ports:
      - "${Port}:${Port}"
    networks:
      - asistente-local
"@ | Set-Content -LiteralPath $OverrideFile -Encoding ascii

try {
  docker compose --env-file $EnvFile -f $ComposeFile -f $OverrideFile up -d --no-deps backend-restore-verify 2>$null
  if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo (exit=$LASTEXITCODE)" }

  # Esperar health
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  $healthy = $false
  while ((Get-Date) -lt $deadline) {
    try {
      $h = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 5
      if ($h.status -eq "UP") { $healthy = $true; break }
    } catch { }
    Start-Sleep -Seconds 5
  }

  if ($ExpectFailure) {
    if ($healthy) {
      Write-Host "  [FAIL] El backend NUNCA debio quedar UP (se esperaba fallo)" -ForegroundColor Red
      exit 1
    }
    $logs = docker logs $ContainerName 2>&1 | Out-String
    if ($logs -match "(?i)$FailurePattern") {
      Write-Host "  [OK]   Arranque fallo como se esperaba (health no-UP)" -ForegroundColor Green
      Write-Host "  [OK]   Logs contienen el patron esperado: '$FailurePattern'" -ForegroundColor Green
      $match = $logs -match "(?i).*$FailurePattern[^\r\n]{0,200}" | Out-Null
      $line = ($logs -split "`n" | Where-Object { $_ -match "(?i)$FailurePattern" } | Select-Object -First 1)
      if ($line) { Write-Host "  log: $($line.Trim().Substring(0, [Math]::Min(220, $line.Trim().Length)))" -ForegroundColor DarkGray }
      exit 0
    } else {
      Write-Host "  [FAIL] El arranque fallo pero los logs NO contienen el patron '$FailurePattern'" -ForegroundColor Red
      exit 1
    }
  }

  if (-not $healthy) {
    Write-Host "  [FAIL] El backend no alcanzo /actuator/health=UP en ${TimeoutSeconds}s" -ForegroundColor Red
    exit 1
  }
  Write-Host "  [OK]   /actuator/health = UP (Flyway aplico/valido el esquema sobre la base restaurada)" -ForegroundColor Green

  # Operaciones funcionales (lectura + escritura)
  $login = Invoke-RestMethod -Uri "http://localhost:$Port/api/v1/auth/login" -Method Post `
    -Body '{"email":"admin@demo.cl","password":"Cambiar123!"}' -ContentType "application/json" -TimeoutSec 30
  if ($login.accessToken) { Write-Host "  [OK]   Login (admin demo) -> token obtenido" -ForegroundColor Green }
  else { Write-Host "  [FAIL] Login no devolvio accessToken" -ForegroundColor Red; exit 1 }

  $headers = @{ Authorization = "Bearer $($login.accessToken)" }
  $biz = Invoke-RestMethod -Uri "http://localhost:$Port/api/v1/company" -Headers $headers -Method Get -TimeoutSec 30
  if ($biz.id) { Write-Host "  [OK]   GET /api/v1/company -> id=$($biz.id)" -ForegroundColor Green }
  else { Write-Host "  [FAIL] /api/v1/company no devolvio id" -ForegroundColor Red; exit 1 }

  $inbound = Invoke-RestMethod -Uri "http://localhost:$Port/api/v1/test/whatsapp-inbound" -Method Post `
    -Headers $headers `
    -Body '{"sessionKey":"restore-check","from":"+56999999999","body":"Hola, quiero agendar una hora"}' `
    -ContentType "application/json" -TimeoutSec 60
  Write-Host "  [OK]   POST /api/v1/test/whatsapp-inbound (escritura en base restaurada) -> 200 OK" -ForegroundColor Green

  Write-Host ""
  Write-Host "Backend funcional contra la base restaurada: OK" -ForegroundColor Green
  exit 0
} finally {
  docker rm -f $ContainerName 2>$null | Out-Null
  Remove-Item -LiteralPath $OverrideFile -Force -ErrorAction SilentlyContinue
}
