<#
.SYNOPSIS
    Re-registra la callback URL del webhook de WhatsApp Cloud API en Meta sin usar el dashboard.
.DESCRIPTION
    Cuando el tunel publico cambia de URL (trycloudflare), Meta deja de entregar los
    webhooks porque la callback URL configurada apunta al hostname anterior.
    Este script actualiza la suscripcion via Graph API:
      POST /v23.0/{APP_ID}/subscriptions
    - Detecta la URL actual del tunel desde los logs del contenedor (o usa -CallbackUrl)
    - APP_ID y verify token se leen de .env.local
    - APP_SECRET se lee de Windows Credential Manager (asistente-local/WHATSAPP_APP_SECRET)
    - NO envia el campo fields: da error de permisos (subcode 1929002)
.EXAMPLE
    .\scripts\re-register-meta-webhook.ps1
    .\scripts\re-register-meta-webhook.ps1 -CallbackUrl "https://mi-dominio.cl/api/v1/integrations/whatsapp-cloud/webhook"
.NOTES
    Requiere: docker corriendo, secretos restaurados (restore-local-secrets.ps1)
#>

[CmdletBinding()]
param(
    [string]$CallbackUrl = ""
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$EnvFile = ".env.local"
$ComposeFile = "docker-compose.local.yml"
$ServiceName = "public-tunnel"
$GraphVersion = "v23.0"
$WebhookPath = "/api/v1/integrations/whatsapp-cloud/webhook"

function Get-EnvValue {
    param([string]$Key, [string]$FilePath)
    $line = Get-Content -Path $FilePath | Where-Object { $_ -match "^$([regex]::Escape($Key))=" } | Select-Object -First 1
    if (-not $line) { return "" }
    return $line.Substring($line.IndexOf("=") + 1)
}

function Get-TunnelUrl {
    $logs = docker compose --env-file "$EnvFile" -f $ComposeFile logs --no-color $ServiceName 2>$null
    $pattern = 'https://[-a-zA-Z0-9.]+\.trycloudflare\.com'
    $urls = @()
    foreach ($line in $logs) {
        $match = [regex]::Match($line, $pattern)
        if ($match.Success -and $match.Value -ne "https://api.trycloudflare.com") {
            $urls += $match.Value
        }
    }
    if ($urls.Count -eq 0) { return "" }
    return $urls[-1]
}

. (Join-Path $PSScriptRoot "lib\CredentialManager.ps1")

if (-not $CallbackUrl) {
    $tunnelUrl = Get-TunnelUrl
    if (-not $tunnelUrl) {
        throw "No se pudo detectar la URL del tunel. Levanta el tunel o usa -CallbackUrl."
    }
    $CallbackUrl = "$($tunnelUrl.TrimEnd('/'))$WebhookPath"
}

$appId = Get-EnvValue -Key "APP_WHATSAPP_CLOUD_API_APP_ID" -FilePath $EnvFile
$verifyToken = Get-EnvValue -Key "APP_WHATSAPP_CLOUD_API_WEBHOOK_VERIFY_TOKEN" -FilePath $EnvFile
$appSecret = Get-LocalSecret -Name "WHATSAPP_APP_SECRET"

if (-not $appId) { throw "APP_WHATSAPP_CLOUD_API_APP_ID vacio en $EnvFile" }
if (-not $verifyToken) { throw "APP_WHATSAPP_CLOUD_API_WEBHOOK_VERIFY_TOKEN vacio en $EnvFile" }
if (-not $appSecret) { throw "No hay WHATSAPP_APP_SECRET en Credential Manager. Ejecuta store-local-secrets.ps1" }

Write-Host "Callback URL: $CallbackUrl"
Write-Host "Registrando suscripcion en Graph API..."

$body = @{
    access_token = "$appId|$appSecret"
    object       = "whatsapp_business_account"
    callback_url = $CallbackUrl
    verify_token = $verifyToken
}

try {
    $resp = Invoke-RestMethod -Method Post -Uri "https://graph.facebook.com/$GraphVersion/$appId/subscriptions" `
        -Body $body -ContentType "application/x-www-form-urlencoded" -TimeoutSec 30
    Write-Host "OK: $(ConvertTo-Json $resp -Compress)"
} catch {
    $detail = $_.Exception.Response.StatusCode
    Write-Error "Fallo el registro del webhook (HTTP $detail). Verifica APP_ID, APP_SECRET y permisos de la app."
    throw
}
