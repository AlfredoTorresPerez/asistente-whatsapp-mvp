# =============================================================================
# RESTORE DB - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\restore-db.ps1 -BackupFile <path> [-TargetDb <nombre>]
#                                [-RestoreToMain] [-DropAfterVerify] [-Simulation]
# Ejemplo: .\scripts\restore-db.ps1 -BackupFile .\backups\asistente_whatsapp_20260803_120000.dump
#          .\scripts\restore-db.ps1 -BackupFile .\backups\...dump -RestoreToMain
#
# Seguridad:
#   - Por defecto restaura en una BD TEMPORAL con nombre unico (nunca toca la principal).
#   - Verifica la suma SHA-256 (obligatoria: rechaza backups sin .sha256 o con suma distinta).
#   - Reemplazar la base principal solo con -RestoreToMain (opcion explicita) y
#     DOBLE CONFIRMACION (CONFIRMAR + nombre de la base). El swap usa rename atomico
#     solo despues de restaurar y verificar en temporal.
#   - Tras restaurar valida: Flyway, tablas, FKs, secuencias y registros de control
#     (invoca verify-restore-db.ps1).
# =============================================================================
param(
  [Parameter(Mandatory = $true)][string]$BackupFile,
  [string]$TargetDb = "",
  [switch]$RestoreToMain,
  [switch]$DropAfterVerify,
  [switch]$Simulation
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "docker-compose.local.yml"
$EnvFile = Join-Path $Root ".env.local"
$PgContainer = "asistente-postgres"

function Invoke-PgSql {
  param([string]$Db, [string]$Query, [string]$User)
  docker compose --env-file $EnvFile -f $ComposeFile exec -T -e "PGPASSWORD=$script:pgPass" postgres psql -h localhost -U $User -d $Db -t -A -v ON_ERROR_STOP=1 -c $Query 2>$null
}

function Invoke-PgAdmin {
  param([string]$Cmd, [string]$User)
  docker compose --env-file $EnvFile -f $ComposeFile exec -T -e "PGPASSWORD=$script:pgPass" postgres sh -c $Cmd
}

if ($Simulation) {
  Write-Host "SIMULACION (sin ejecutar nada)" -ForegroundColor Cyan
  Write-Host "  backup: $BackupFile"
  Write-Host "  checksum SHA-256 -> verificacion previa obligatoria" -ForegroundColor DarkGray
  Write-Host "  destino: $(if ($RestoreToMain) { '<base principal> (doble confirmacion + rename)' } else { '<temporal unico: asistente_whatsapp_restore_<ts>>' })" -ForegroundColor DarkGray
  Write-Host "  pg_restore --no-owner --no-acl -> verify-restore-db.ps1 (Flyway/tablas/FKs/secuencias/control)" -ForegroundColor DarkGray
  exit 0
}

if (-not (Test-Path -LiteralPath $BackupFile)) { Write-Error "Archivo no encontrado: $BackupFile"; exit 1 }
if (-not (Test-Path -LiteralPath $ComposeFile)) { Write-Error "No se encuentra $ComposeFile"; exit 1 }
if (-not (Test-Path -LiteralPath $EnvFile))    { Write-Error "No se encuentra $EnvFile"; exit 1 }

# Formato segun extension
$Ext = [System.IO.Path]::GetExtension($BackupFile).ToLowerInvariant()
if ($Ext -eq ".dump")      { $Format = "custom" }
elseif ($Ext -eq ".gz")    { $Format = "plain" }
else                       { Write-Error "Extension no soportada '$Ext'. Usa .dump (custom) o .sql.gz (plain)"; exit 1 }

# --- Credenciales desde el contenedor (no versionadas) ---
$pgUser = (docker exec $PgContainer printenv POSTGRES_USER 2>$null | Out-String).Trim()
$pgDb   = (docker exec $PgContainer printenv POSTGRES_DB 2>$null | Out-String).Trim()
$pgPass = (docker exec $PgContainer printenv POSTGRES_PASSWORD 2>$null | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($pgUser) -or [string]::IsNullOrWhiteSpace($pgDb)) {
  Write-Error "No se pudo leer POSTGRES_USER/POSTGRES_DB del contenedor $PgContainer. Revisa que el stack local este corriendo."
  exit 1
}

# --- 1. Suma SHA-256 obligatoria ---
$ShaFile = "$BackupFile.sha256"
if (-not (Test-Path -LiteralPath $ShaFile)) {
  Write-Error "No existe la suma SHA-256 ($ShaFile). Los respaldos sin suma no se restauran (politica de integridad)."
  exit 1
}
$expected = (Get-Content -LiteralPath $ShaFile -Raw).Trim() -split "\s+" | Select-Object -First 1
$actual = (Get-FileHash -LiteralPath $BackupFile -Algorithm SHA256).Hash.ToLowerInvariant()
if ($expected.ToLowerInvariant() -ne $actual) {
  Write-Error "Suma SHA-256 NO coincide (integridad del archivo comprometida). Esperada: $($expected.Substring(0,16))... obtenida: $($actual.Substring(0,16))..."
  exit 1
}
Write-Host "SHA-256 verificado OK" -ForegroundColor Green

# --- 2. Determinar destino ---
$restoreToMain = $RestoreToMain -or (-not [string]::IsNullOrWhiteSpace($TargetDb) -and $TargetDb.Trim() -eq $pgDb)
if ([string]::IsNullOrWhiteSpace($TargetDb)) {
  if ($RestoreToMain) { $TargetDb = $pgDb }
  else { $TargetDb = "asistente_whatsapp_restore_$(Get-Date -Format 'yyyyMMdd_HHmmss')" }
}
$isMain = ($TargetDb -eq $pgDb)

if ($isMain) {
  Write-Host "ADVERTENCIA: esta operacion REEMPLAZA la base principal '$pgDb'." -ForegroundColor Yellow
  $c1 = Read-Host "Confirmacion 1/2: escriba CONFIRMAR para continuar"
  if ($c1 -ne "CONFIRMAR") { Write-Host "Cancelado (confirmacion 1/2 no dada)."; exit 1 }
  $c2 = Read-Host "Confirmacion 2/2: escriba el nombre de la base a reemplazar ($pgDb)"
  if ($c2 -ne $pgDb) { Write-Host "Cancelado (confirmacion 2/2 no dada)."; exit 1 }
} else {
  $exists = (Invoke-PgSql -Db $TargetDb -Query "SELECT 1" -User $pgUser)
  if ($LASTEXITCODE -eq 0) { Write-Error "La base destino '$TargetDb' ya existe. Eliminala o usa otro nombre."; exit 1 }
}
# --- 3. Crear BD temporal (siempre; el swap a principal usa rename) ---
$tempName = if ($isMain) { "${pgDb}_restore_swap_$(Get-Date -Format 'yyyyMMdd_HHmmss')" } else { $TargetDb }
Invoke-PgAdmin -User $pgUser -Cmd "createdb -h localhost -U '$pgUser' -O '$pgUser' '$tempName'"
if ($LASTEXITCODE -ne 0) { Write-Error "No se pudo crear la base temporal '$tempName'"; exit 1 }
Write-Host "Base temporal creada: $tempName" -ForegroundColor Cyan

$started = Get-Date
$ContainerTmp = "/tmp/asistente_restore_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
docker cp $BackupFile "${PgContainer}:${ContainerTmp}" 2>$null
if ($LASTEXITCODE -ne 0) {
  Invoke-PgAdmin -User $pgUser -Cmd "dropdb -h localhost -U '$pgUser' --if-exists '$tempName'"
  Write-Error "No se pudo copiar el respaldo al contenedor"
  exit 1
}

# --- 4. Restaurar ---
$restoreOk = $false
if ($Format -eq "custom") {
  Invoke-PgAdmin -User $pgUser -Cmd "pg_restore -h localhost -U '$pgUser' -d '$tempName' --no-owner --no-acl '$ContainerTmp'"
  $restoreOk = ($LASTEXITCODE -eq 0)
} else {
  Invoke-PgAdmin -User $pgUser -Cmd "gunzip -c '$ContainerTmp' | psql -h localhost -U '$pgUser' -d '$tempName' -v ON_ERROR_STOP=1 -q"
  $restoreOk = ($LASTEXITCODE -eq 0)
}
docker compose --env-file $EnvFile -f $ComposeFile exec -T postgres rm -f "$ContainerTmp" 2>$null | Out-Null
if (-not $restoreOk) {
  Invoke-PgAdmin -User $pgUser -Cmd "dropdb -h localhost -U '$pgUser' --if-exists '$tempName'"
  Write-Error "La restauracion fallo; la base temporal '$tempName' fue eliminada."
  exit 1
}
$restoreSeconds = [math]::Round(((Get-Date) - $started).TotalSeconds, 1)
Write-Host "Restauracion completada en ${restoreSeconds}s ($tempName)" -ForegroundColor Green

# --- 5. Verificacion post-restauracion ---
$verifyScript = Join-Path $PSScriptRoot "verify-restore-db.ps1"
& $verifyScript -DbName $tempName -ReferenceDb $pgDb
if ($LASTEXITCODE -ne 0) {
  Invoke-PgAdmin -User $pgUser -Cmd "dropdb -h localhost -U '$pgUser' --if-exists '$tempName'"
  Write-Error "La verificacion de la base restaurada fallo; la base temporal '$tempName' fue eliminada."
  exit 1
}

# --- 6. Swap a principal (solo -RestoreToMain) ---
if ($isMain) {
  Invoke-PgAdmin -User $pgUser -Cmd "psql -h localhost -U '$pgUser' -d '$pgDb' -c `"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$pgDb' AND pid <> pg_backend_pid();`""
  Invoke-PgAdmin -User $pgUser -Cmd "dropdb -h localhost -U '$pgUser' '$pgDb'"
  if ($LASTEXITCODE -ne 0) { Write-Error "No se pudo eliminar la base principal (swap abortado, temporal '$tempName' conservada)"; exit 1 }
  Invoke-PgAdmin -User $pgUser -Cmd "psql -h localhost -U $pgUser -d postgres -c `"ALTER DATABASE $tempName RENAME TO $pgDb;`""
  if ($LASTEXITCODE -ne 0) { Write-Error "No se pudo renombrar la base temporal a '$pgDb' (swap abortado; temporal '$tempName' conservada)"; exit 1 }
  Write-Host "Swap completado: la base principal '$pgDb' fue reemplazada por la restaurada." -ForegroundColor Yellow
} elseif ($DropAfterVerify) {
  Invoke-PgAdmin -User $pgUser -Cmd "dropdb -h localhost -U '$pgUser' --if-exists '$tempName'"
  Write-Host "Base temporal '$tempName' eliminada tras la verificacion (-DropAfterVerify)." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Resumen:" -ForegroundColor Cyan
Write-Host "  backup:       $BackupFile"
Write-Host "  formato:      $Format"
Write-Host "  destino:      $($tempName)$(if ($isMain) { ' (base principal, swap)' } else { ' (temporal; conservada para verificacion funcional)' })"
Write-Host "  restauracion: ${restoreSeconds}s"
Write-Host "  verificacion: OK (Flyway, tablas, FKs, secuencias, registros de control)"
exit 0

