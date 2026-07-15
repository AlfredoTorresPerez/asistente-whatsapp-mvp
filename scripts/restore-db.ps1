# =============================================================================
# RESTORE DB - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\restore-db.ps1 -BackupFile <path>
# Ejemplo: .\scripts\restore-db.ps1 -BackupFile .\backups\asistente_whatsapp_20260715_120000.sql.gz
# =============================================================================
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile
)

if (-not (Test-Path -LiteralPath $BackupFile)) {
    Write-Error "Archivo no encontrado: $BackupFile"
    exit 1
}

$db = $env:PGDATABASE ?? "asistente_whatsapp"
Write-Host "ADVERTENCIA: Esto sobrescribira la base de datos '$db'."
$confirm = Read-Host "Continuar? (s/N)"
if ($confirm -ne "s" -and $confirm -ne "S") {
    Write-Host "Restauracion cancelada."
    exit 0
}

$env:PGPASSWORD = $env:PGPASSWORD ?? "assistant"
$hostname = $env:PGHOST ?? "localhost"
$port = $env:PGPORT ?? "5433"
$user = $env:PGUSER ?? "assistant"

# Terminar conexiones activas
& psql -h $hostname -p $port -U $user -d $db -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$db' AND pid <> pg_backend_pid();" 2>$null

# Restaurar
gunzip -c $BackupFile | & psql -h $hostname -p $port -U $user -d $db

Write-Host "Restauracion completada desde: $BackupFile"
