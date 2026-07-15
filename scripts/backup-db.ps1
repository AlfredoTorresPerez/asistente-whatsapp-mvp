# =============================================================================
# BACKUP DB - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\backup-db.ps1 [[-OutputDir] <string>]
# Ejemplo: .\scripts\backup-db.ps1 -OutputDir C:\backups
# =============================================================================
param(
    [string]$OutputDir = ".\backups"
)

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Filename = "asistente_whatsapp_$Timestamp.sql.gz"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$env:PGPASSWORD = $env:PGPASSWORD ?? "assistant"

$pgDump = Get-Command pg_dump -ErrorAction SilentlyContinue
if (-not $pgDump) {
    Write-Error "pg_dump no encontrado. Instale PostgreSQL o agregue pg_dump al PATH."
    exit 1
}

$dumpFile = Join-Path $OutputDir $Filename
$hostname = $env:PGHOST ?? "localhost"
$port = $env:PGPORT ?? "5433"
$user = $env:PGUSER ?? "assistant"
$db = $env:PGDATABASE ?? "asistente_whatsapp"

& pg_dump -h $hostname -p $port -U $user -d $db --no-owner --no-acl | & gzip -c > $dumpFile

Write-Host "Backup creado: $dumpFile"
