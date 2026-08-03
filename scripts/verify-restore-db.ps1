# =============================================================================
# VERIFY RESTORE DB - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\verify-restore-db.ps1 -DbName <bd_restaurada> [-ReferenceDb <bd_referencia>]
# Valida la base restaurada contra la base de referencia:
#   1. Conectividad de ambas bases
#   2. Cantidad de tablas
#   3. Cantidad de restricciones FOREIGN KEY (validados)
#   4. Cantidad de secuencias
#   5. Defaults nextval (identity/sequences en columnas)
#   6. Flyway: historial completo (version:checksum encadenado), ultima migracion
#      aplicada y registros exitosos
#   7. Registros de control: conteos de tablas criticas vs referencia
#   8. Integridad referencial: 0 filas huerfanas en TODAS las FKs
# Exit 0 si todo OK; exit 1 si alguna validacion falla.
# =============================================================================
param(
  [Parameter(Mandatory = $true)][string]$DbName,
  [string]$ReferenceDb = ""
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "docker-compose.local.yml"
$EnvFile = Join-Path $Root ".env.local"
$PgContainer = "asistente-postgres"

$failed = $false
function Check {
  param([string]$Label, [string]$Ref, [string]$Got, [switch]$Compare)
  if ($Compare -and $Ref -ne $Got) {
    Write-Host "  [FAIL] $Label (ref=$Ref vs restaurada=$Got)" -ForegroundColor Red
    $script:failed = $true
  } else {
    Write-Host "  [OK]   $Label = $Got" -ForegroundColor Green
  }
}

function Get-PgScalar {
  param([string]$Db, [string]$Query)
  $out = docker compose --env-file $EnvFile -f $ComposeFile exec -T -e "PGPASSWORD=$script:pgPass" postgres psql -h localhost -U $script:pgUser -d $Db -t -A -v ON_ERROR_STOP=1 -c $Query 2>$null
  return ($out | Out-String).Trim()
}

# Credenciales y nombres de base desde el contenedor
$pgUser = (docker exec $PgContainer printenv POSTGRES_USER 2>$null | Out-String).Trim()
$pgDb   = (docker exec $PgContainer printenv POSTGRES_DB 2>$null | Out-String).Trim()
$pgPass = (docker exec $PgContainer printenv POSTGRES_PASSWORD 2>$null | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($ReferenceDb)) { $ReferenceDb = $pgDb }

if ($DbName -eq $ReferenceDb) { Write-Error "DbName y ReferenceDb son la misma base ($DbName). Verifica siempre contra otra base."; exit 1 }

Write-Host "Verificando base restaurada '$DbName' contra referencia '$ReferenceDb'..." -ForegroundColor Cyan

# 1. Conectividad
$refOk = Get-PgScalar -Db $ReferenceDb -Query "SELECT 1"
if ($LASTEXITCODE -ne 0 -or $refOk -ne "1") { Write-Host "  [FAIL] No se pudo conectar a la base de referencia '$ReferenceDb'" -ForegroundColor Red; exit 1 }
$gotOk = Get-PgScalar -Db $DbName -Query "SELECT 1"
if ($LASTEXITCODE -ne 0 -or $gotOk -ne "1") { Write-Host "  [FAIL] No se pudo conectar a la base restaurada '$DbName'" -ForegroundColor Red; exit 1 }
Write-Host "  [OK]   Conectividad a ambas bases" -ForegroundColor Green

# 2. Tablas
$refTables = Get-PgScalar -Db $ReferenceDb -Query "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';"
$gotTables = Get-PgScalar -Db $DbName -Query "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';"
Check -Label "Cantidad de tablas" -Ref $refTables -Got $gotTables -Compare

# 3. FKs (solo validadas)
$refFk = Get-PgScalar -Db $ReferenceDb -Query "SELECT count(*) FROM pg_constraint WHERE contype='f' AND convalidated;"
$gotFk = Get-PgScalar -Db $DbName -Query "SELECT count(*) FROM pg_constraint WHERE contype='f' AND convalidated;"
Check -Label "Restricciones FOREIGN KEY (validados)" -Ref $refFk -Got $gotFk -Compare

# 4. Secuencias
$refSeq = Get-PgScalar -Db $ReferenceDb -Query "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='S' AND n.nspname='public';"
$gotSeq = Get-PgScalar -Db $DbName -Query "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='S' AND n.nspname='public';"
Check -Label "Secuencias (public)" -Ref $refSeq -Got $gotSeq -Compare

# 5. Defaults nextval
$refNext = Get-PgScalar -Db $ReferenceDb -Query "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_default LIKE 'nextval(%';"
$gotNext = Get-PgScalar -Db $DbName -Query "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_default LIKE 'nextval(%';"
Check -Label "Columnas con default nextval" -Ref $refNext -Got $gotNext -Compare

# 6. Flyway
$refFwRank = Get-PgScalar -Db $ReferenceDb -Query "SELECT max(installed_rank) FROM flyway_schema_history;"
$gotFwRank = Get-PgScalar -Db $DbName -Query "SELECT max(installed_rank) FROM flyway_schema_history;"
Check -Label "Flyway: ultima migracion (installed_rank)" -Ref $refFwRank -Got $gotFwRank -Compare

$refFwOk = Get-PgScalar -Db $ReferenceDb -Query "SELECT count(*) FROM flyway_schema_history WHERE success;"
$gotFwOk = Get-PgScalar -Db $DbName -Query "SELECT count(*) FROM flyway_schema_history WHERE success;"
Check -Label "Flyway: migraciones exitosas" -Ref $refFwOk -Got $gotFwOk -Compare

$refFwChain = Get-PgScalar -Db $ReferenceDb -Query "SELECT string_agg(version || ':' || COALESCE(checksum::text,'null'), ',' ORDER BY installed_rank) FROM flyway_schema_history;"
$gotFwChain = Get-PgScalar -Db $DbName -Query "SELECT string_agg(version || ':' || COALESCE(checksum::text,'null'), ',' ORDER BY installed_rank) FROM flyway_schema_history;"
Check -Label "Flyway: cadena version:checksum completa" -Ref $refFwChain.Substring(0, [Math]::Min(60, $refFwChain.Length)) -Got $gotFwChain.Substring(0, [Math]::Min(60, $gotFwChain.Length)) -Compare

# 7. Registros de control (tablas criticas)
$controlTables = @(
  "business", "business_location", "business_ai_settings", "business_policy", "business_user",
  "aesthetic_service", "aesthetic_professional", "aesthetic_service_location", "aesthetic_professional_location",
  "booking", "booking_status_history", "booking_confirmation_link", "booking_reschedule_link", "booking_cancellation_link",
  "customer", "conversation", "message", "channel_account",
  "ai_intent", "ai_intent_expression", "ai_canonical_entity", "ai_entity_alias",
  "agenda_block", "agenda_business_hours", "agenda_professional_hours", "agenda_room",
  "user_account", "role", "permission", "order_request", "lead"
)
$badControl = 0
foreach ($t in $controlTables) {
  $rc = Get-PgScalar -Db $ReferenceDb -Query "SELECT count(*) FROM $t;"
  $gc = Get-PgScalar -Db $DbName -Query "SELECT count(*) FROM $t;"
  if ($rc -ne $gc) {
    Write-Host "  [FAIL] Control $t (ref=$rc vs restaurada=$gc)" -ForegroundColor Red
    $failed = $true; $badControl++
  }
}
if ($badControl -eq 0) {
  Write-Host "  [OK]   Registros de control: $($controlTables.Count) tablas criticas con conteos identicos" -ForegroundColor Green
}

# 8. Integridad referencial (0 huerfanos en todas las FKs)
$sqlFile = Join-Path $env:TEMP "orphan_check_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
@'
DO $$
DECLARE r record; bad int := 0; total int := 0;
BEGIN
  FOR r IN
    SELECT tc.table_name AS t, kcu.column_name AS c,
           ccu.table_name AS ft, ccu.column_name AS fc
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage ccu
      ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
  LOOP
    EXECUTE format('SELECT count(*) FROM %I t LEFT JOIN %I ft ON t.%I = ft.%I WHERE ft.%I IS NULL',
                   r.t, r.ft, r.c, r.fc, r.fc) INTO bad;
    total := total + bad;
  END LOOP;
  RAISE NOTICE 'TOTAL_ORPHANS %', total;
END $$;
'@ | Set-Content -LiteralPath $sqlFile -Encoding ascii
docker cp $sqlFile "${PgContainer}:/tmp/orphan_check.sql" 2>$null
function Invoke-OrphanCheck {
  param([string]$Db)
  $out = docker compose --env-file $EnvFile -f $ComposeFile exec -T -e "PGPASSWORD=$script:pgPass" postgres psql -h localhost -U $script:pgUser -d $Db -f /tmp/orphan_check.sql 2>&1
  $exit = $LASTEXITCODE
  $text = ($out | ForEach-Object { $_.ToString() } | Out-String)
  $orphans = if ($text -match 'TOTAL_ORPHANS (\d+)') { $matches[1] } else { "?" }
  return [pscustomobject]@{ Exit = $exit; Orphans = $orphans }
}
$orphRef = Invoke-OrphanCheck -Db $ReferenceDb
$orphGot = Invoke-OrphanCheck -Db $DbName
docker compose --env-file $EnvFile -f $ComposeFile exec -T postgres rm -f /tmp/orphan_check.sql 2>$null | Out-Null
Remove-Item -LiteralPath $sqlFile -Force -ErrorAction SilentlyContinue
if ($orphGot.Exit -eq 0 -and $orphRef.Exit -eq 0 -and $orphRef.Orphans -eq $orphGot.Orphans) {
  Write-Host "  [OK]   Integridad referencial: huerfanos identicos a la referencia (TOTAL_ORPHANS=$($orphGot.Orphans))" -ForegroundColor Green
} else {
  Write-Host "  [FAIL] Integridad referencial: referencia=$($orphRef.Orphans) restaurada=$($orphGot.Orphans) (exit ref=$($orphRef.Exit) got=$($orphGot.Exit))" -ForegroundColor Red
  $failed = $true
}

Write-Host ""
if ($failed) {
  Write-Host "VERIFICACION DE RESTAURACION: FALLIDA (exit 1)" -ForegroundColor Red
  exit 1
} else {
  Write-Host "VERIFICACION DE RESTAURACION: OK (exit 0)" -ForegroundColor Green
  exit 0
}
