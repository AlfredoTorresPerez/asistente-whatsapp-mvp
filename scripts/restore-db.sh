#!/usr/bin/env bash
# =============================================================================
# RESTORE DB - PostgreSQL (Linux/macOS)
# Uso:
#   ./scripts/restore-db.sh <backup.dump|.sql.gz>          # restaura en BD temporal
#   ./scripts/restore-db.sh <backup> -d <nombre>            # restaura en BD especifica
#   ./scripts/restore-db.sh <backup> --restore-to-main      # RUTA DESTRUCTIVA (doble confirmacion)
#   ./scripts/restore-db.sh <backup> --drop-after-verify    # elimina la temporal al verificar
#   ./scripts/restore-db.sh <backup> --simulation           # solo muestra lo que haria
# Verifica la suma SHA-256, restaura con pg_restore --no-owner --no-acl y
# valida la base resultante con verify-restore-db.sh.
# =============================================================================
set -euo pipefail

RESTORE_TO_MAIN=0
DROP_AFTER_VERIFY=0
SIMULATION=0
TARGET_DB=""
BACKUP_FILE=""

while [ $# -gt 0 ]; do
  case "$1" in
    -d|--target-db) TARGET_DB="$2"; shift 2 ;;
    --restore-to-main) RESTORE_TO_MAIN=1; shift ;;
    --drop-after-verify) DROP_AFTER_VERIFY=1; shift ;;
    --simulation) SIMULATION=1; shift ;;
    *) BACKUP_FILE="$1"; shift ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.local.yml"
ENV_FILE="$SCRIPT_DIR/.env.local"
PG_CONTAINER="asistente-postgres"

if [ -z "$BACKUP_FILE" ]; then
  echo "ERROR: falta el archivo de respaldo"
  echo "Uso: $0 <backup.dump|.sql.gz> [-d <db>|--restore-to-main] [--drop-after-verify] [--simulation]"
  exit 1
fi
[ -f "$BACKUP_FILE" ] || { echo "ERROR: no existe $BACKUP_FILE"; exit 1; }

PG_USER="$(docker exec "$PG_CONTAINER" printenv POSTGRES_USER 2>/dev/null | tr -d '\r' || true)"
PG_DB="$(docker exec "$PG_CONTAINER" printenv POSTGRES_DB 2>/dev/null | tr -d '\r' || true)"
PG_PASS="$(docker exec "$PG_CONTAINER" printenv POSTGRES_PASSWORD 2>/dev/null | tr -d '\r' || true)"
[ -n "$PG_USER" ] && [ -n "$PG_DB" ] || { echo "ERROR: no se pudo leer credenciales del contenedor $PG_CONTAINER"; exit 1; }

FILENAME="$(basename "$BACKUP_FILE")"
SHA_FILE="${BACKUP_FILE}.sha256"

if [ ! -f "$SHA_FILE" ]; then
  echo "ERROR: falta $SHA_FILE (la verificacion de integridad es obligatoria)"
  exit 1
fi
EXPECTED_SHA="$(awk '{print $1}' "$SHA_FILE" | tr '[:upper:]' '[:lower:]')"
ACTUAL_SHA="$(sha256sum "$BACKUP_FILE" | awk '{print $1}')"
if [ "$EXPECTED_SHA" != "$ACTUAL_SHA" ]; then
  echo "ERROR: suma SHA-256 no coincide (esperada $EXPECTED_SHA, real $ACTUAL_SHA)"
  exit 1
fi
echo "Integridad SHA-256 verificada: $ACTUAL_SHA"

case "$FILENAME" in
  *.sql.gz) FORMAT="plain" ;;
  *.dump) FORMAT="custom" ;;
  *) echo "ERROR: extension desconocida (use .dump o .sql.gz)"; exit 1 ;;
esac

if [ "$RESTORE_TO_MAIN" -eq 1 ]; then
  if [ "$SIMULATION" -eq 1 ]; then
    echo "SIMULACION: restauraria $FILENAME sobre la base principal $PG_DB (destructivo)"
    exit 0
  fi
  echo "ATENCION: esta operacion REEMPLAZARA la base principal '$PG_DB'."
  echo "Para confirmar escriba CONFIRMAR y presione Enter:"
  read -r CONFIRM
  if [ "$CONFIRM" != "CONFIRMAR" ]; then
    echo "Cancelado."
    exit 1
  fi
  echo "Escriba el nombre exacto de la base a reemplazar ('$PG_DB'):"
  read -r CONFIRM2
  if [ "$CONFIRM2" != "$PG_DB" ]; then
    echo "Cancelado (nombre no coincide)."
    exit 1
  fi
  TARGET_DB="$PG_DB"
else
  if [ -z "$TARGET_DB" ]; then
    TARGET_DB="asistente_whatsapp_restore_$(date +%Y%m%d_%H%M%S)"
  fi
fi

if [ "$SIMULATION" -eq 1 ]; then
  echo "SIMULACION:"
  echo "  backup   : $BACKUP_FILE ($FORMAT)"
  echo "  destino  : $TARGET_DB"
  echo "  main     : $PG_DB (intacta: SI)"
  exit 0
fi

echo "Restaurando $FILENAME -> $TARGET_DB ..."
if [ "$TARGET_DB" != "$PG_DB" ]; then
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "CREATE DATABASE $TARGET_DB;" >/dev/null || { echo "ERROR: no se pudo crear la base temporal"; exit 1; }
fi

CONTAINER_TMP="/tmp/${FILENAME}"
docker cp "$BACKUP_FILE" "${PG_CONTAINER}:${CONTAINER_TMP}" 2>/dev/null

RESTORE_RC=0
if [ "$FORMAT" = "plain" ]; then
  docker exec -e "PGPASSWORD=$PG_PASS" "$PG_CONTAINER" sh -c "gunzip -c '$CONTAINER_TMP' | psql -h localhost -U '$PG_USER' -d '$TARGET_DB' --single-transaction --no-psqlrc" >/dev/null 2>&1 || RESTORE_RC=1
else
  docker exec -e "PGPASSWORD=$PG_PASS" "$PG_CONTAINER" sh -c "pg_restore -h localhost -U '$PG_USER' -d '$TARGET_DB' --no-owner --no-acl '$CONTAINER_TMP'" >/dev/null 2>&1 || RESTORE_RC=1
fi
docker exec "$PG_CONTAINER" rm -f "$CONTAINER_TMP" 2>/dev/null || true

if [ "$RESTORE_RC" -ne 0 ]; then
  echo "ERROR: pg_restore/psql fallo sobre $TARGET_DB"
  if [ "$TARGET_DB" != "$PG_DB" ]; then
    docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "DROP DATABASE $TARGET_DB;" >/dev/null 2>&1 || true
    echo "Base temporal $TARGET_DB eliminada."
  fi
  exit 1
fi

echo "Verificando integridad estructural de $TARGET_DB ..."
if [ "$TARGET_DB" != "$PG_DB" ]; then
  "$SCRIPT_DIR/verify-restore-db.sh" -d "$TARGET_DB"
  VERIFY_RC=$?
else
  "$SCRIPT_DIR/verify-restore-db.sh" -d "$TARGET_DB" --self-reference
  VERIFY_RC=$?
fi

if [ "$VERIFY_RC" -ne 0 ]; then
  echo "ERROR: la verificacion de $TARGET_DB fallo"
  if [ "$TARGET_DB" != "$PG_DB" ]; then
    docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "DROP DATABASE $TARGET_DB;" >/dev/null 2>&1 || true
    echo "Base temporal $TARGET_DB eliminada."
  fi
  exit 1
fi

if [ "$RESTORE_TO_MAIN" -eq 1 ]; then
  echo "Aplicando reemplazo de la base principal ..."
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$PG_DB';" >/dev/null 2>&1 || true
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "DROP DATABASE $PG_DB;" >/dev/null || { echo "ERROR: no se pudo reemplazar la base principal"; exit 1; }
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "ALTER DATABASE $TARGET_DB RENAME TO $PG_DB;" >/dev/null || { echo "ERROR: no se pudo renombrar $TARGET_DB a $PG_DB"; exit 1; }
  echo "Base principal $PG_DB restaurada desde $FILENAME."
else
  echo "Restauracion completada en la base temporal: $TARGET_DB"
  echo "  - Base principal $PG_DB intacta."
  echo "  - Use --drop-after-verify para eliminarla o pruebe el backend con restore-backend-check.sh -d $TARGET_DB"
  if [ "$DROP_AFTER_VERIFY" -eq 1 ]; then
    docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -c "DROP DATABASE $TARGET_DB;" >/dev/null 2>&1 || true
    echo "Base temporal $TARGET_DB eliminada (--drop-after-verify)."
  fi
fi
exit 0
