#!/usr/bin/env bash
# =============================================================================
# RESTORE DB - PostgreSQL
# Uso: ./scripts/restore-db.sh <backup_file> [output_dir]
# Ejemplo: ./scripts/restore-db.sh backups/asistente_whatsapp_20260715_120000.sql.gz
# =============================================================================
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Uso: $0 <backup_file>"
    echo "Ejemplo: $0 backups/asistente_whatsapp_20260715_120000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Archivo no encontrado: $BACKUP_FILE"
    exit 1
fi

echo "ADVERTENCIA: Esto sobrescribira la base de datos '${PGDATABASE:-asistente_whatsapp}'."
read -rp "¿Continuar? (s/N): " confirm
if [ "$confirm" != "s" ] && [ "$confirm" != "S" ]; then
    echo "Restauracion cancelada."
    exit 0
fi

PGPASSWORD="${PGPASSWORD:-assistant}" psql \
  -h "${PGHOST:-localhost}" \
  -p "${PGPORT:-5433}" \
  -U "${PGUSER:-assistant}" \
  -d "${PGDATABASE:-asistente_whatsapp}" \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${PGDATABASE:-asistente_whatsapp}' AND pid <> pg_backend_pid();" 2>/dev/null || true

gunzip -c "$BACKUP_FILE" | PGPASSWORD="${PGPASSWORD:-assistant}" psql \
  -h "${PGHOST:-localhost}" \
  -p "${PGPORT:-5433}" \
  -U "${PGUSER:-assistant}" \
  -d "${PGDATABASE:-asistente_whatsapp}"

echo "Restauracion completada desde: $BACKUP_FILE"
