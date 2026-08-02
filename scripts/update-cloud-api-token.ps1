<#
.SYNOPSIS
    Actualiza el token de acceso de WhatsApp Cloud API (Meta) en .env.local y en la BD, y verifica permisos.
.DESCRIPTION
    1. Lee el token nuevo (parametro -Token, env WHATSAPP_NEW_TOKEN, o prompt seguro)
    2. Lo encripta con AES-GCM (mismo esquema del backend: salt + Rfc2898DeriveBytes 65536/SHA256)
    3. Actualiza APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN en .env.local (con respaldo .bak)
    4. Actualiza encrypted_access_token en la tabla channel_account (docker exec psql)
    5. Verifica el token contra Graph API (GET phone_number_id) y reporta si tiene permisos
    6. Opcional: recrea el contenedor backend-java (-RestartBackend)
.EXAMPLE
    .\scripts\update-cloud-api-token.ps1 -Token "EAAG..."
    .\scripts\update-cloud-api-token.ps1 -RestartBackend
#>

[CmdletBinding()]
param(
    [string]$Token = "",
    [switch]$RestartBackend,
    [switch]$SkipGraphCheck
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$EnvFile = ".env.local"
$ComposeFile = "docker-compose.local.yml"
$DbContainer = "asistente-postgres"
$GraphVersion = "v23.0"
$PhoneNumberId = "1173085549230681"
$SaltText = "WhatsAppCloudApiTokenEncryption"

function Get-EnvValue {
    param([string]$Key, [string]$FilePath)
    $line = Get-Content -Path $FilePath | Where-Object { $_ -match "^$([regex]::Escape($Key))=" } | Select-Object -First 1
    if (-not $line) { return "" }
    return $line.Substring($line.IndexOf("=") + 1)
}

function Set-EnvValue {
    param([string]$Key, [string]$Value, [string]$FilePath)
    $lines = Get-Content -Path $FilePath
    $pattern = "^$([regex]::Escape($Key))="
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            $lines[$i] = "$Key=$Value"
            Set-Content -Path $FilePath -Value $lines -Encoding UTF8
            return
        }
    }
    Add-Content -Path $FilePath -Value "$Key=$Value" -Encoding UTF8
}

function Protect-Token {
    param([string]$PlainToken, [string]$Secret)
    $salt = [System.Text.Encoding]::UTF8.GetBytes($SaltText)
    $derive = [System.Security.Cryptography.Rfc2898DeriveBytes]::new($Secret, $salt, 65536,
        [System.Security.Cryptography.HashAlgorithmName]::SHA256)
    $key = $derive.GetBytes(32)
    $iv = New-Object byte[] 12
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($iv)
    $plain = [System.Text.Encoding]::UTF8.GetBytes($PlainToken)
    $ct = New-Object byte[] $plain.Length
    $tag = New-Object byte[] 16
    $aes = [System.Security.Cryptography.AesGcm]::new($key)
    $aes.Encrypt($iv, $plain, $ct, $tag)
    $combined = New-Object byte[] ($iv.Length + $ct.Length + $tag.Length)
    [System.Buffer]::BlockCopy($iv, 0, $combined, 0, $iv.Length)
    [System.Buffer]::BlockCopy($ct, 0, $combined, $iv.Length, $ct.Length)
    [System.Buffer]::BlockCopy($tag, 0, $combined, $iv.Length + $ct.Length, $tag.Length)
    return [System.Convert]::ToBase64String($combined)
}

# 1) Obtener token
if (-not $Token) { $Token = $env:WHATSAPP_NEW_TOKEN }
if (-not $Token) {
    $secure = Read-Host "Pega el token nuevo de Meta (no se mostrara)" -AsSecureString
    $Token = [System.Net.NetworkCredential]::new("", $secure).Password
}
$Token = $Token.Trim()
if (-not $Token) { throw "No se proporciono token." }
Write-Host "Token recibido: $($Token.Length) caracteres, prefijo $($Token.Substring(0, [Math]::Min(4, $Token.Length)))..."

# 2) Encriptar
$encryptionSecret = Get-EnvValue -Key "APP_WHATSAPP_CLOUD_API_CREDENTIAL_ENCRYPTION_SECRET" -FilePath $EnvFile
if (-not $encryptionSecret) { throw "Falta APP_WHATSAPP_CLOUD_API_CREDENTIAL_ENCRYPTION_SECRET en $EnvFile" }
$encrypted = Protect-Token -PlainToken $Token -Secret $encryptionSecret
Write-Host "Token encriptado: $($encrypted.Length) chars base64"

# 3) Actualizar .env.local (con respaldo)
$backup = "$EnvFile.bak-token-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item -Path $EnvFile -Destination $backup
Set-EnvValue -Key "APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN" -Value $Token -FilePath $EnvFile
Write-Host ".env.local actualizado (respaldo en $backup)"

# 4) Actualizar BD
$sql = "UPDATE channel_account SET encrypted_access_token = :'tok', token_expires_at = NULL WHERE provider_name = 'META_CLOUD_API';"
$null = $encrypted | docker exec -i $DbContainer psql -U assistant -d asistente_whatsapp -v tok="$encrypted" -c $sql
if ($LASTEXITCODE -ne 0) { throw "Fallo la actualizacion en la BD (docker exec psql)." }
Write-Host "BD actualizada: encrypted_access_token en channel_account (META_CLOUD_API)"

# 5) Verificar contra Graph API
if (-not $SkipGraphCheck) {
    Write-Host "Verificando permisos contra Graph API..."
    try {
        $r = Invoke-RestMethod -Uri "https://graph.facebook.com/$GraphVersion/$PhoneNumberId?fields=id,display_phone_number,verified_name" `
            -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 20
        Write-Host "OK: token con permisos -> id=$($r.id) display=$($r.display_phone_number) verified=$($r.verified_name)" -ForegroundColor Green
    } catch {
        $detail = $_.ErrorDetails.Message
        if ($detail -match '"error_subcode":\s*33' -or $detail -match 'missing permissions') {
            Write-Warning "El token NO tiene permisos sobre el numero (error 100/33). Genera el token de sistema en WhatsApp Manager -> Configuracion de API con el permiso whatsapp_business_messaging."
        } elseif ($detail -match '"code":\s*190') {
            Write-Warning "El token parece invalido o vencido (error 190). Verifica que lo copiaste completo."
        } else {
            Write-Warning "Respuesta inesperada de Graph API: $detail"
        }
    }
}

# 6) Recrear backend si se pide
if ($RestartBackend) {
    Write-Host "Recreando contenedor backend-java..."
    docker compose --env-file $EnvFile -f $ComposeFile up -d --force-recreate backend-java 2>&1 | Select-Object -Last 2
    if ($LASTEXITCODE -ne 0) { Write-Warning "No se pudo recrear el backend. Revisa docker compose." }
    else { Write-Host "Backend recreado. Verifica health con: curl http://localhost:8080/actuator/health" -ForegroundColor Green }
}

Write-Host "Listo." -ForegroundColor Green
