$ErrorActionPreference = "Stop"
if (Get-Variable PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$ComposeFile = "docker-compose.local.yml"
$EnvFile = ".env"
$Profile = "public-link"
$MaxAttempts = 90

function Write-Step {
    param([string]$Message)
    Write-Host $Message
}

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

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command,
        [Parameter(Mandatory = $true)]
        [string]$ErrorMessage
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError "$ErrorMessage Codigo de salida: $LASTEXITCODE"
    }
}

function Update-EnvValue {
    param(
        [string]$Key,
        [string]$Value,
        [string]$FilePath
    )

    if (-not (Test-Path $FilePath)) {
        New-Item -ItemType File -Path $FilePath | Out-Null
    }

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
}

function Get-TunnelUrl {
    $logs = docker compose -f $ComposeFile logs --no-color public-tunnel 2>$null
    $matches = $logs | Select-String -Pattern 'https://[-a-zA-Z0-9.]+\.trycloudflare\.com' -AllMatches
    $values = @()
    foreach ($matchLine in $matches) {
        foreach ($match in $matchLine.Matches) {
            if ($match.Value -ne "https://api.trycloudflare.com") {
                $values += $match.Value
            }
        }
    }
    if ($values.Count -eq 0) {
        return ""
    }
    return $values[-1]
}

Assert-Command "docker"

Write-Step "Levantando MVP local..."
Invoke-CheckedCommand -ErrorMessage "No se pudo levantar el MVP local." -Command {
    docker compose -f $ComposeFile up -d --build
}

Write-Step "Levantando tunel publico HTTPS para el frontend..."
Invoke-CheckedCommand -ErrorMessage "No se pudo levantar el tunel publico." -Command {
    docker compose -f $ComposeFile --profile $Profile up -d --force-recreate public-tunnel
}

Write-Step "Esperando URL publica del tunel..."
$publicUrl = ""
for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
    $publicUrl = Get-TunnelUrl
    if (-not [string]::IsNullOrWhiteSpace($publicUrl)) {
        break
    }
    Start-Sleep -Seconds 2
}

if ([string]::IsNullOrWhiteSpace($publicUrl)) {
    Stop-WithError "No se pudo obtener URL publica. Revisa: docker compose -f $ComposeFile logs public-tunnel"
}

$confirmationUrl = $publicUrl.TrimEnd('/') + "/reservas/confirmar"
$rescheduleUrl = $publicUrl.TrimEnd('/') + "/reservas/reprogramar"
$cancellationUrl = $publicUrl.TrimEnd('/') + "/reservas/cancelar"
$paymentUrl = $publicUrl.TrimEnd('/') + "/reservas/pagar"

@(
    "APP_FRONTEND_PUBLIC_BASE_URL=$publicUrl",
    "APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL=$confirmationUrl",
    "APP_BOOKING_RESCHEDULE_PUBLIC_BASE_URL=$rescheduleUrl",
    "APP_BOOKING_CANCELLATION_PUBLIC_BASE_URL=$cancellationUrl",
    "APP_BOOKING_PAYMENT_CHECKOUT_PUBLIC_BASE_URL=$paymentUrl",
    "VITE_API_BASE_URL=/api/v1",
    "APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES=60",
    "TZ=America/Santiago",
    "JAVA_TOOL_OPTIONS=-Duser.timezone=America/Santiago",
    "SPRING_JACKSON_TIME_ZONE=America/Santiago",
    "APP_TIME_ZONE=America/Santiago",
    "APP_METHOD_TRACING_MAX_PAYLOAD_LENGTH=600"
) | Set-Content -Path $EnvFile -Encoding ASCII

Write-Step "URL publica frontend: $publicUrl"
Write-Step "URL publica de confirmacion: $confirmationUrl"
Write-Step "URL publica de reprogramacion: $rescheduleUrl"
Write-Step "URL publica de pago: $paymentUrl"
Write-Step "Recreando backend con URL navegable..."
Invoke-CheckedCommand -ErrorMessage "No se pudo recrear el backend con la URL publica." -Command {
    docker compose -f $ComposeFile up -d --force-recreate backend-java whatsapp-web-service frontend-react
}

Write-Step "Listo. Los enlaces de confirmacion usaran: $confirmationUrl/{token}"
Write-Step "Listo. Los enlaces de reprogramacion usaran: $rescheduleUrl/{token}"
Write-Step "Listo. Los enlaces de reserva asistida usaran: $publicUrl/reservar?token=..."
Write-Step "Para ver el tunel: docker compose -f $ComposeFile logs -f public-tunnel"
