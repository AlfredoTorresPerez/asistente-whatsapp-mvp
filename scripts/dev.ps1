<#
.SYNOPSIS
    Script de desarrollo para Asistente WhatsApp MVP
    Atajo para comandos docker compose frecuentes (docker-compose.local.yml).

.PARAMETER Command
    up          - Inicia postgres + backend + frontend (+ mailpit)
    logs        - Sigue logs de todos los servicios
    down        - Detiene servicios (sin borrar volúmenes, incluye perfiles)
    reset       - Borra volúmenes, reconstruye y levanta
    ps          - Lista estado de servicios
    build       - Reconstruye imágenes sin cache
    restart     - Reinicia servicios
    verify      - Ejecuta local-verify.ps1 (health + smoke test)

.EXAMPLE
    .\scripts\dev.ps1 up
    .\scripts\dev.ps1 logs
    .\scripts\dev.ps1 verify
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet('up', 'logs', 'down', 'reset', 'ps', 'build', 'restart', 'verify')]
    [string]$Command = 'up'
)

$RootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ComposeFile = Join-Path $RootDir "docker-compose.local.yml"
$EnvFile = Join-Path $RootDir ".env.local"

if (-not (Test-Path $ComposeFile)) {
    Write-Error "No se encuentra docker-compose.local.yml en $RootDir"
    exit 1
}

$ComposeBase = "docker compose --env-file `"$EnvFile`" -f `"$ComposeFile`""
$AllProfiles = @('--profile observability', '--profile monitoring', '--profile backup', '--profile public-link', '--profile https') -join ' '

switch ($Command) {
    'up' {
        Write-Host "=== Levantando servicios: postgres, backend, frontend, mailpit ===" -ForegroundColor Cyan
        Invoke-Expression "$ComposeBase up -d"
    }
    'logs' {
        Invoke-Expression "$ComposeBase logs -f --tail=100"
    }
    'down' {
        Write-Host "=== Deteniendo servicios (volúmenes preservados, todos los perfiles) ===" -ForegroundColor Yellow
        Invoke-Expression "$ComposeBase $AllProfiles down"
    }
    'reset' {
        Write-Host "=== Reset total: borrando volúmenes + reconstruyendo ===" -ForegroundColor Red
        Invoke-Expression "$ComposeBase $AllProfiles down -v"
        Invoke-Expression "$ComposeBase up --build -d"
    }
    'ps' {
        Invoke-Expression "$ComposeBase ps"
    }
    'build' {
        Write-Host "=== Reconstruyendo imágenes ===" -ForegroundColor Cyan
        Invoke-Expression "$ComposeBase build --no-cache"
    }
    'restart' {
        Write-Host "=== Reiniciando servicios ===" -ForegroundColor Yellow
        Invoke-Expression "$ComposeBase restart"
    }
    'verify' {
        & "$PSScriptRoot\local-verify.ps1"
        exit $LASTEXITCODE
    }
}
