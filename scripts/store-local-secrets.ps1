#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Guarda o actualiza secretos locales en Windows Credential Manager.
.DESCRIPTION
  Permite almacenar o actualizar los secretos de forma segura en Windows Credential Manager.
  Los valores se piden de forma interactiva (oculta mientras se escribe).
  No escribe ningun archivo en disco.

  Secretos que gestiona:
    - WHATSAPP_APP_SECRET  : App Secret de Meta (para webhook)
    - WHATSAPP_ACCESS_TOKEN : Token persistente de WhatsApp Cloud API
    - GMAIL_PASSWORD       : App Password de Gmail para envio SMTP local

.PARAMETER Batch
  Permite pasar los valores como parametros (solo para automatizacion).
  Uso: .\scripts\store-local-secrets.ps1 -Batch @{WHATSAPP_APP_SECRET="..."; WHATSAPP_ACCESS_TOKEN="..."; GMAIL_PASSWORD="..."}
.EXAMPLE
  .\scripts\store-local-secrets.ps1
  # Modo interactivo: pide cada secreto ocultando la escritura
.EXAMPLE
  .\scripts\store-local-secrets.ps1 -WhatIf
  # Muestra que secretos se guardarian sin modificarlos
#>

param(
  [hashtable]$Batch = @{},
  [switch]$WhatIf
)

$ROOT = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "lib\CredentialManager.ps1")

$secretDefs = @(
  @{ Name = "WHATSAPP_APP_SECRET"; Prompt = "App Secret de Meta (webhook signature)" }
  @{ Name = "WHATSAPP_ACCESS_TOKEN"; Prompt = "Access Token de WhatsApp Cloud API" }
  @{ Name = "GMAIL_PASSWORD"; Prompt = "App Password de Gmail (notificacionesassistentelocale@gmail.com)" }
)

Write-Host "=== Almacenar secretos locales en Windows Credential Manager ===" -ForegroundColor Cyan
Write-Host "Los valores se almacenan cifrados con DPAPI (solo tu usuario puede leerlos).`n" -ForegroundColor Gray

$saved = 0
$skipped = 0

foreach ($def in $secretDefs) {
  $name = $def.Name

  if ($WhatIf) {
    Write-Host "  [$name] se guardaria (o se saltaria si Batch contiene el valor)" -ForegroundColor Cyan
    continue
  }

  $current = Get-LocalSecret -Name $name
  if ($null -ne $current) {
    Write-Host "  [$name] ya existe en Credential Manager" -ForegroundColor Yellow
    $resp = Read-Host "    Deseas sobrescribirlo? (s/N)"
    if ($resp -ne 's' -and $resp -ne 'S') {
      $skipped++
      continue
    }
  }

  $value = $null
  if ($Batch.ContainsKey($name) -and -not [string]::IsNullOrEmpty($Batch[$name])) {
    $value = $Batch[$name]
    Write-Host "  [$name] usando valor del parametro -Batch" -ForegroundColor Gray
  } else {
    $secure = Read-Host -AsSecureString "  $($def.Prompt): "
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $value = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  }

  if ([string]::IsNullOrEmpty($value)) {
    Write-Warn "  [$name] valor vacio, se omite"
    continue
  }

  Set-LocalSecret -Name $name -Value $value
  Write-Host "  [$name] guardado ($($value.Length) chars)" -ForegroundColor Green
  $saved++
}

Write-Host ""
if ($WhatIf) {
  Write-Host "Modo WhatIf: ningun secreto fue modificado." -ForegroundColor Cyan
} else {
  Write-Host "Resumen: $saved guardados, $skipped omitidos." -ForegroundColor Cyan
  if ($saved -gt 0) {
    Write-Host "Para verificar: .\scripts\restore-local-secrets.ps1 -WhatIf" -ForegroundColor Gray
  }
}
