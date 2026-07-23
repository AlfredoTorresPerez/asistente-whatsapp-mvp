<#
.SYNOPSIS
    Detiene el tunel publico HTTPS sin afectar los servicios locales
.DESCRIPTION
    Detiene SOLO el contenedor cloudflared.
    PostgreSQL, backend y frontend siguen corriendo.
.EXAMPLE
    .\scripts\stop-public-link.ps1
#>

$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$ComposeFile = "docker-compose.local.yml"
$EnvFile = ".env.local"
$Profile = "public-link"
$ServiceName = "public-tunnel"

Write-Host "Deteniendo tunel publico..."
docker compose --env-file "$EnvFile" -f $ComposeFile --profile $Profile stop $ServiceName

if ($LASTEXITCODE -eq 0) {
    Write-Host "Tunel detenido. Los servicios locales continuan funcionando." -ForegroundColor Green
    Write-Host "Para reiniciar: .\scripts\start-public-link.ps1" -ForegroundColor Gray
} else {
    Write-Error "Error al detener el tunel"
    exit 1
}
