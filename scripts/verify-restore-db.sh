#!/usr/bin/env bash
# =============================================================================
# VERIFY RESTORE DB - PostgreSQL (Linux/macOS)
# Uso: ./scripts/verify-restore-db.sh -d <db_restaurada> [--self-reference]
# Compara la base restaurada contra la base de referencia (asistente_whatsapp):
#   tablas, FKs validas, secuencias, defaults nextval, Flyway, tablas de control
#   y orfandad de registros (huerfanos por FK). Exit 0 = OK, 1 = al menos una
#   comprobacion fallo.
# Con --self-reference compara contra si misma (para el swap de la principal).
# =============================================================================
set -euo pipefail

REF_DB="asistente_whatsapp"
TARGET_DB=""
SELF_REFERENCE=0

while [ $# -gt 0 ]; do
  case "$1" in
    -d|--db) TARGET_DB="$2"; shift 2 ;;
    --self-reference) SELF_REFERENCE=1; shift ;;
    *) echo "ERROR: argumento desconocido $1"; exit 1 ;;
  esac
done
[ -n "$TARGET_DB" ] || { echo "ERROR: falta -d <db_restaurada>"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.local.yml"
ENV_FILE="$SCRIPT_DIR/.env.local"
PG_CONTAINER="asistente-postgres"
PG_USER="$(docker exec "$PG_CONTAINER" printenv POSTGRES_USER 2>/dev/null | tr -d '\r' || true)"
PG_PASS="$(docker exec "$PG_CONTAINER" printenv POSTGRES_PASSWORD 2>/dev/null | tr -d '\r' || true)"
[ -n "$PG_USER" ] || { echo "ERROR: no se pudo leer POSTGRES_USER de $PG_CONTAINER"; exit 1; }

if [ "$SELF_REFERENCE" -eq 1 ]; then
  REF_DB="$TARGET_DB"
fi

PG() {
  docker exec -e "PGPASSWORD=$PG_PASS" "$PG_CONTAINER" psql -h localhost -U "$PG_USER" -d "$1" -t -A -c "$2" 2>/dev/null | tr -d '\r'
}

FAIL=0
check() {
  local label="$1" expected="$2" actual="$3" detail="$4"
  if [ "$expected" = "$actual" ]; then
    echo "[PASS] $label ($detail)"
  else
    echo "[FAIL] $label: esperado=$expected real=$actual ($detail)"
    FAIL=1
  fi
}

echo "=== Verificacion de restauracion: $TARGET_DB (referencia: $REF_DB) ==="

# 1. Conectividad
check "conexion" "1" "$(PG "$TARGET_DB" "SELECT 1;")" "SELECT 1"

# 2. Tablas publicas
NT_REF="$(PG "$REF_DB" "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';")"
NT_TGT="$(PG "$TARGET_DB" "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';")"
check "tablas_publicas" "$NT_REF" "$NT_TGT" "ref=$NT_REF"

# 3. Foreign keys validas
FK_REF="$(PG "$REF_DB" "SELECT count(*) FROM information_schema.referential_constraints rc JOIN information_schema.table_constraints tc ON tc.constraint_schema=rc.constraint_schema AND tc.constraint_name=rc.constraint_name WHERE tc.constraint_type='FOREIGN KEY' AND tc.constraint_schema='public';")"
FK_TGT="$(PG "$TARGET_DB" "SELECT count(*) FROM information_schema.referential_constraints rc JOIN information_schema.table_constraints tc ON tc.constraint_schema=rc.constraint_schema AND tc.constraint_name=rc.constraint_name WHERE tc.constraint_type='FOREIGN KEY' AND tc.constraint_schema='public';")"
check "fk_validas" "$FK_REF" "$FK_TGT" "ref=$FK_REF"

# 4. Secuencias
SQ_REF="$(PG "$REF_DB" "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema='public';")"
SQ_TGT="$(PG "$TARGET_DB" "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema='public';")"
check "secuencias" "$SQ_REF" "$SQ_TGT" "ref=$SQ_REF"

# 5. Defaults nextval (no deberian existir; PKs son UUID)
NV_REF="$(PG "$REF_DB" "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_default LIKE 'nextval(%';")"
NV_TGT="$(PG "$TARGET_DB" "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_default LIKE 'nextval(%';")"
check "defaults_nextval" "$NV_REF" "$NV_TGT" "ref=$NV_REF"

# 6. Flyway: max installed_rank y success
FR_REF="$(PG "$REF_DB" "SELECT max(installed_rank) FROM flyway_schema_history;")"
FR_TGT="$(PG "$TARGET_DB" "SELECT max(installed_rank) FROM flyway_schema_history;")"
check "flyway_max_installed_rank" "$FR_REF" "$FR_TGT" "ref=$FR_REF"
FS_REF="$(PG "$REF_DB" "SELECT count(*) FROM flyway_schema_history WHERE success=true;")"
FS_TGT="$(PG "$TARGET_DB" "SELECT count(*) FROM flyway_schema_history WHERE success=true;")"
check "flyway_success" "$FS_REF" "$FS_TGT" "ref=$FS_REF"

# 7. Flyway cadena version:checksum
CHAIN_REF="$(PG "$REF_DB" "SELECT string_agg(version||'='||checksum, ',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success=true;")"
CHAIN_TGT="$(PG "$TARGET_DB" "SELECT string_agg(version||'='||checksum, ',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success=true;")"
check "flyway_cadena" "$CHAIN_REF" "$CHAIN_TGT" "checksums V1..V105"

# 8. Tablas de control: conteo de filas
CTL_TABLES="business,business_location,aesthetic_service,aesthetic_professional,booking,customer,conversation,ai_intent,ai_canonical_entity,channel_account,business_ai_settings,agenda_block"
ctl_snapshot() {
  local db="$1" out="" n
  IFS=',' read -ra T <<< "$CTL_TABLES"
  for t in "${T[@]}"; do
    n="$(PG "$db" "SELECT count(*) FROM \"$t\";")"
    out="${out}${t}=${n},"
  done
  echo "${out%,}"
}
CTL_REF="$(ctl_snapshot "$REF_DB")"
CTL_TGT="$(ctl_snapshot "$TARGET_DB")"
check "control_conteos" "$CTL_REF" "$CTL_TGT" "tablas: $CTL_TABLES"

# 9. Huerfanos por FK: genera SQL que cuenta huerfanos por restriccion y lo ejecuta
FK_LIST="$(PG "$TARGET_DB" "SELECT c.conrelid::regclass::text || '|' || r.relname || '|' || a.attname || '|' || b.attname FROM pg_constraint c JOIN pg_class r ON r.oid=c.confrelid JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey) JOIN pg_attribute b ON b.attrelid=c.confrelid AND b.attnum=ANY(c.confkey) WHERE c.contype='f' AND n.nspname='public';")"
ORPHAN_COUNT=0
if [ -n "$FK_LIST" ]; then
  ORPH_SQL=""
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    IFS='|' read -r child_table parent_table child_col parent_col <<< "$line"
    ORPH_SQL="${ORPH_SQL}SELECT '$child_table' AS tabla, count(*) AS h FROM \"$child_table\" ch LEFT JOIN \"$parent_table\" p ON p.\"$parent_col\" = ch.\"$child_col\" WHERE p.\"$parent_col\" IS NULL AND ch.\"$child_col\" IS NOT NULL UNION ALL "
  done <<< "$FK_LIST"
  ORPH_SQL="${ORPH_SQL%UNION ALL }"
  ORPH_REF="$(PG "$REF_DB" "SELECT coalesce(sum(t.h),0) FROM ($ORPH_SQL) t;")"
  ORPH_TGT="$(PG "$TARGET_DB" "SELECT coalesce(sum(t.h),0) FROM ($ORPH_SQL) t;")"
  check "orfandad_fk" "$ORPH_REF" "$ORPH_TGT" "huerfanos ref=$ORPH_REF"
else
  echo "[PASS] orfandad_fk (sin FKs que verificar)"
fi

if [ "$FAIL" -eq 1 ]; then
  echo "VERIFICACION: FALLIDA"
  exit 1
fi
echo "VERIFICACION: COMPLETADA (OK)"
exit 0
