<#
.SYNOPSIS
    Test IA Auto-reply Local (PowerShell)
    Flujo: webhook -> ai_reply_outbox -> PROCESSED -> outbound_message

.PARAMETER Token
    Bearer token de autenticación (obligatorio)

.PARAMETER BaseUrl
    URL base del backend. Default: http://localhost:8080

.PARAMETER WebhookSecret
    Secret HMAC. Default: dev-whatsapp-web-webhook-secret
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$Token,
    
    [string]$BaseUrl = "http://localhost:8080",
    [string]$WebhookSecret = "dev-whatsapp-web-webhook-secret"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Test IA Auto-reply Local (PowerShell) ===" -ForegroundColor Cyan

$headers = @{ Authorization = "Bearer $Token" }

function Get-HMACSHA256 {
    param([string]$Key, [string]$Message)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Key))
    $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($Message))
    return [System.BitConverter]::ToString($hash).Replace("-", "").ToLower()
}

# 1. Stats outbox ANTES
Write-Host "1/4 - Stats outbox ANTES..." -ForegroundColor Yellow
$statsBefore = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ai/outbox/stats" -Headers $headers -Method Get
Write-Host "  $($statsBefore | ConvertTo-Json)" -ForegroundColor Gray

# 2. Enviar mensaje via webhook
Write-Host "2/4 - Enviando mensaje para trigger IA..." -ForegroundColor Yellow
$phone = "56950954580"
$deliveryId = [guid]::NewGuid().ToString()
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
$sessionKey = "demo-sales"
$message = "Hola, quiero agendar una limpieza facial para mañana a las 10:00 en Providencia"

$payload = @{
    eventType = "MESSAGE_RECEIVED"
    deliveryId = $deliveryId
    occurredAt = $timestamp
    sessionKey = $sessionKey
    payload = @{
        from = $phone
        body = $message
        to = "56900000000"
        externalMessageId = "ext-$deliveryId"
        chatId = "$phone@c.us"
        hasMedia = $false
        messageType = "text"
        timestamp = [int][double]::Parse((Get-Date -Date (Get-Date).ToUniversalTime() -UFormat %s))
    }
} | ConvertTo-Json -Depth 5 -Compress

$body = $payload
$sig = "sha256=$(Get-HMACSHA256 -Key $WebhookSecret -Message "$timestamp.$body")"

$webhookHeaders = @{
    "X-WhatsApp-Web-Timestamp" = $timestamp
    "X-WhatsApp-Web-Signature" = $sig
    "X-WhatsApp-Web-Delivery-Id" = $deliveryId
    "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri "$BaseUrl/api/v1/integrations/whatsapp-web/webhook" `
    -Method Post -Headers $webhookHeaders -Body $body | Out-Null
Write-Host "  Webhook enviado OK" -ForegroundColor Green

# 3. Poll outbox hasta PROCESSED (max 40s)
Write-Host "3/4 - Polling ai_reply_outbox (max 40s)..." -ForegroundColor Yellow
$processed = $false
for ($i = 1; $i -le 20; $i++) {
    Start-Sleep -Seconds 2
    $stats = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ai/outbox/stats" -Headers $headers -Method Get
    Write-Host "  [$($i*2)s] pending=$($stats.pending) processing=$($stats.processing) failed=$($stats.failed)" -ForegroundColor Gray
    
    if ($stats.pending -eq 0 -and $stats.processing -eq 0 -and $stats.failed -eq 0) {
        Write-Host "  ✅ Outbox procesado completamente" -ForegroundColor Green
        $processed = $true
        break
    }
}

if (-not $processed) {
    Write-Host "❌ Timeout esperando outbox" -ForegroundColor Red
    exit 1
}

# 4. Verificar outbound_message via channel_event_log
Write-Host "4/4 - Verificando outbound_message..." -ForegroundColor Yellow
$events = Invoke-RestMethod -Uri "$BaseUrl/api/v1/whatsapp-web/status" -Headers $headers -Method Get
$outboundCount = ($events.recentEvents | Where-Object { $_.eventType -eq "MESSAGE_SENT" }).Count
Write-Host "  Mensajes salientes (MESSAGE_SENT): $outboundCount" -ForegroundColor Gray

if ($outboundCount -ge 1) {
    Write-Host "✅ IA Auto-reply test PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "❌ No se detectó MESSAGE_SENT en eventos" -ForegroundColor Red
    exit 1
}