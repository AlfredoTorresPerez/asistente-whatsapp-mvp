#!/usr/bin/env bash
# =============================================================================
# BACKUP DB - PostgreSQL
# Uso: ./scripts/backup-db.sh [output_dir]
# Ejemplo: ./scripts/backup-db.sh /backups
# =============================================================================
set -euo pipefail

OUTPUT_DIR="${1:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="asistente_whatsapp_${TIMESTAMP}.sql.gz"

mkdir -p "$OUTPUT_DIR"

PGPASSWORD="${PGPASSWORD:-assistant}" pg_dump \
  -h "${PGHOST:-localhost}" \
  -p "${PGPORT:-5433}" \
  -U "${PGUSER:-assistant}" \
  -d "${PGDATABASE:-asistente_whatsapp}" \
  --no-owner \
  --no-acl \
  | gzip > "${OUTPUT_DIR}/${FILENAME}"

echo "Backup creado: ${OUTPUT_DIR}/${FILENAME}"
echo "Tamaño: $(du -h "${OUTPUT_DIR}/${FILENAME}" | cut -f1)"
