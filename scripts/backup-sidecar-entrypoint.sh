#!/usr/bin/env sh
# =============================================================================
# Backup Sidecar — entrypoint for docker-compose service
# Runs pg_dump on a schedule (UTC), keeps last 7 backups, logs to stdout.
# =============================================================================
set -e

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
CRON_SCHEDULE="${CRON_SCHEDULE:-0 4 * * *}"
TIMESTAMP_FMT="${TIMESTAMP_FMT:-%Y%m%d_%H%M%S}"

mkdir -p "$BACKUP_DIR"

log() { echo "[backup-sidecar] $(date '+%Y-%m-%d %H:%M:%S') $*"; }

__int() { printf '%s' "$1" | sed 's/^0*//'; }

run_backup() {
  TS=$(date +"$TIMESTAMP_FMT")
  FILENAME="asistente_whatsapp_${TS}.sql.gz"
  TMPFILE="${BACKUP_DIR}/.${FILENAME}.tmp"

  log "Starting backup -> ${FILENAME}"

  PGPASSWORD="${PGPASSWORD}" pg_dump \
    -h "${PGHOST:-postgres}" \
    -p "${PGPORT:-5432}" \
    -U "${PGUSER:-assistant}" \
    -d "${PGDATABASE:-asistente_whatsapp}" \
    --no-owner \
    --no-acl 2>/dev/null \
    | gzip > "$TMPFILE"

  mv "$TMPFILE" "${BACKUP_DIR}/${FILENAME}"
  SIZE=$(du -h "${BACKUP_DIR}/${FILENAME}" 2>/dev/null | cut -f1)
  log "Backup complete: ${FILENAME} (${SIZE})"

  if [ "$RETENTION_DAYS" -gt 0 ]; then
    log "Cleaning backups older than ${RETENTION_DAYS} days"
    find "$BACKUP_DIR" -name 'asistente_whatsapp_*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -delete
  fi
}

_match_cron_field() {
  CRON_VAL="$1"
  CURRENT="$2"
  case "$CRON_VAL" in
    "*") return 0 ;;
    */*)
      INTERVAL=$(printf '%s' "$CRON_VAL" | sed 's|^.*/||')
      INTERVAL=$(__int "$INTERVAL")
      [ "$INTERVAL" -gt 0 ] 2>/dev/null || return 1
      [ "$(( CURRENT % INTERVAL ))" -eq 0 ]
      return $?
      ;;
    *,*)
      _IFS_SAVE="$IFS"; IFS=','
      for _tok in $CRON_VAL; do
        _tok=$(__int "$_tok")
        if [ "$CURRENT" -eq "$_tok" ] 2>/dev/null; then
          IFS="$_IFS_SAVE"; return 0
        fi
      done
      IFS="$_IFS_SAVE"
      return 1
      ;;
    *)
      CRON_VAL=$(__int "$CRON_VAL")
      [ "$CURRENT" -eq "$CRON_VAL" ] 2>/dev/null
      return $?
      ;;
  esac
}

log "Backup sidecar started"
log "Schedule: ${CRON_SCHEDULE} (UTC)"
log "Retention: ${RETENTION_DAYS} days"
log "Backup dir: ${BACKUP_DIR}"

CRON_MIN=$(printf '%s' "$CRON_SCHEDULE" | awk '{print $1}')
CRON_HOUR=$(printf '%s' "$CRON_SCHEDULE" | awk '{print $2}')

while true; do
  CURRENT_HOUR=$(__int "$(date +%H)")
  CURRENT_MIN=$(__int "$(date +%M)")

  if _match_cron_field "$CRON_HOUR" "$CURRENT_HOUR" && \
     _match_cron_field "$CRON_MIN" "$CURRENT_MIN"; then
    run_backup
    sleep 61
  else
    sleep 30
  fi
done