#!/usr/bin/env bash
# =============================================================================
# local-verify.sh - Verifica el estado del entorno local (health + smoke test,
#                   Linux/macOS).
#
# Comprueba:
#   - Contenedores core saludables (postgres, backend, frontend, mailpit)
#   - Contenedores de perfiles opcionales si estan corriendo
#   - Backend /actuator/health = UP
#   - Frontend HTTP 200
#   - Login y API basica
#
# Uso:
#   bash scripts/local-verify.sh
#   bash scripts/local-verify.sh --timeout 60
# =============================================================================
set -uo pipefail

TIMEOUT=30
for arg in "$@"; do
  case "$arg" in
    --timeout=*) TIMEOUT="${arg#--timeout=}" ;;
    -h|--help) head -20 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.local.yml"
FAILED=0

ok()   { echo "  [OK]   $1"; }
fail() { echo "  [FAIL] $1"; FAILED=1; }

echo "=============================================="
echo "  VERIFICACION DEL ENTORNO LOCAL"
echo "=============================================="

if [[ ! -f "$COMPOSE_FILE" ]]; then
  fail "No se encuentra $COMPOSE_FILE"
  exit 1
fi

container_status() {
  docker inspect --format='{{.State.Status}}' "$1" 2>/dev/null || echo "missing"
}
container_health() {
  docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$1" 2>/dev/null || echo "none"
}

# ── 1. Verificar contenedores Docker ───────────────────────
echo ""
echo "1/4 - Verificando contenedores Docker..."

CORE=(asistente-postgres asistente-backend asistente-frontend asistente-mailpit)
OPTIONAL=(asistente-prometheus asistente-loki asistente-tempo asistente-alloy asistente-grafana \
          asistente-backup-sidecar asistente-public-tunnel asistente-caddy)

for name in "${CORE[@]}"; do
  state=$(container_status "$name")
  if [[ "$state" != "running" ]]; then
    fail "Contenedor core $name no esta corriendo"
    continue
  fi
  health=$(container_health "$name")
  if [[ "$health" == "healthy" || "$health" == "none" ]]; then
    ok "$name = running (health: $health)"
  elif [[ "$health" == "starting" ]]; then
    fail "$name = starting (aun no listo)"
  else
    fail "$name = $health"
  fi
done

RUNNING_OPTIONAL=0
for name in "${OPTIONAL[@]}"; do
  state=$(container_status "$name")
  [[ "$state" != "running" ]] && continue
  RUNNING_OPTIONAL=1
  health=$(container_health "$name")
  if [[ "$health" == "healthy" || "$health" == "none" ]]; then
    ok "$name = running (health: $health)"
  else
    fail "$name = $health"
  fi
done
if [[ "$RUNNING_OPTIONAL" == "0" ]]; then
  echo "  [--] Sin contenedores opcionales activos (perfiles no levantados)"
fi

# ── 2. Backend health endpoint ──────────────────────────────
echo ""
echo "2/4 - Verificando backend..."
HEALTH=$(curl -s --max-time "$TIMEOUT" http://localhost:8080/actuator/health 2>/dev/null)
if [[ "$HEALTH" == *'"status":"UP"'* ]]; then
  ok "Backend /actuator/health = UP"
else
  fail "Backend health no es UP: ${HEALTH:0:120}"
fi

# ── 3. Frontend HTTP 200 ────────────────────────────────────
echo ""
echo "3/4 - Verificando frontend..."
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time "$TIMEOUT" http://localhost:5173 2>/dev/null)
if [[ "$HTTP_CODE" == "200" ]]; then
  ok "Frontend HTTP 200"
else
  fail "Frontend HTTP $HTTP_CODE"
fi

# ── 4. Login + API smoke ───────────────────────────────────
echo ""
echo "4/4 - Smoke test (login + API)..."
LOGIN=$(curl -s --max-time "$TIMEOUT" -X POST \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@demo.cl","password":"Cambiar123!"}' \
  http://localhost:8080/api/v1/auth/login 2>/dev/null)
TOKEN=$(echo "$LOGIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
if [[ -n "$TOKEN" ]]; then
  ok "Login exitoso (token obtenido)"
else
  fail "Login no devolvio accessToken"
fi

BIZ=$(curl -s --max-time "$TIMEOUT" -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/company 2>/dev/null)
if [[ "$BIZ" == *'"id"'* ]]; then
  ok "GET /api/v1/company OK"
else
  fail "/api/v1/company no devolvio id"
fi

# ── Resumen ─────────────────────────────────────────────────
echo ""
if [[ "$FAILED" == "1" ]]; then
  echo "VERIFICACION COMPLETADA CON FALLOS"
  exit 1
fi
echo "VERIFICACION COMPLETADA: TODO OK"
exit 0
