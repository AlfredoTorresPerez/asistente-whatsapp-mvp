<#
.SYNOPSIS
    Verifica el estado del tunel publico HTTPS
.DESCRIPTION
    - Estado del contenedor cloudflared
    - Ultima URL trycloudflare.com detectada en logs
    - Verificacion HTTP real contra la URL detectada
    - Estado del frontend y backend locales
.EXAMPLE
    .\scripts\check-public-link.ps1
#>

$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$ComposeFile = "docker-compose.local.yml"
$EnvFile = ".env.local"
$Profile = "public-link"
$ServiceName = "public-tunnel"

function Write-Result {
    param([string]$Label, [string]$Status, [string]$Color = "Gray")
    Write-Host ("  {0,-30} " -f $Label) -NoNewline
    Write-Host $Status -ForegroundColor $Color
}

function Get-TunnelUrlFromLogs {
    $logs = docker compose --env-file "$EnvFile" -f $ComposeFile logs --no-color --tail=200 $ServiceName 2>$null
    $pattern = 'https://[-a-zA-Z0-9.]+\.trycloudflare\.com'
    $urls = @()
    foreach ($line in $logs) {
        $match = [regex]::Match($line, $pattern)
        if ($match.Success -and $match.Value -ne "https://api.trycloudflare.com") {
            $urls += $match.Value
        }
    }
    if ($urls.Count -eq 0) { return $null }
    return $urls[-1]
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  VERIFICACION DE TUNEL PUBLICO" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 1. Estado del contenedor
$containerStatus = docker ps --filter "name=asistente-public-tunnel" --format "{{.Status}}"
if ([string]::IsNullOrWhiteSpace($containerStatus)) {
    Write-Result "Contenedor cloudflared" "NO INICIADO" "Red"
    Write-Result "Tunel" "NO INICIADO" "Red"
} else {
    Write-Result "Contenedor cloudflared" $containerStatus "Green"

    # 2. Extraer URL de logs
    $url = Get-TunnelUrlFromLogs
    if ($url) {
        Write-Result "URL detectada en logs" $url "Green"

        # 3. Verificacion HTTP real
        try {
            $resp = Invoke-WebRequest -Uri $url -Method Get -TimeoutSec 10 -UseBasicParsing
            if ($resp.StatusCode -eq 200) {
                Write-Result "Verificacion HTTP" "VIGENTE (HTTP $($resp.StatusCode))" "Green"
                Write-Result "Tunel" "VIGENTE" "Green"
            } else {
                Write-Result "Verificacion HTTP" "RESPONDE (HTTP $($resp.StatusCode))" "Yellow"
                Write-Result "Tunel" "VIGENTE (con advertencias)" "Yellow"
            }
        } catch {
            if ($_.Exception.Response.StatusCode -eq 502) {
                Write-Result "Verificacion HTTP" "VIGENTE (HTTP 502 - inicio)" "Yellow"
                Write-Result "Tunel" "VIGENTE (iniciando...)" "Yellow"
            } else {
                Write-Result "Verificacion HTTP" "EXPIRADO ($($_.Exception.Message))" "Red"
                Write-Result "Tunel" "EXPIRADO" "Red"
            }
        }
    } else {
        Write-Result "URL detectada" "NO DISPONIBLE (cloudflared iniciando)" "Yellow"
        Write-Result "Tunel" "INICIANDO..." "Yellow"
    }
}

# 4. Estado del frontend local
try {
    $fe = Invoke-WebRequest -Uri "http://localhost:5173" -Method Get -TimeoutSec 5 -UseBasicParsing
    Write-Result "Frontend local" "OK (HTTP $($fe.StatusCode))" "Green"
} catch {
    Write-Result "Frontend local" "NO ACCESIBLE" "Red"
}

# 5. Estado del backend local
try {
    $be = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 5 -UseBasicParsing
    $data = $be.Content | ConvertFrom-Json
    Write-Result "Backend local" "OK ($($data.status))" "Green"
} catch {
    Write-Result "Backend local" "NO ACCESIBLE" "Red"
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

if ([string]::IsNullOrWhiteSpace($containerStatus)) {
    Write-Host "Para iniciar el tunel:" -ForegroundColor Yellow
    Write-Host "  .\scripts\start-public-link.ps1" -ForegroundColor Gray
} elseif ($url -and $null -eq (Get-TunnelUrlFromLogs)) {
    Write-Host "El tunel esta iniciando. Espera unos segundos y vuelve a ejecutar:" -ForegroundColor Yellow
    Write-Host "  .\scripts\check-public-link.ps1" -ForegroundColor Gray
} elseif (-not $url) {
    Write-Host "Para regenerar el enlace:" -ForegroundColor Yellow
    Write-Host "  docker compose --env-file .env.local -f docker-compose.local.yml --profile public-link up -d --force-recreate public-tunnel" -ForegroundColor Gray
    Write-Host "  .\scripts\check-public-link.ps1" -ForegroundColor Gray
}
