# =============================================================================
# BACKUP DB - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\backup-db.ps1 [[-OutputDir] <string>]
# Ejemplo: .\scripts\backup-db.ps1 -OutputDir C:\backups
# Nota: usa pg_dump del host si existe; si no, cae a `docker compose exec postgres`.
# =============================================================================
param(
    [string]$OutputDir = ".\backups"
)

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Filename = "asistente_whatsapp_$Timestamp.sql.gz"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$env:PGPASSWORD = $env:PGPASSWORD ?? "assistant"
$hostname = $env:PGHOST ?? "localhost"
$port = $env:PGPORT ?? "5433"
$user = $env:PGUSER ?? "assistant"
$db = $env:PGDATABASE ?? "asistente_whatsapp"

$dumpFile = Join-Path $OutputDir $Filename

$pgDump = Get-Command pg_dump -ErrorAction SilentlyContinue
if ($pgDump) {
    Write-Host "Usando pg_dump del host..." -ForegroundColor DarkGray
    & pg_dump -h $hostname -p $port -U $user -d $db --no-owner --no-acl | & gzip -c > $dumpFile
    if ($LASTEXITCODE -ne 0) { Write-Error "pg_dump fallo (exit code: $LASTEXITCODE)"; exit 1 }
} else {
    Write-Host "pg_dump no esta en el PATH. Usando docker compose exec postgres..." -ForegroundColor DarkGray
    $root = Split-Path -Parent $PSScriptRoot
    $dumpTmp = docker compose --env-file (Join-Path $root ".env.local") -f (Join-Path $root "docker-compose.local.yml") exec -T postgres pg_dump -U $user -d $db --no-owner --no-acl 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Error "No se pudo ejecutar pg_dump via docker compose exec postgres. Revisa que el stack local este corriendo."
        exit 1
    }
    $dumpTmp | & gzip -c > $dumpFile
}

Write-Host "Backup creado: $dumpFile"
