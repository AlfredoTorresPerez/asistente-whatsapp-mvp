<#
.SYNOPSIS
    Inicia el tunel publico HTTPS via Cloudflare Tunnel (trycloudflare.com)
.DESCRIPTION
    - Levanta los servicios locales si no estan corriendo
    - Inicia cloudflared con --profile public-link
    - Espera la URL generada por Cloudflare
    - Actualiza .env.local con las URLs publicas
    - Recrea backend-java y frontend-react con las nuevas URLs
.EXAMPLE
    .\scripts\start-public-link.ps1
.NOTES
    La URL cambia cada vez que se reinicia el contenedor cloudflared.
    No usar en QA ni produccion.
#>

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$ComposeFile = "docker-compose.local.yml"
$EnvFile = ".env.local"
$Profile = "public-link"
$MaxAttempts = 90
$ServiceName = "public-tunnel"

function Write-Step { param([string]$Message) Write-Host $Message }

function Stop-WithError {
    param([string]$Message)
    Write-Error $Message
    exit 1
}

function Assert-Command {
    param([string]$CommandName)
    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        Stop-WithError "No se encontro el comando requerido: $CommandName"
    }
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

function Update-EnvValue {
    param([string]$Key, [string]$Value, [string]$FilePath)
    $lines = Get-Content -Path $FilePath -ErrorAction SilentlyContinue
    $pattern = "^$([regex]::Escape($Key))="
    $updated = $false
    $newLines = foreach ($line in $lines) {
        if ($line -match $pattern) {
            "$Key=$Value"
            $updated = $true
        } else {
            $line
        }
    }
    if (-not $updated) {
        $newLines += "$Key=$Value"
    }
    Set-Content -Path $FilePath -Value $newLines -Encoding ASCII
    Set-Item -Path "env:$Key" -Value $Value
}

Assert-Command "docker"

Write-Step "Paso 1/5: Levantando servicios locales..."
docker compose --env-file "$EnvFile" -f $ComposeFile up -d --build
if ($LASTEXITCODE -ne 0) { Stop-WithError "docker compose up fallo" }

Write-Step "Paso 2/5: Iniciando tunel Cloudflare..."
docker compose --env-file "$EnvFile" -f $ComposeFile --profile $Profile up -d --force-recreate $ServiceName
if ($LASTEXITCODE -ne 0) { Stop-WithError "No se pudo iniciar el tunel" }

Write-Step "Paso 3/5: Esperando URL publica..."
$publicUrl = ""
for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
    $publicUrl = Get-TunnelUrl
    if (-not [string]::IsNullOrWhiteSpace($publicUrl)) {
        break
    }
    Start-Sleep -Seconds 2
}

if ([string]::IsNullOrWhiteSpace($publicUrl)) {
    Stop-WithError "No se obtuvo URL publica. Revisa: docker compose --env-file $EnvFile -f $ComposeFile logs $ServiceName"
}

Write-Step "Paso 4/5: Actualizando .env.local con URL publica: $publicUrl"
Update-EnvValue -Key "APP_WHATSAPP_CLOUD_API_WEBHOOK_PUBLIC_URL" -Value "$publicUrl" -FilePath $EnvFile
Update-EnvValue -Key "APP_FRONTEND_PUBLIC_BASE_URL" -Value "$publicUrl" -FilePath $EnvFile
Update-EnvValue -Key "APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL" -Value "$publicUrl/reservas/confirmar" -FilePath $EnvFile
Update-EnvValue -Key "APP_BOOKING_RESCHEDULE_PUBLIC_BASE_URL" -Value "$publicUrl/reservas/reprogramar" -FilePath $EnvFile
Update-EnvValue -Key "APP_BOOKING_CANCELLATION_PUBLIC_BASE_URL" -Value "$publicUrl/reservas/cancelar" -FilePath $EnvFile
Update-EnvValue -Key "APP_BOOKING_PAYMENT_CHECKOUT_PUBLIC_BASE_URL" -Value "$publicUrl/reservas/pagar" -FilePath $EnvFile

Write-Step "Paso 5/5: Recreando backend y frontend con la nueva URL..."
docker compose --env-file "$EnvFile" -f $ComposeFile up -d --force-recreate backend-java frontend-react
if ($LASTEXITCODE -ne 0) { Stop-WithError "No se pudo recrear servicios" }

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  TUNEL PUBLICO ACTIVO" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  URL:     $publicUrl" -ForegroundColor Green
Write-Host "  Frontend: $publicUrl" -ForegroundColor Green
Write-Host "  API:     $publicUrl/api/v1/..." -ForegroundColor Green
Write-Host "  Health:  $publicUrl/api/v1/health" -ForegroundColor Green
Write-Host "  Webhook: $publicUrl/api/v1/integrations/whatsapp-cloud/webhook" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "ADVERTENCIA: Esta URL es TEMPORAL." -ForegroundColor Yellow
Write-Host "- Cambia al reiniciar el contenedor cloudflared" -ForegroundColor Yellow
Write-Host "- No usar en QA ni produccion" -ForegroundColor Yellow
Write-Host "- No registrar como webhook permanente de WhatsApp" -ForegroundColor Yellow
Write-Host "- No incluir en correos o enlaces persistentes" -ForegroundColor Yellow
Write-Host ""
Write-Host "Para ver logs: docker compose -f $ComposeFile logs -f $ServiceName" -ForegroundColor Gray
Write-Host "Para detener:  .\scripts\stop-public-link.ps1" -ForegroundColor Gray
