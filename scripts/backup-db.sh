#!/usr/bin/env bash
# =============================================================================
# BACKUP DB - PostgreSQL (Linux/macOS)
# Uso: ./scripts/backup-db.sh [output_dir]   (BACKUP_FORMAT=custom|plain, RETENTION_DAYS)
# Genera asistente_whatsapp_<ts>.dump|.sql.gz + .sha256 + .metadata.json + metrics
# Usa pg_dump del contenedor postgres del stack local.
# =============================================================================
set -euo pipefail

OUTPUT_DIR="${1:-./backups}"
FORMAT="${BACKUP_FORMAT:-custom}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
SIMULATION="${BACKUP_SIMULATION_MODE:-false}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.local.yml"
ENV_FILE="$SCRIPT_DIR/.env.local"
PG_CONTAINER="asistente-postgres"

if [ "$SIMULATION" = "true" ]; then
  echo "SIMULACION (sin ejecutar nada)"
  echo "  pg_dump -Fc -Z 5 --no-owner --no-acl -d <db> -> asistente_whatsapp_<ts>.dump"
  echo "  sha256sum + metadatos + metrics + retencion ${RETENTION_DAYS} dias"
  exit 0
fi

[ -f "$COMPOSE_FILE" ] || { echo "ERROR: no se encuentra $COMPOSE_FILE"; exit 1; }
[ -f "$ENV_FILE" ] || { echo "ERROR: no se encuentra $ENV_FILE"; exit 1; }

PG_USER="$(docker exec "$PG_CONTAINER" printenv POSTGRES_USER 2>/dev/null | tr -d '\r' || true)"
PG_DB="$(docker exec "$PG_CONTAINER" printenv POSTGRES_DB 2>/dev/null | tr -d '\r' || true)"
PG_PASS="$(docker exec "$PG_CONTAINER" printenv POSTGRES_PASSWORD 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PG_USER" ] || [ -z "$PG_DB" ]; then
  echo "ERROR: no se pudo leer POSTGRES_USER/POSTGRES_DB del contenedor $PG_CONTAINER (stack local arriba?)"
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
TS=$(date +%Y%m%d_%H%M%S)
EXT="dump"
[ "$FORMAT" = "plain" ] && EXT="sql.gz"
FILENAME="asistente_whatsapp_${TS}.${EXT}"
CONTAINER_TMP="/tmp/$FILENAME"
START=$(date +%s)

echo "Respaldo PostgreSQL (formato: $FORMAT) -> $FILENAME ..."
if [ "$FORMAT" = "plain" ]; then
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T -e "PGPASSWORD=$PG_PASS" postgres \
    sh -c "pg_dump -h localhost -U '$PG_USER' -d '$PG_DB' --no-owner --no-acl | gzip -f - > '$CONTAINER_TMP'" 2>/dev/null
else
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T -e "PGPASSWORD=$PG_PASS" postgres \
    sh -c "pg_dump -h localhost -U '$PG_USER' -d '$PG_DB' -Fc -Z 5 --no-owner --no-acl -f '$CONTAINER_TMP'" 2>/dev/null
fi
RC=$?
if [ "$RC" -ne 0 ]; then
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres rm -f "$CONTAINER_TMP" 2>/dev/null || true
  echo "ERROR: pg_dump fallo (exit=$RC)"
  exit 1
fi

docker cp "${PG_CONTAINER}:${CONTAINER_TMP}" "$OUTPUT_DIR/$FILENAME" 2>/dev/null
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres rm -f "$CONTAINER_TMP" 2>/dev/null || true
[ -f "$OUTPUT_DIR/$FILENAME" ] || { echo "ERROR: no se pudo copiar el respaldo"; exit 1; }

echo "Validando integridad del respaldo..."
VALID=0
if [ "$FORMAT" = "plain" ]; then
  if gzip -t "$OUTPUT_DIR/$FILENAME" 2>/dev/null; then VALID=1; fi
else
  INSPECT="/tmp/${FILENAME}_inspect"
  docker cp "$OUTPUT_DIR/$FILENAME" "${PG_CONTAINER}:${INSPECT}" 2>/dev/null
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres pg_restore -l "$INSPECT" >/dev/null 2>&1
  [ $? -eq 0 ] && VALID=1
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres rm -f "$INSPECT" 2>/dev/null || true
fi
if [ "$VALID" -ne 1 ]; then
  echo "ERROR: el respaldo no paso la validacion de integridad"
  exit 1
fi

SHA="$(sha256sum "$OUTPUT_DIR/$FILENAME" | awk '{print $1}')"
printf '%s  %s\n' "$SHA" "$FILENAME" > "$OUTPUT_DIR/$FILENAME.sha256"
END=$(date +%s)
DUR=$((END - START))
SIZE=$(wc -c < "$OUTPUT_DIR/$FILENAME" | tr -d ' ')

cat > "$OUTPUT_DIR/$FILENAME.metadata.json" <<EOF
{
  "script": "backup-db.sh",
  "simulated": false,
  "backup": {
    "file": "$FILENAME",
    "format": "$FORMAT",
    "database": "$PG_DB",
    "source_host": "$PG_CONTAINER",
    "size_bytes": $SIZE,
    "sha256": "$SHA",
    "sha256_ok": 1,
    "duration_seconds": $DUR,
    "started_at": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  },
  "policy": { "retention_days": "$RETENTION_DAYS" }
}
EOF

cat > "$OUTPUT_DIR/metrics" <<EOF
# HELP backup_sidecar_last_success_timestamp_seconds Ultimo respaldo exitoso (epoch)
# TYPE backup_sidecar_last_success_timestamp_seconds gauge
backup_sidecar_last_success_timestamp_seconds $(date +%s)
# HELP backup_sidecar_last_duration_seconds Duracion del ultimo respaldo
# TYPE backup_sidecar_last_duration_seconds gauge
backup_sidecar_last_duration_seconds $DUR
# HELP backup_sidecar_last_size_bytes Tamano del ultimo respaldo
# TYPE backup_sidecar_last_size_bytes gauge
backup_sidecar_last_size_bytes $SIZE
# HELP backup_sidecar_last_sha256_ok Suma SHA-256 verificada
# TYPE backup_sidecar_last_sha256_ok gauge
backup_sidecar_last_sha256_ok 1
# HELP backup_sidecar_last_result Resultado (1 exito, 0 fallo)
# TYPE backup_sidecar_last_result gauge
backup_sidecar_last_result 1
EOF

if [ "$RETENTION_DAYS" -gt 0 ]; then
  find "$OUTPUT_DIR" -maxdepth 1 -name 'asistente_whatsapp_*' -type f -mtime "+$RETENTION_DAYS" -delete 2>/dev/null || true
fi

echo "Backup creado: $OUTPUT_DIR/$FILENAME"
echo "  sha256: $SHA"
echo "  duracion: ${DUR}s | tamano: $SIZE bytes"
exit 0
