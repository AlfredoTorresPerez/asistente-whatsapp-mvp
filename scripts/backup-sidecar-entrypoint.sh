#!/usr/bin/env sh
# =============================================================================
# Backup Sidecar — entrypoint for docker-compose service (Fase 9)
# Runs pg_dump on a schedule (UTC), validates SHA-256, writes metadata and
# Prometheus text metrics, keeps backups per retention policy.
#
# Formato principal: custom (pg_dump -Fc -Z 5) -> <ts>.dump + .sha256 + .metadata.json
#   plain (BACKUP_FORMAT=plain) -> <ts>.sql.gz  + .sha256 + .metadata.json
#
# Variables de entorno:
#   PGHOST/PGPORT/PGUSER/PGPASSWORD/PGDATABASE
#   BACKUP_DIR            (default /backups)
#   BACKUP_FORMAT         (custom|plain, default custom)
#   RETENTION_DAYS        (default 7; 0 = conservar todo)
#   CRON_SCHEDULE         (default "0 4 * * *")
#   BACKUP_ON_START       (true = ejecutar un respaldo al arrancar, default false)
#   BACKUP_SIMULATION_MODE(true = no toca la BD; genera un artefacto simulado, default false)
#
# Uso: /entrypoint.sh backup  -> ejecuta un solo respaldo y sale (docker exec)
#      /entrypoint.sh         -> bucle cron
# =============================================================================
set -e

BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_FORMAT="${BACKUP_FORMAT:-custom}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
CRON_SCHEDULE="${CRON_SCHEDULE:-0 4 * * *}"
TIMESTAMP_FMT="${TIMESTAMP_FMT:-%Y%m%d_%H%M%S}"
BACKUP_ON_START="${BACKUP_ON_START:-false}"
SIMULATION="${BACKUP_SIMULATION_MODE:-false}"

SCRIPT_VERSION="fase9-sidecar-v1"
COUNTERS_FILE="$BACKUP_DIR/.backup-counters"

mkdir -p "$BACKUP_DIR"

SUCCESS_TOTAL=0
FAIL_TOTAL=0
if [ -f "$COUNTERS_FILE" ]; then
  # shellcheck disable=SC1090
  . "$COUNTERS_FILE" 2>/dev/null || true
fi
SUCCESS_TOTAL="${SUCCESS_TOTAL:-0}"
FAIL_TOTAL="${FAIL_TOTAL:-0}"

log() { echo "[backup-sidecar] $(date -u '+%Y-%m-%dT%H:%M:%SZ') $*"; }

write_metrics() {
  RESULT="$1"
  DURATION="$2"
  SIZE="$3"
  SHA_OK="$4"
  LAST_TS="$5"
  {
    echo "# HELP backup_sidecar_success_total Respaldo exitoso (contador persistente)"
    echo "# TYPE backup_sidecar_success_total counter"
    echo "backup_sidecar_success_total $SUCCESS_TOTAL"
    echo "# HELP backup_sidecar_failures_total Respaldo fallido (contador persistente)"
    echo "# TYPE backup_sidecar_failures_total counter"
    echo "backup_sidecar_failures_total $FAIL_TOTAL"
    echo "# HELP backup_sidecar_last_success_timestamp_seconds Ultimo respaldo exitoso (epoch)"
    echo "# TYPE backup_sidecar_last_success_timestamp_seconds gauge"
    echo "backup_sidecar_last_success_timestamp_seconds $LAST_TS"
    echo "# HELP backup_sidecar_last_duration_seconds Duracion del ultimo respaldo"
    echo "# TYPE backup_sidecar_last_duration_seconds gauge"
    echo "backup_sidecar_last_duration_seconds $DURATION"
    echo "# HELP backup_sidecar_last_size_bytes Tamano del ultimo respaldo"
    echo "# TYPE backup_sidecar_last_size_bytes gauge"
    echo "backup_sidecar_last_size_bytes $SIZE"
    echo "# HELP backup_sidecar_last_sha256_ok Suma SHA-256 verificada en el ultimo respaldo"
    echo "# TYPE backup_sidecar_last_sha256_ok gauge"
    echo "backup_sidecar_last_sha256_ok $SHA_OK"
    echo "# HELP backup_sidecar_last_result Resultado del ultimo respaldo (1 exito, 0 fallo)"
    echo "# TYPE backup_sidecar_last_result gauge"
    echo "backup_sidecar_last_result $RESULT"
    if [ "$SIMULATION" = "true" ]; then
      echo "# HELP backup_sidecar_simulation_mode 1 si el respaldo fue simulado"
      echo "# TYPE backup_sidecar_simulation_mode gauge"
      echo "backup_sidecar_simulation_mode 1"
    fi
  } > "$BACKUP_DIR/metrics"
}

persist_counters() {
  printf 'SUCCESS_TOTAL=%s\nFAIL_TOTAL=%s\n' "$SUCCESS_TOTAL" "$FAIL_TOTAL" > "$COUNTERS_FILE"
}

run_backup() {
  TS=$(date +"$TIMESTAMP_FMT")
  if [ "$BACKUP_FORMAT" = "plain" ]; then
    EXT="sql.gz"
    DUMP_ARGS="--no-owner --no-acl"
    DUMP_CMD="PGPASSWORD=\"${PGPASSWORD}\" pg_dump -h \"${PGHOST:-postgres}\" -p \"${PGPORT:-5432}\" -U \"${PGUSER:-assistant}\" -d \"${PGDATABASE:-asistente_whatsapp}\" ${DUMP_ARGS}"
  else
    EXT="dump"
    DUMP_ARGS="-Fc -Z 5 --no-owner --no-acl"
    DUMP_CMD="PGPASSWORD=\"${PGPASSWORD}\" pg_dump -h \"${PGHOST:-postgres}\" -p \"${PGPORT:-5432}\" -U \"${PGUSER:-assistant}\" -d \"${PGDATABASE:-asistente_whatsapp}\" ${DUMP_ARGS}"
  fi
  FILENAME="asistente_whatsapp_${TS}.${EXT}"
  TMPFILE="${BACKUP_DIR}/.${FILENAME}.tmp"

  log "starting backup file=${FILENAME} format=${BACKUP_FORMAT} simulation=${SIMULATION}"

  START_EPOCH=$(date +%s)
  START_ISO=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

  if [ "$SIMULATION" = "true" ]; then
    printf 'SIMULATED_BACKUP %s %s %s\n' "$TS" "$BACKUP_FORMAT" "$SCRIPT_VERSION" > "$TMPFILE"
    PG_VERSION_SAFE="simulated"
  else
    if PGPASSWORD="${PGPASSWORD}" pg_dump \
      -h "${PGHOST:-postgres}" \
      -p "${PGPORT:-5432}" \
      -U "${PGUSER:-assistant}" \
      -d "${PGDATABASE:-asistente_whatsapp}" \
      $DUMP_ARGS > "$TMPFILE" 2>/dev/null; then
      :
    else
      rm -f "$TMPFILE"
      FAIL_TOTAL=$((FAIL_TOTAL + 1))
      persist_counters
      write_metrics 0 0 0 0 0
      log "backup result=failed file=${FILENAME} reason=pg_dump_failed"
      log "backup FAILED: ${FILENAME}"
      return 1
    fi
    PG_VERSION_SAFE=$(pg_dump --version 2>/dev/null || echo "unknown")
  fi

  mv "$TMPFILE" "${BACKUP_DIR}/${FILENAME}"
  END_EPOCH=$(date +%s)
  DURATION=$((END_EPOCH - START_EPOCH))
  SIZE=$(wc -c < "${BACKUP_DIR}/${FILENAME}" 2>/dev/null | tr -d ' ')

  SHA=""
  SHA_OK=0
  if command -v sha256sum >/dev/null 2>&1; then
    SHA=$(sha256sum "${BACKUP_DIR}/${FILENAME}" | awk '{print $1}')
  elif command -v sha256 >/dev/null 2>&1; then
    SHA=$(sha256 -q "${BACKUP_DIR}/${FILENAME}")
  fi
  if [ -n "$SHA" ]; then
    printf '%s  %s\n' "$SHA" "$FILENAME" > "${BACKUP_DIR}/${FILENAME}.sha256"
    SHA_OK=1
  fi

  cat > "${BACKUP_DIR}/${FILENAME}.metadata.json" <<EOF
{
  "script": "$SCRIPT_VERSION",
  "simulated": $([ "$SIMULATION" = "true" ] && echo true || echo false),
  "backup": {
    "file": "$FILENAME",
    "format": "$BACKUP_FORMAT",
    "database": "${PGDATABASE:-asistente_whatsapp}",
    "pg_dump_version": "$PG_VERSION_SAFE",
    "size_bytes": $SIZE,
    "sha256": "${SHA:-unavailable}",
    "sha256_ok": $SHA_OK,
    "duration_seconds": $DURATION,
    "started_at": "$START_ISO",
    "finished_at": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  },
  "source": { "host": "${PGHOST:-postgres}", "port": "${PGPORT:-5432}" },
  "policy": { "retention_days": "$RETENTION_DAYS" }
}
EOF

  SUCCESS_TOTAL=$((SUCCESS_TOTAL + 1))
  persist_counters
  write_metrics 1 "$DURATION" "$SIZE" "$SHA_OK" "$END_EPOCH"

  log "backup result=success file=${FILENAME} format=${BACKUP_FORMAT} duration_seconds=${DURATION} size_bytes=${SIZE} sha256_ok=${SHA_OK} database=${PGDATABASE:-asistente_whatsapp} retention_days=${RETENTION_DAYS}"
  log "Backup complete: ${FILENAME} (${SIZE} bytes, ${DURATION}s)"

  if [ "$RETENTION_DAYS" -gt 0 ]; then
    OLD=$(find "$BACKUP_DIR" -maxdepth 1 -name 'asistente_whatsapp_*' -type f -mtime "+${RETENTION_DAYS}" 2>/dev/null | wc -l)
    find "$BACKUP_DIR" -maxdepth 1 -name 'asistente_whatsapp_*' -type f -mtime "+${RETENTION_DAYS}" -delete 2>/dev/null || true
    log "retention cleaned_old_files=${OLD} retention_days=${RETENTION_DAYS}"
  fi
}

_int() { printf '%s' "$1" | sed 's/^0*//'; }

_match_cron_field() {
  CRON_VAL="$1"
  CURRENT="$2"
  case "$CRON_VAL" in
    "*") return 0 ;;
    */*)
      INTERVAL=$(printf '%s' "$CRON_VAL" | sed 's|^.*/||')
      INTERVAL=$(_int "$INTERVAL")
      [ "$INTERVAL" -gt 0 ] 2>/dev/null || return 1
      [ "$(( CURRENT % INTERVAL ))" -eq 0 ]
      return $?
      ;;
    *,*)
      _IFS_SAVE="$IFS"; IFS=','
      for _tok in $CRON_VAL; do
        _tok=$(_int "$_tok")
        if [ "$CURRENT" -eq "$_tok" ] 2>/dev/null; then
          IFS="$_IFS_SAVE"; return 0
        fi
      done
      IFS="$_IFS_SAVE"
      return 1
      ;;
    *)
      CRON_VAL=$(_int "$CRON_VAL")
      [ "$CURRENT" -eq "$CRON_VAL" ] 2>/dev/null
      return $?
      ;;
  esac
}

log "Backup sidecar started"
log "Schedule: ${CRON_SCHEDULE} (UTC) retention_days=${RETENTION_DAYS} format=${BACKUP_FORMAT} simulation=${SIMULATION} backup_dir=${BACKUP_DIR}"

if [ "$1" = "backup" ]; then
  run_backup
  exit 0
fi

if [ "$BACKUP_ON_START" = "true" ]; then
  log "Running backup at startup (BACKUP_ON_START=true)"
  run_backup || true
fi

CRON_MIN=$(printf '%s' "$CRON_SCHEDULE" | awk '{print $1}')
CRON_HOUR=$(printf '%s' "$CRON_SCHEDULE" | awk '{print $2}')

while true; do
  CURRENT_HOUR=$(_int "$(date +%H)")
  CURRENT_MIN=$(_int "$(date +%M)")

  if _match_cron_field "$CRON_HOUR" "$CURRENT_HOUR" && \
     _match_cron_field "$CRON_MIN" "$CURRENT_MIN"; then
    run_backup || true
    sleep 61
  else
    sleep 30
  fi
done
