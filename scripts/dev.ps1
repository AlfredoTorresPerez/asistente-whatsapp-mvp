<#
.SYNOPSIS
    Script de desarrollo para Asistente WhatsApp MVP
    Atajo para comandos docker compose frecuentes.

.PARAMETER Command
    up          - Inicia postgres + backend + frontend
    up:whatsapp - Inicia todos los servicios incluido WhatsApp Web
    logs        - Sigue logs de todos los servicios
    down        - Detiene servicios (sin borrar volúmenes)
    reset       - Borra volúmenes, reconstruye y levanta
    ps          - Lista estado de servicios
    build       - Reconstruye imágenes sin cache
    restart     - Reinicia servicios

.EXAMPLE
    .\scripts\dev.ps1 up
    .\scripts\dev.ps1 up:whatsapp
    .\scripts\dev.ps1 logs
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet('up', 'up:whatsapp', 'logs', 'down', 'reset', 'ps', 'build', 'restart')]
    [string]$Command = 'up'
)

$RootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ComposeFile = Join-Path $RootDir "docker-compose.local.yml"

if (-not (Test-Path $ComposeFile)) {
    Write-Error "No se encuentra docker-compose.local.yml en $RootDir"
    exit 1
}

switch ($Command) {
    'up' {
        Write-Host "=== Levantando servicios: postgres, backend, frontend ===" -ForegroundColor Cyan
        docker compose -f $ComposeFile up -d
    }
    'up:whatsapp' {
        Write-Host "=== Levantando todos los servicios (incluye WhatsApp Web) ===" -ForegroundColor Cyan
        docker compose -f $ComposeFile --profile whatsapp up -d
    }
    'logs' {
        docker compose -f $ComposeFile logs -f --tail=100
    }
    'down' {
        Write-Host "=== Deteniendo servicios (volúmenes preservados) ===" -ForegroundColor Yellow
        docker compose -f $ComposeFile down
    }
    'reset' {
        Write-Host "=== Reset total: borrando volúmenes + reconstruyendo ===" -ForegroundColor Red
        docker compose -f $ComposeFile down -v
        docker compose -f $ComposeFile up --build -d
    }
    'ps' {
        docker compose -f $ComposeFile ps
    }
    'build' {
        Write-Host "=== Reconstruyendo imágenes ===" -ForegroundColor Cyan
        docker compose -f $ComposeFile build --no-cache
    }
    'restart' {
        Write-Host "=== Reiniciando servicios ===" -ForegroundColor Yellow
        docker compose -f $ComposeFile restart
    }
}
