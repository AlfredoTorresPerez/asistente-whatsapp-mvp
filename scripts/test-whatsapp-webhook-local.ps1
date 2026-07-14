<#
.SYNOPSIS
    Test rápido webhook WhatsApp Web local (PowerShell)
    Valida: HMAC válido -> 200 OK -> channel_event_log insertado

.PARAMETER Token
    Bearer token de autenticación (obligatorio)

.PARAMETER BaseUrl
    URL base del backend. Default: http://localhost:8080

.PARAMETER WebhookSecret
    Secret HMAC para firmar. Default: dev-whatsapp-web-webhook-secret

.EXAMPLE
    .\scripts\test-whatsapp-webhook-local.ps1 -Token "eyJhbGciOiJIUzI1NiIs..."
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$Token,
    
    [string]$BaseUrl = "http://localhost:8080",
    [string]$WebhookSecret = "dev-whatsapp-web-webhook-secret"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Test Webhook WhatsApp Web (PowerShell) ===" -ForegroundColor Cyan

$headers = @{ Authorization = "Bearer $Token" }

function Get-HMACSHA256 {
    param([string]$Key, [string]$Message)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Key))
    $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($Message))
    return [System.BitConverter]::ToString($hash).Replace("-", "").ToLower()
}

# Payload
$phone = "56950954580"
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
        body = "Test webhook local $(Get-Date -Format HH:mm:ss)"
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

Write-Host "Enviando webhook..." -ForegroundColor Yellow
$resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/integrations/whatsapp-web/webhook" `
    -Method Post -Headers $webhookHeaders -Body $body
Write-Host "Response: $($resp | ConvertTo-Json)" -ForegroundColor Green

# Verificar channel_event_log
Start-Sleep -Seconds 1
$events = Invoke-RestMethod -Uri "$BaseUrl/api/v1/whatsapp-web/status" -Headers $headers -Method Get
$found = $events.recentEvents | Where-Object { $_.deliveryId -eq $deliveryId } | Select-Object -First 1

if ($found -and $found.processingStatus -eq "PROCESSED") {
    Write-Host "✅ Webhook test PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "❌ Evento no encontrado o no PROCESSED: $($found.processingStatus)" -ForegroundColor Red
    exit 1
}