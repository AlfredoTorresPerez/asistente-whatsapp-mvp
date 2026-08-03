#!/usr/bin/env bash
# =============================================================================
# TEST RECOVERY NEGATIVE - PostgreSQL (Linux/macOS)
# Uso: ./scripts/test-recovery-negative.sh [backup_dir]
# Pruebas negativas de la capacidad de recuperacion (Fase 9):
#   1. Archivo vacio -> restore falla y no deja BD temporal
#   2. Suma SHA-256 incorrecta -> restore rechaza el respaldo
#   3. Archivo truncado -> pg_restore falla; limpieza de temporal
#   4. Falta de espacio (proxy: dir no escribible) -> backup falla limpio
#   5. PostgreSQL no disponible -> backup y restore fallan; recuperacion al volver
#   6. Migracion incompatible (Flyway V999) -> backend NO arranca
# Exit 0 si todas pasan; exit 1 si alguna falla.
# =============================================================================
set -u

BACKUP_DIR="${1:-./backups}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"
PG_CONTAINER="asistente-postgres"
PG_USER="$(docker exec "$PG_CONTAINER" printenv POSTGRES_USER 2>/dev/null | tr -d '\r' || true)"
PG_PASS="$(docker exec "$PG_CONTAINER" printenv POSTGRES_PASSWORD 2>/dev/null | tr -d '\r' || true)"
BACKUP_DIR="$(realpath "$BACKUP_DIR")"
FAILED=0
TMPW="$(mktemp -d)"

report() {
  local test="$1" ok="$2" detail="$3"
  if [ "$ok" = "1" ]; then
    echo "[PASS] $test: $detail"
  else
    echo "[FAIL] $test: $detail"
    FAILED=1
  fi
}

tempdb_count() {
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -t -A -c "SELECT count(*) FROM pg_database WHERE datname LIKE 'asistente_whatsapp_restore_%';" 2>/dev/null | tr -d '\r'
}

echo "Preparando respaldo de referencia ..."
REAL_BACKUP="$(ls -t "$BACKUP_DIR"/asistente_whatsapp_*.dump 2>/dev/null | head -1 || true)"
if [ -z "$REAL_BACKUP" ]; then
  "$SCRIPT_DIR/backup-db.sh" "$BACKUP_DIR"
  REAL_BACKUP="$(ls -t "$BACKUP_DIR"/asistente_whatsapp_*.dump 2>/dev/null | head -1)"
fi
echo "Referencia: $REAL_BACKUP"

# ---------- 1. Archivo vacio ----------
echo; echo "=== 1. Archivo vacio ==="
EMPTY="$TMPW/empty.dump"
: > "$EMPTY"
EMPTY_HASH="$(sha256sum "$EMPTY" | awk '{print $1}')"
echo "$EMPTY_HASH  empty.dump" > "$EMPTY.sha256"
BEFORE="$(tempdb_count)"
"$SCRIPT_DIR/restore-db.sh" "$EMPTY" >/dev/null 2>&1
RC=$?
AFTER="$(tempdb_count)"
report "archivo_vacio" "$([ "$RC" -ne 0 ] && echo 1 || echo 0)" "restore exit=$RC (esperado != 0)"
report "archivo_vacio_sin_BD_temporal" "$([ "$BEFORE" = "$AFTER" ] && echo 1 || echo 0)" "temporales antes=$BEFORE despues=$AFTER"

# ---------- 2. Suma incorrecta ----------
echo; echo "=== 2. Suma SHA-256 incorrecta ==="
REAL_SHA_FILE="$REAL_BACKUP.sha256"
REAL_SHA_CONTENT="$(cat "$REAL_SHA_FILE")"
printf '%064d  %s\n' 0 "$(basename "$REAL_BACKUP")" > "$REAL_SHA_FILE"
BEFORE="$(tempdb_count)"
"$SCRIPT_DIR/restore-db.sh" "$REAL_BACKUP" >/dev/null 2>&1
RC=$?
printf '%s\n' "$REAL_SHA_CONTENT" > "$REAL_SHA_FILE"
AFTER="$(tempdb_count)"
report "suma_incorrecta" "$([ "$RC" -ne 0 ] && echo 1 || echo 0)" "restore exit=$RC (esperado != 0)"
report "suma_incorrecta_sin_BD_temporal" "$([ "$BEFORE" = "$AFTER" ] && echo 1 || echo 0)" "temporales antes=$BEFORE despues=$AFTER"

# ---------- 3. Archivo truncado ----------
echo; echo "=== 3. Archivo truncado ==="
TRUNC="$TMPW/truncated.dump"
head -c 1024 "$REAL_BACKUP" > "$TRUNC"
TRUNC_HASH="$(sha256sum "$TRUNC" | awk '{print $1}')"
echo "$TRUNC_HASH  truncated.dump" > "$TRUNC.sha256"
BEFORE="$(tempdb_count)"
"$SCRIPT_DIR/restore-db.sh" "$TRUNC" >/dev/null 2>&1
RC=$?
AFTER="$(tempdb_count)"
report "archivo_truncado" "$([ "$RC" -ne 0 ] && echo 1 || echo 0)" "restore exit=$RC (esperado != 0, pg_restore debe fallar)"
report "archivo_truncado_limpieza" "$([ "$BEFORE" = "$AFTER" ] && echo 1 || echo 0)" "temporales antes=$BEFORE despues=$AFTER"

# ---------- 4. Falta de espacio (proxy) ----------
echo; echo "=== 4. Falta de espacio (proxy: dir no escribible) ==="
BLOCKER="$TMPW/blocker.txt"
touch "$BLOCKER"
"$SCRIPT_DIR/backup-db.sh" "$BLOCKER/subdir" >/dev/null 2>&1
RC=$?
report "falta_espacio" "$([ "$RC" -ne 0 ] && echo 1 || echo 0)" "backup exit=$RC (esperado != 0, fallo de escritura manejado limpio)"

# ---------- 5. PostgreSQL no disponible ----------
echo; echo "=== 5. PostgreSQL no disponible ==="
docker stop "$PG_CONTAINER" >/dev/null 2>&1
sleep 3
"$SCRIPT_DIR/backup-db.sh" "$BACKUP_DIR" >/dev/null 2>&1
RC_BK=$?
"$SCRIPT_DIR/restore-db.sh" "$REAL_BACKUP" >/dev/null 2>&1
RC_RS=$?
report "postgres_caido_backup" "$([ "$RC_BK" -ne 0 ] && echo 1 || echo 0)" "backup exit=$RC_BK (esperado != 0)"
report "postgres_caido_restore" "$([ "$RC_RS" -ne 0 ] && echo 1 || echo 0)" "restore exit=$RC_RS (esperado != 0)"

docker start "$PG_CONTAINER" >/dev/null 2>&1
PG_UP=0
for i in $(seq 1 30); do
  if docker exec "$PG_CONTAINER" pg_isready -U "$PG_USER" -d postgres -h localhost >/dev/null 2>&1; then PG_UP=1; break; fi
  sleep 5
done
report "postgres_recuperado" "$PG_UP" "pg_isready OK tras el reinicio"
rm -rf "$TMPW"

# ---------- 6. Migracion incompatible (Flyway V999) ----------
echo; echo "=== 6. Migracion incompatible (Flyway V999) ==="
"$SCRIPT_DIR/restore-db.sh" "$REAL_BACKUP" >/dev/null 2>&1 || { echo "[FAIL] restore de referencia no disponible"; exit 1; }
TEMP_DB="$(docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -t -A -c "SELECT datname FROM pg_database WHERE datname LIKE 'asistente_whatsapp_restore_%' ORDER BY datname DESC LIMIT 1;" | tr -d '\r')"
docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$TEMP_DB" -c "UPDATE flyway_schema_history SET version='999' WHERE installed_rank=(SELECT max(installed_rank) FROM flyway_schema_history);" >/dev/null 2>&1
"$SCRIPT_DIR/restore-backend-check.sh" -d "$TEMP_DB" --expect-failure --failure-pattern "flyway"
RC_BE=$?
report "migracion_incompatible" "$([ "$RC_BE" -eq 0 ] && echo 1 || echo 0)" "backend-check exit=$RC_BE (esperado 0: fallo esperado confirmado)"
docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "DROP DATABASE IF EXISTS $TEMP_DB;" >/dev/null 2>&1

echo; echo "=== RESUMEN PRUEBAS NEGATIVAS ==="
if [ "$FAILED" -eq 1 ]; then
  echo "PRUEBAS NEGATIVAS: FALLIDAS (exit 1)"
  exit 1
fi
echo "PRUEBAS NEGATIVAS: TODAS APROBADAS (exit 0)"
exit 0
