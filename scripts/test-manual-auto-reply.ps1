<#
.SYNOPSIS
    Test manual automatizado del flujo auto-reply IA local (Fase 4)
    Verifica: webhook -> channel_event_log -> ai_reply_outbox -> procesador -> ChannelDispatch -> WhatsApp Web

.PREREQUISITES
    - Docker Desktop corriendo
    - docker compose -f docker-compose.local.yml --profile whatsapp up -d
    - Backend Java compilado y corriendo (puerto 8080)
    - whatsapp-web-service conectado y con sesión activa (CONNECTED)

.USAGE
    .\scripts\test-manual-auto-reply.ps1
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$WhatsAppWebUrl = "http://localhost:3001",
    [string]$ApiKey = "dev-whatsapp-web-key",
    [string]$WebhookSecret = "dev-whatsapp-web-webhook-secret"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST MANUAL AUTO-REPLY IA LOCAL (FASE 4)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Verificar que el backend está disponible
Write-Host "`n[1/8] Verificando backend Java..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -ErrorAction Stop
    if ($health.status -ne "UP") {
        Write-Error "Backend no está UP: $($health | ConvertTo-Json)"
    }
    Write-Host "  OK - Backend UP" -ForegroundColor Green
} catch {
    Write-Error "Backend no disponible en $BaseUrl: $_"
}

# 2. Verificar whatsapp-web-service
Write-Host "`n[2/8] Verificando whatsapp-web-service..." -ForegroundColor Yellow
try {
    $wwHealth = Invoke-RestMethod -Uri "$WhatsAppWebUrl/health" -Method Get -ErrorAction Stop
    if ($wwHealth.runtimeReady -ne $true) {
        Write-Warning "whatsapp-web-service no está listo (runtimeReady=false). Asegúrate de haber escaneado el QR."
    } else {
        Write-Host "  OK - whatsapp-web-service runtimeReady=true" -ForegroundColor Green
    }
} catch {
    Write-Error "whatsapp-web-service no disponible en $WhatsAppWebUrl: $_"
}

# 3. Verificar estado del canal WhatsApp
Write-Host "`n[3/8] Verificando estado canal WhatsApp..." -ForegroundColor Yellow
$token = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body '{"email":"admin@demo.cl","password":"admin123"}' -ContentType "application/json").accessToken
$headers = @{ Authorization = "Bearer $token" }

$status = Invoke-RestMethod -Uri "$BaseUrl/api/v1/whatsapp-web/status" -Headers $headers -Method Get
Write-Host "  Estado: $($status.sessionStatus) | Teléfono: $($status.phoneNumber) | QR: $(if($status.qrCode){'SÍ'}else{'NO'})" -ForegroundColor Green

if ($status.sessionStatus -ne "CONNECTED") {
    Write-Warning "Canal no está CONNECTED. Escanea el QR desde /admin/whatsapp-web"
}

# 4. Verificar stats del outbox ANTES
Write-Host "`n[4/8] Stats outbox ANTES del test..." -ForegroundColor Yellow
$statsBefore = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ai/outbox/stats" -Headers $headers -Method Get
Write-Host "  Pendientes: $($statsBefore.pending) | Procesando: $($statsBefore.processing) | Fallidos: $($statsBefore.failed) | Más antiguo: $($statsBefore.oldestAgeSeconds)s" -ForegroundColor Gray

# 5. Enviar mensaje simulado via webhook
Write-Host "`n[5/8] Enviando mensaje simulado via webhook..." -ForegroundColor Yellow
$phone = "56950954580"
$message = "Hola, quiero agendar una limpieza facial para mañana a las 10:00 en Providencia"
$deliveryId = [guid]::NewGuid().ToString()
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
$sessionKey = "demo-sales"

$payload = @{
    eventType = "MESSAGE_RECEIVED"
    deliveryId = $deliveryId
    occurredAt = $timestamp
    sessionKey = $sessionKey
    payload = @{
        from = $phone
        body = $message
        to = "56927305158"
        externalMessageId = "ext-$deliveryId"
        chatId = "$phone@c.us"
        hasMedia = $false
        messageType = "text"
        timestamp = [int][double]::Parse((Get-Date -Date (Get-Date).ToUniversalTime() -UFormat %s))
    }
} | ConvertTo-Json -Depth 5 -Compress

$body = $payload
$sig = "sha256=$(Use-HMACSHA256 -Key $WebhookSecret -Message "$timestamp.$body")"

$webhookHeaders = @{
    "X-WhatsApp-Web-Timestamp" = $timestamp
    "X-WhatsApp-Web-Signature" = $sig
    "X-WhatsApp-Web-Delivery-Id" = $deliveryId
    "Content-Type" = "application/json"
}

$response = Invoke-RestMethod -Uri "$BaseUrl/api/v1/integrations/whatsapp-web/webhook" -Method Post -Headers $webhookHeaders -Body $body
Write-Host "  Webhook response: $($response | ConvertTo-Json)" -ForegroundColor Green

# 6. Poll ai_reply_outbox hasta PROCESSED
Write-Host "`n[6/8] Polling ai_reply_outbox hasta PROCESSED (máx 30s)..." -ForegroundColor Yellow
$maxWait = 30
$elapsed = 0
$processed = $false
$outboxId = $null

while ($elapsed -lt $maxWait -and -not $processed) {
    Start-Sleep -Seconds 2
    $elapsed += 2
    
    $stats = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ai/outbox/stats" -Headers $headers -Method Get
    Write-Host "  [$elapsed s] Pendientes: $($stats.pending) Procesando: $($stats.processing) Fallidos: $($stats.failed) Más antiguo: $($stats.oldestAgeSeconds)s" -ForegroundColor Gray
    
    if ($stats.pending -eq 0 -and $stats.processing -eq 0 -and $stats.failed -eq 0) {
        # Verificar si se procesó consultando la BD directamente vía endpoint admin
        $processed = $true
    }
}

if (-not $processed) {
    Write-Warning "Timeout esperando procesamiento. Revisa logs del outbox worker."
}

# 7. Verificar outbound_message insertado
Write-Host "`n[7/8] Verificando outbound_message insertado..." -ForegroundColor Yellow
# No hay endpoint directo, pero podemos ver via channel_event_log
$events = Invoke-RestMethod -Uri "$BaseUrl/api/v1/whatsapp-web/status" -Headers $headers -Method Get
Write-Host "  Últimos eventos: $($events.recentEvents.Count)" -ForegroundColor Gray
$events.recentEvents | ForEach-Object {
    Write-Host "    - $($_.eventType) | $($_.processingStatus) | $($_.receivedAt)" -ForegroundColor Gray
}

# 8. Stats outbox DESPUÉS
Write-Host "`n[8/8] Stats outbox DESPUÉS del test..." -ForegroundColor Yellow
$statsAfter = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ai/outbox/stats" -Headers $headers -Method Get
Write-Host "  Pendientes: $($statsAfter.pending) | Procesando: $($statsAfter.processing) | Fallidos: $($statsAfter.failed) | Más antiguo: $($statsAfter.oldestAgeSeconds)s" -ForegroundColor Gray

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TEST COMPLETADO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Función helper HMAC
function Use-HMACSHA256 {
    param([string]$Key, [string]$Message)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Key))
    $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($Message))
    return [System.BitConverter]::ToString($hash).Replace("-", "").ToLower()
}