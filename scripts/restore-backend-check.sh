#!/usr/bin/env bash
# =============================================================================
# RESTORE BACKEND CHECK - Backend contra BD restaurada (Linux/macOS)
# Uso: ./scripts/restore-backend-check.sh -d <db_restaurada>
#      ./scripts/restore-backend-check.sh -d <db> --expect-failure [--failure-pattern <regex>]
# Levanta un contenedor backend-restore-verify (puerto 8081) apuntando a la BD
# dada, espera /actuator/health=UP, prueba login, GET /api/v1/company y un
# inbound de prueba (escritura). Limpia el contenedor al terminar (finally).
# Exit 0 = OK. Con --expect-failure: exit 0 si el backend NO arranca y los logs
# coinciden con el patron (default: flyway).
# =============================================================================
set -euo pipefail

DB_NAME=""
EXPECT_FAILURE=0
FAILURE_PATTERN="flyway"
PORT=8081
TIMEOUT_SECONDS=180

while [ $# -gt 0 ]; do
  case "$1" in
    -d|--db) DB_NAME="$2"; shift 2 ;;
    --expect-failure) EXPECT_FAILURE=1; shift ;;
    --failure-pattern) FAILURE_PATTERN="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --timeout) TIMEOUT_SECONDS="$2"; shift 2 ;;
    *) echo "ERROR: argumento desconocido $1"; exit 1 ;;
  esac
done
[ -n "$DB_NAME" ] || { echo "ERROR: falta -d <db_restaurada>"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.local.yml"
ENV_FILE="$SCRIPT_DIR/.env.local"
OVERRIDE="$(mktemp /tmp/backend-restore-verify.XXXXXX.yml)"

cleanup() {
  echo "Limpiando contenedor temporal ..."
  docker rm -f backend-restore-verify >/dev/null 2>&1 || true
  rm -f "$OVERRIDE"
}
trap cleanup EXIT

cat > "$OVERRIDE" <<EOF
services:
  backend-restore-verify:
    profiles: ["core"]
    image: asistente-backend-java:latest
    container_name: backend-restore-verify
    environment:
      APP_DB_URL: "jdbc:postgresql://postgres:5432/$DB_NAME"
      SPRING_PROFILES_ACTIVE: "local,local-safe"
      APP_WHATSAPP_CHANNEL_PROVIDER: "SIMULATED"
      APP_WHATSAPP_CLOUD_API_ENABLED: "false"
      APP_EMAIL_MIRROR_ENABLED: "false"
      APP_OPENAI_ENABLED: "false"
      APP_BOOKING_PAYMENT_PROVIDER: "SIMULATED"
      SERVER_PORT: "8081"
    ports:
      - "${PORT}:8081"
    networks:
      - asistente-local
EOF

if [ "$EXPECT_FAILURE" -eq 1 ]; then
  echo "Test de fallo esperado contra $DB_NAME (patron: $FAILURE_PATTERN)"
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -f "$OVERRIDE" up -d --no-deps backend-restore-verify >/dev/null 2>&1 || true
  sleep 20
  LOGS="$(docker logs backend-restore-verify 2>&1 || true)"
  echo "Buscando patron '$FAILURE_PATTERN' en logs del backend ..."
  if grep -qiE "$FAILURE_PATTERN" <<< "$LOGS"; then
    echo "PATRON ENCONTRADO: el backend fallo al arrancar contra $DB_NAME (comportamiento esperado)."
    echo "[PASS] migracion_incompatible_esperada"
    exit 0
  fi
  echo "PATRON NO ENCONTRADO: el backend deberia haber fallado pero no lo hizo."
  echo "[FAIL] migracion_incompatible_esperada"
  exit 1
fi

echo "Levantando backend-restore-verify contra $DB_NAME (puerto $PORT) ..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -f "$OVERRIDE" up -d --no-deps backend-restore-verify >/dev/null 2>&1

echo "Esperando /actuator/health ..."
DEADLINE=$((SECONDS + TIMEOUT_SECONDS))
HEALTH_OK=0
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  if curl -sf "http://localhost:${PORT}/actuator/health" | grep -q '"UP"'; then
    HEALTH_OK=1
    break
  fi
  sleep 5
done
if [ "$HEALTH_OK" -ne 1 ]; then
  echo "ERROR: el backend no alcanzo estado UP contra $DB_NAME en ${TIMEOUT_SECONDS}s"
  docker logs backend-restore-verify 2>&1 | tail -n 30 || true
  exit 1
fi
echo "Backend UP contra $DB_NAME."

echo "Prueba 1: login demo ..."
LOGIN="$(curl -sf -X POST "http://localhost:${PORT}/api/v1/auth/login" -H "Content-Type: application/json" -d '{"username":"admin@demo.cl","password":"Cambiar123!"}' || true)"
if [ -z "$LOGIN" ]; then
  echo "ERROR: login fallo"
  exit 1
fi
echo "Login OK (token obtenido)."

echo "Prueba 2: GET /api/v1/company ..."
COMPANY="$(curl -sf "http://localhost:${PORT}/api/v1/company" -H "Authorization: Bearer $(echo "$LOGIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4 || true)" || true)"
if [ -z "$COMPANY" ]; then
  echo "ERROR: GET /api/v1/company fallo"
  exit 1
fi
echo "Company OK."

echo "Prueba 3: inbound de prueba (escritura) ..."
INBOUND="$(curl -sf -X POST "http://localhost:${PORT}/api/v1/test/whatsapp-inbound" -H "Content-Type: application/json" -d '{"sessionKey":"restore-check","from":"+56999999999","body":"Hola, quiero agendar una hora"}' || true)"
if [ -z "$INBOUND" ]; then
  echo "ERROR: inbound de prueba fallo"
  exit 1
fi
echo "Inbound OK."

echo "=== RESTORE BACKEND CHECK: TODAS LAS PRUEBAS APROBADAS (exit 0) ==="
exit 0
