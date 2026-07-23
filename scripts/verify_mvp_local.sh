#!/usr/bin/env bash
# verify_mvp_local.sh - Bash version for Git Bash / WSL / Linux
# Exit codes: 0=OK, 1=Docker, 2=Health, 3=API, 4=WhatsApp, 5=IA

set -euo pipefail

QUICK=false
PROFILE=""
TIMEOUT_MINUTES=3
NO_CLEANUP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --quick) QUICK=true; shift ;;
        --profile) PROFILE="$2"; shift 2 ;;
        --timeout) TIMEOUT_MINUTES="$2"; shift 2 ;;
        --no-cleanup) NO_CLEANUP=true; shift ;;
        *) echo "Uso: $0 [--quick] [--profile whatsapp] [--timeout 3] [--no-cleanup]"; exit 1 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$ROOT_DIR/docker-compose.local.yml"
ENV_FILE="$ROOT_DIR/.env.local"

START_TIME=$(date +%s)
TIMEOUT_EPOCH=$((START_TIME + TIMEOUT_MINUTES * 60))
EXIT_CODE=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

log() { echo -e "[$(($(date +%s) - START_TIME))s] $*"; }
pass() { log "${GREEN}✅ PASS:${NC} $*"; }
fail() { log "${RED}❌ FAIL:${NC} $*"; EXIT_CODE=1; }
warn() { log "${YELLOW}⚠️  WARN:${NC} $*"; }
info() { log "${GRAY}ℹ️  $*${NC}"; }

check_timeout() {
    if [[ $(date +%s) -gt $TIMEOUT_EPOCH ]]; then
        fail "TIMEOUT global (${TIMEOUT_MINUTES} min) excedido"
        exit 1
    fi
}

retry() {
    local desc="$1"; shift
    local max_retries=30
    local delay=2
    for ((i=1; i<=max_retries; i++)); do
        check_timeout
        if "$@"; then return 0; fi
        sleep $delay
    done
    fail "Retry agotado: $desc"
    return 1
}

# Header
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  MVP ASISTENTE WHATSAPP - VALIDACIÓN LOCAL (${QUICK:-QUICK:-FULL})  ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"

# 0. Verificar archivos
info "Verificando archivos..."
[[ -f "$COMPOSE_FILE" ]] || { fail "No existe $COMPOSE_FILE"; exit 1; }
[[ -f "$ENV_FILE" ]] || warn "No existe .env.local (usando defaults)"

# 1. Docker Compose Up
log "1/8 - Levantando servicios..."
COMPOSE_CMD="docker compose -f \"$COMPOSE_FILE\""
[[ -n "$PROFILE" ]] && COMPOSE_CMD+=" --profile $PROFILE"
COMPOSE_CMD+=" up -d --build"
eval "$COMPOSE_CMD"
pass "docker compose up -d --build OK"

# 2. Wait Healthchecks
log "2/8 - Esperando healthchecks (máx ${TIMEOUT_MINUTES} min)..."
declare -A CONTAINER_MAP=(
    ["postgres"]="asistente-postgres"
    ["backend-java"]="asistente-backend"
    ["frontend-react"]="asistente-frontend"
    ["whatsapp-web-service"]="asistente-whatsapp-web"
)
SERVICES=("postgres" "backend-java" "frontend-react")
[[ "$PROFILE" == "whatsapp" ]] && SERVICES+=("whatsapp-web-service")

for svc in "${SERVICES[@]}"; do
    container_name="${CONTAINER_MAP[$svc]}"
    info "  Esperando $svc (container=$container_name)..."
    healthy=false
    for ((i=1; i<=90; i++)); do
        check_timeout
        state=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")
        [[ "$state" == "healthy" ]] && { healthy=true; break; }
        [[ "$state" == "unhealthy" ]] && { fail "$svc unhealthy"; exit 2; }
        sleep 2
    done
    [[ "$healthy" == true ]] || { fail "$svc no alcanzó healthy en 3 min"; exit 2; }
    pass "$svc healthy"
done

# 3. Health Endpoints
log "3/8 - Verificando endpoints de salud..."

# Backend
health=$(curl -sf "http://localhost:8080/actuator/health" || echo '{"status":"DOWN"}')
status=$(echo "$health" | jq -r '.status // "DOWN"')
[[ "$status" == "UP" ]] || { fail "Backend health: $status"; exit 3; }
pass "Backend /actuator/health = UP"

# Frontend
curl -sf "http://localhost:5173" >/dev/null || { fail "Frontend HTTP falló"; exit 3; }
pass "Frontend HTTP 200"

# WhatsApp Web
if [[ "$PROFILE" == "whatsapp" ]]; then
    ww_health=$(curl -sf "http://localhost:3001/health" || echo '{"runtimeReady":false}')
    rr=$(echo "$ww_health" | jq -r '.runtimeReady // false')
    [[ "$rr" == "true" ]] && pass "WhatsApp Web runtimeReady=true" || warn "WhatsApp Web runtimeReady=false (requiere QR)"
fi

# 4. Auth + API Smoke
log "4/8 - Autenticación y API smoke test..."
TOKEN=$(curl -sf -X POST "http://localhost:8080/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@demo.cl","password":"Cambiar123!"}' | jq -r '.accessToken // empty')
[[ -n "$TOKEN" ]] || { fail "Login no devolvió accessToken"; exit 3; }
pass "Login admin@demo.cl OK"

AUTH_HEADER="Authorization: Bearer $TOKEN"
BIZ=$(curl -sf -H "$AUTH_HEADER" "http://localhost:8080/api/v1/company" | jq -r '.id // empty')
[[ -n "$BIZ" ]] || { fail "/api/v1/company sin id"; exit 3; }
pass "GET /api/v1/company OK (id=$BIZ)"

if [[ "$QUICK" == true ]]; then
    echo -e "\n${CYAN}═══════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ QUICK VALIDATION PASSED (health + auth)${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    [[ "$NO_CLEANUP" == false ]] && docker compose -f "$COMPOSE_FILE" down
    exit 0
fi

# 5. WhatsApp Web Status
if [[ "$PROFILE" == "whatsapp" ]]; then
    log "5/8 - Verificando estado WhatsApp Web..."
    WW_STATUS=$(curl -sf -H "$AUTH_HEADER" "http://localhost:8080/api/v1/whatsapp-web/status")
    SESS=$(echo "$WW_STATUS" | jq -r '.sessionStatus // "UNKNOWN"')
    if [[ "$SESS" == "CONNECTED" || "$SESS" == "QR_PENDING" ]]; then
        pass "WhatsApp Web sessionStatus = $SESS"
    else
        fail "WhatsApp Web sessionStatus = $SESS (esperado CONNECTED o QR_PENDING)"
        exit 4
    fi
fi

# 6. Webhook Test
if [[ "$PROFILE" == "whatsapp" ]]; then
    log "6/8 - Test webhook WhatsApp..."
    WEBHOOK_SCRIPT="$SCRIPT_DIR/test-whatsapp-webhook-local.sh"
    if [[ -f "$WEBHOOK_SCRIPT" ]]; then
        bash "$WEBHOOK_SCRIPT" "$TOKEN" || { fail "Webhook test falló"; exit 4; }
    else
        warn "test-whatsapp-webhook-local.sh no encontrado, saltando"
    fi
else
    log "6/8 - Test webhook WhatsApp..."
    info "Saltando (usa --profile whatsapp para incluirlo)"
fi

# 7. AI Auto-reply Test
if [[ "$PROFILE" == "whatsapp" ]]; then
    log "7/8 - Test IA Auto-reply..."
    AI_SCRIPT="$SCRIPT_DIR/test-ai-auto-reply-local.sh"
    if [[ -f "$AI_SCRIPT" ]]; then
        bash "$AI_SCRIPT" "$TOKEN" || { fail "IA Auto-reply test falló"; exit 5; }
    else
        warn "test-ai-auto-reply-local.sh no encontrado, saltando"
    fi
else
    log "7/8 - Test IA Auto-reply..."
    info "Saltando (usa --profile whatsapp para incluirlo)"
fi

# Success
echo -e "\n${CYAN}═══════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ FULL VALIDATION PASSED${NC}"
echo -e "${CYAN}═══════════════════════════════════════════${NC}"

[[ "$NO_CLEANUP" == false ]] && docker compose -f "$COMPOSE_FILE" down
exit 0