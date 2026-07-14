<#
.SYNOPSIS
    Validación local completa del MVP Asistente WhatsApp en <3 min.
    Ejecuta: health checks, auth, API smoke, WhatsApp Web, AI auto-reply.

.DESCRIPTION
    Suite de validación end-to-end para entorno local (docker-compose.local.yml).
    Exit codes:
      0 = OK (todo PASS)
      1 = Docker compose falló
      2 = Healthchecks timeout
      3 = API/Auth falló
      4 = WhatsApp Web falló
      5 = IA Auto-reply falló

.PARAMETER Quick
    Solo health + auth (para pre-push hook). Default: $false

.PARAMETER Profile
    Perfil docker-compose a usar. Default: "" (base). Use "whatsapp" para incluir whatsapp-web-service.

.PARAMETER TimeoutMinutes
    Timeout global en minutos. Default: 3

.PARAMETER NoCleanup
    No hacer docker compose down al final. Default: $false

.EXAMPLE
    .\scripts\verify_mvp_local.ps1
    .\scripts\verify_mvp_local.ps1 -Quick
    .\scripts\verify_mvp_local.ps1 -Profile whatsapp
    .\scripts\verify_mvp_local.ps1 -TimeoutMinutes 5 -NoCleanup
#>

param(
    [switch]$Quick,
    [string]$Profile = "",
    [int]$TimeoutMinutes = 3,
    [switch]$NoCleanup
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir
$ComposeFile = Join-Path $RootDir "docker-compose.local.yml"
$EnvFile = Join-Path $RootDir ".env.local"

$Global:ExitCode = 0
$Global:StartTime = Get-Date
$Global:Timeout = (Get-Date).AddMinutes($TimeoutMinutes)

# Colors
$C_Green = [ConsoleColor]::Green
$C_Red = [ConsoleColor]::Red
$C_Yellow = [ConsoleColor]::Yellow
$C_Cyan = [ConsoleColor]::Cyan
$C_Gray = [ConsoleColor]::Gray

function Write-Status {
    param([string]$Msg, [ConsoleColor]$Color = $C_Cyan)
    $elapsed = [math]::Round(((Get-Date) - $Global:StartTime).TotalSeconds, 0)
    Write-Host "[$elapsed s] $Msg" -ForegroundColor $Color
}

function Write-Pass { param([string]$Msg) Write-Status "PASS: $Msg" $C_Green }
function Write-Fail { param([string]$Msg) Write-Status "FAIL: $Msg" $C_Red; $Global:ExitCode = 1 }
function Write-Warn { param([string]$Msg) Write-Status "WARN: $Msg" $C_Yellow }
function Write-Info { param([string]$Msg) Write-Status "INFO: $Msg" $C_Gray }

function Check-Timeout {
    if ((Get-Date) -gt $Global:Timeout) {
        Write-Fail "TIMEOUT global ($TimeoutMinutes min) excedido"
        exit 1
    }
}

function Invoke-Retry {
    param(
        [scriptblock]$Action,
        [string]$Description,
        [int]$MaxRetries = 30,
        [int]$DelaySeconds = 2
    )
    for ($i = 1; $i -le $MaxRetries; $i++) {
        Check-Timeout
        try {
            $result = & $Action
            if ($result) { return $result }
        } catch {
            if ($i -eq $MaxRetries) { throw }
        }
        Start-Sleep -Seconds $DelaySeconds
    }
    throw "Retry agotado: $Description"
}

# ============================================================================
# MAIN
# ============================================================================
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor $C_Cyan
Write-Host "║  MVP ASISTENTE WHATSAPP - VALIDACIÓN LOCAL ($($Quick ? 'QUICK' : 'FULL'))  ║" -ForegroundColor $C_Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor $C_Cyan

# 0. Verificar archivos
Write-Info "Verificando archivos requeridos..."
if (-not (Test-Path $ComposeFile)) { Write-Fail "No existe $ComposeFile"; exit 1 }
if (-not (Test-Path $EnvFile))    { Write-Warn "No existe .env.local (usando defaults)" }

# 1. Docker Compose Up
Write-Status "1/8 - Levantando servicios con docker compose..."
$composeCmd = "docker compose -f `"$ComposeFile`""
if ($Profile) { $composeCmd += " --profile $Profile" }
$composeCmd += " up -d --build"

try {
    $output = Invoke-Expression $composeCmd
    Write-Pass "docker compose up -d --build OK"
} catch {
    Write-Fail "docker compose up falló: $_"
    exit 1
}

# 2. Wait Healthchecks
Write-Status "2/8 - Esperando healthchecks (máx $TimeoutMinutes min)..."
$services = @("postgres", "backend-java", "frontend-react")
if ($Profile -eq "whatsapp") { $services += "whatsapp-web-service" }

foreach ($svc in $services) {
    Write-Info "  Esperando $svc..."
    $healthy = $false
    $retries = 90  # 3 min / 2s
    for ($i = 1; $i -le $retries; $i++) {
        Check-Timeout
        $state = docker inspect --format='{{.State.Health.Status}}' "asistente-$svc" 2>$null
        if ($state -eq "healthy") { $healthy = $true; break }
        if ($state -eq "unhealthy") { Write-Fail "$svc está unhealthy"; exit 2 }
        Start-Sleep -Seconds 2
    }
    if (-not $healthy) { Write-Fail "$svc no alcanzó healthy en 3 min"; exit 2 }
    Write-Pass "$svc healthy"
}

# 3. Health Endpoints
Write-Status "3/8 - Verificando endpoints de salud..."

# Backend
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 10
    if ($health.status -ne "UP") { Write-Fail "Backend health: $($health.status)"; exit 3 }
    Write-Pass "Backend /actuator/health = UP"
} catch { Write-Fail "Backend health endpoint falló: $_"; exit 3 }

# Frontend
try {
    $resp = Invoke-WebRequest -Uri "http://localhost:5173" -Method Get -TimeoutSec 10 -UseBasicParsing
    if ($resp.StatusCode -ne 200) { Write-Fail "Frontend HTTP $($resp.StatusCode)"; exit 3 }
    Write-Pass "Frontend HTTP 200"
} catch { Write-Fail "Frontend endpoint falló: $_"; exit 3 }

# WhatsApp Web (si profile activo)
if ($Profile -eq "whatsapp") {
    try {
        $wwHealth = Invoke-RestMethod -Uri "http://localhost:3001/health" -Method Get -TimeoutSec 10
        if ($wwHealth.runtimeReady -ne $true) { Write-Warn "WhatsApp Web runtimeReady=false (puede requerir QR)" }
        else { Write-Pass "WhatsApp Web runtimeReady=true" }
    } catch { Write-Warn "WhatsApp Web health no accesible: $_" }
}

# 4. Auth + API Smoke
Write-Status "4/8 - Autenticación y API smoke test..."
$token = $null
try {
    $login = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post `
        -Body '{"email":"admin@demo.cl","password":"Cambiar123!"}' -ContentType "application/json" -TimeoutSec 10
    $token = $login.accessToken
    if (-not $token) { Write-Fail "Login no devolvió accessToken"; exit 3 }
    Write-Pass "Login admin@demo.cl OK"
} catch { Write-Fail "Login falló: $_"; exit 3 }

$headers = @{ Authorization = "Bearer $token" }

try {
    $biz = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/businesses/current" -Headers $headers -Method Get -TimeoutSec 10
    if (-not $biz.id) { Write-Fail "businesses/current no devolvió id"; exit 3 }
    Write-Pass "GET /businesses/current OK (id=$($biz.id))"
} catch { Write-Fail "businesses/current falló: $_"; exit 3 }

if ($Quick) {
    Write-Host "`n═══════════════════════════════════════════" -ForegroundColor $C_Cyan
    Write-Host "✅ QUICK VALIDATION PASSED (health + auth)" -ForegroundColor $C_Green
    Write-Host "═══════════════════════════════════════════" -ForegroundColor $C_Cyan
    if (-not $NoCleanup) { docker compose -f $ComposeFile down }
    exit 0
}

# 5. WhatsApp Web Status
if ($Profile -eq "whatsapp") {
    Write-Status "5/8 - Verificando estado WhatsApp Web..."
    try {
        $wwStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/whatsapp-web/status" -Headers $headers -Method Get -TimeoutSec 10
        $sess = $wwStatus.sessionStatus
        if ($sess -in @("CONNECTED", "QR_PENDING")) {
            Write-Pass "WhatsApp Web sessionStatus = $sess"
        } else {
            Write-Fail "WhatsApp Web sessionStatus = $sess (esperado CONNECTED o QR_PENDING)"
            exit 4
        }
    } catch { Write-Fail "WhatsApp Web status falló: $_"; exit 4 }
}

# 6. Webhook Test
Write-Status "6/8 - Test webhook WhatsApp..."
$webhookScript = Join-Path $ScriptDir "test-whatsapp-webhook-local.ps1"
if (Test-Path $webhookScript) {
    $whResult = & $webhookScript -Token $token
    if ($LASTEXITCODE -ne 0) { Write-Fail "Webhook test falló (exit $LASTEXITCODE)"; exit 4 }
    Write-Pass "Webhook test OK"
} else {
    Write-Warn "test-whatsapp-webhook-local.ps1 no encontrado, saltando"
}

# 7. AI Auto-Reply Test
Write-Status "7/8 - Test IA Auto-reply..."
$aiScript = Join-Path $ScriptDir "test-ai-auto-reply-local.ps1"
if (Test-Path $aiScript) {
    $aiResult = & $aiScript -Token $token
    if ($LASTEXITCODE -ne 0) { Write-Fail "AI auto-reply test falló (exit $LASTEXITCODE)"; exit 5 }
    Write-Pass "AI auto-reply test OK"
} else {
    Write-Warn "test-ai-auto-reply-local.ps1 no encontrado, saltando"
}

# 8. Cleanup
if (-not $NoCleanup) {
    Write-Status "8/8 - Limpiando (docker compose down)..."
    docker compose -f $ComposeFile down | Out-Null
    Write-Pass "Cleanup OK"
} else {
    Write-Info "8/8 - NoCleanup: servicios siguen corriendo"
}

# Summary
$totalSec = [math]::Round((Get-Date) - $Global:StartTime).TotalSeconds
Write-Host "`n═══════════════════════════════════════════" -ForegroundColor $C_Cyan
Write-Host "✅ VALIDACIÓN LOCAL COMPLETA: PASS ($totalSec s)" -ForegroundColor $C_Green
Write-Host "═══════════════════════════════════════════" -ForegroundColor $C_Cyan
exit 0