#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Restaura secretos locales desde Windows Credential Manager como variables de entorno.
.DESCRIPTION
  Lee los secretos almacenados en Windows Credential Manager y los exporta como
  variables de entorno en la sesion actual para que docker-compose los herede.
  No escribe ningun archivo en disco con los secretos.

  Secretos que restaura:
    - JWT_SECRET
    - WHATSAPP_APP_SECRET
    - WHATSAPP_ACCESS_TOKEN
    - GMAIL_PASSWORD
    - APP_EMAIL_MIRROR_PASSWORD (mismo valor que GMAIL_PASSWORD)
    - OPENAI_API_KEY
    - SENTRY_DSN (opcional; si existe activa SENTRY_ENABLED=true)

.PARAMETER WhatIf
  Muestra que se restauraria sin modificar variables de entorno.
.EXAMPLE
  .\scripts\restore-local-secrets.ps1
.EXAMPLE
  .\scripts\restore-local-secrets.ps1 -WhatIf
#>

param([switch]$WhatIf)

$ROOT = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "lib\CredentialManager.ps1")

$requiredSecrets = [ordered]@{
  APP_JWT_SECRET = "asistente-local/JWT_SECRET"
  APP_WHATSAPP_CLOUD_API_APP_SECRET = "asistente-local/WHATSAPP_APP_SECRET"
  APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN = "asistente-local/WHATSAPP_ACCESS_TOKEN"
  SPRING_MAIL_PASSWORD = "asistente-local/GMAIL_PASSWORD"
  APP_EMAIL_MIRROR_PASSWORD = "asistente-local/GMAIL_PASSWORD"
  APP_OPENAI_API_KEY = "asistente-local/OPENAI_API_KEY"
}

$optionalSecrets = [ordered]@{
  SENTRY_DSN = "asistente-local/SENTRY_DSN"
}

$allFound = $true
$restored = @()

foreach ($envVar in $requiredSecrets.Keys) {
  $target = $requiredSecrets[$envVar]
  $value = Get-LocalSecret -Name ($target -replace '^asistente-local/', '')

  if ($null -eq $value) {
    Write-Warning "  [$envVar] NO ENCONTRADO en Credential Manager (target: $target)"
    $allFound = $false
  } else {
    if ($WhatIf) {
      Write-Host "  [$envVar] -> $target (longitud: $($value.Length) chars)" -ForegroundColor Cyan
    } else {
      Set-Item -Path "env:$envVar" -Value $value
      Write-Host "  [$envVar] restaurado ($($value.Length) chars)" -ForegroundColor Green
    }
    $restored += $envVar
  }
}

foreach ($envVar in $optionalSecrets.Keys) {
  $target = $optionalSecrets[$envVar]
  $value = Get-LocalSecret -Name ($target -replace '^asistente-local/', '')

  if ($null -eq $value) {
    Write-Host "  [$envVar] opcional no configurado (Sentry local queda desactivado)" -ForegroundColor DarkGray
  } else {
    if ($WhatIf) {
      Write-Host "  [$envVar] -> $target (opcional, longitud: $($value.Length) chars)" -ForegroundColor Cyan
    } else {
      Set-Item -Path "env:$envVar" -Value $value
      Set-Item -Path "env:SENTRY_ENABLED" -Value "true"
      Write-Host "  [$envVar] restaurado; SENTRY_ENABLED=true" -ForegroundColor Green
    }
    $restored += $envVar
  }
}

if ($restored.Count -gt 0 -and -not $WhatIf) {
  Write-Host "Secretos restaurados: $($restored -join ', ')" -ForegroundColor Cyan
}

if (-not $allFound) {
  Write-Warning "Algunos secretos no estan en Credential Manager."
  Write-Host "Para guardarlos: .\scripts\store-local-secrets.ps1" -ForegroundColor Yellow
  exit 1
}

$global:LASTEXITCODE = 0
