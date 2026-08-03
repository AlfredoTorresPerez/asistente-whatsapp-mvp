#!/usr/bin/env bash
# =============================================================================
# local-start.sh - Levanta los servicios locales con Docker Compose
#                  (comando oficial de arranque, Linux/macOS).
#
# Perfiles opcionales (docker-compose.local.yml):
#   observability  - Prometheus, Loki, Tempo, Alloy, Grafana
#   backup         - backup-sidecar (cron diario de pg_dump)
#   public-link    - tunel publico HTTPS (cloudflared)
#   https          - Caddy (HTTPS local autosigned)
#
# Uso:
#   bash scripts/local-start.sh
#   bash scripts/local-start.sh --build
#   bash scripts/local-start.sh --profile observability,backup
#   bash scripts/local-start.sh --profile all --verify
#
# Nota: en Linux/macOS no existe Windows Credential Manager; los secretos se
# leen directamente de .env.local (--env-file). Nunca se imprimen valores.
# =============================================================================
set -uo pipefail

PROFILE=""
BUILD=0
DETACH=1
VERIFY=0

for arg in "$@"; do
  case "$arg" in
    --profile=*) PROFILE="${arg#--profile=}" ;;
    --build) BUILD=1 ;;
    --detach) DETACH=1 ;;
    --foreground) DETACH=0 ;;
    --verify) VERIFY=1 ;;
    -h|--help) head -25 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.local.yml"
ENV_FILE="$ROOT_DIR/.env.local"
KNOWN_PROFILES="observability monitoring backup public-link https"

# ------------------------------------------------------------------------
# Pre-flight: docker disponible
# ------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  echo "docker no esta instalado o no esta en el PATH." >&2
  exit 1
fi

# ------------------------------------------------------------------------
# Pre-flight: archivo compose
# ------------------------------------------------------------------------
if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "No se encuentra $COMPOSE_FILE" >&2
  exit 1
fi

# ------------------------------------------------------------------------
# Pre-flight: validar perfiles solicitados
# ------------------------------------------------------------------------
PROFILE_LIST=()
if [[ -n "$PROFILE" ]]; then
  IFS=',' read -ra RAW <<< "$PROFILE"
  for p in "${RAW[@]}"; do
    p="$(echo "$p" | xargs)"
    [[ -z "$p" ]] && continue
    if [[ "$p" == "all" ]]; then
      PROFILE_LIST=(observability backup public-link https)
      break
    fi
    if ! echo "$KNOWN_PROFILES" | grep -qw "$p"; then
      echo "Perfil invalido: $p. Perfiles validos: $KNOWN_PROFILES, all" >&2
      exit 1
    fi
    PROFILE_LIST+=("$p")
  done
  echo "=== Perfiles: ${PROFILE_LIST[*]} ==="
fi

# ------------------------------------------------------------------------
# Pre-flight: docker compose config valido
# Nota: en Compose v5 los --profile deben ir ANTES del subcomando config
# ------------------------------------------------------------------------
CONFIG_ARGS=(compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")
for p in "${PROFILE_LIST[@]}"; do CONFIG_ARGS+=(--profile "$p"); done
CONFIG_ARGS+=(config --quiet)
if ! docker "${CONFIG_ARGS[@]}" >/dev/null 2>&1; then
  echo "docker compose config fallo. Revisa $COMPOSE_FILE y .env.local" >&2
  exit 1
fi
echo "  [OK] docker compose config valido (perfiles: ${PROFILE_LIST[*]:-ninguno})"

# ------------------------------------------------------------------------
# Docker Compose da precedencia a variables ya presentes en el shell por
# sobre --env-file. Sincroniza valores no secretos (pueden cambiar por
# tunel/perfil) para evitar que un env antiguo reemplace .env.local.
# ------------------------------------------------------------------------
if [[ -f "$ENV_FILE" ]]; then
  for key in \
    APP_WHATSAPP_CLOUD_API_WEBHOOK_PUBLIC_URL APP_FRONTEND_PUBLIC_BASE_URL \
    APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL APP_BOOKING_RESCHEDULE_PUBLIC_BASE_URL \
    APP_BOOKING_CANCELLATION_PUBLIC_BASE_URL APP_BOOKING_PAYMENT_CHECKOUT_PUBLIC_BASE_URL \
    APP_AI_AGENTS_ENABLED APP_AI_AGENTS_AUTO_REPLY_ENABLED APP_AI_AGENTS_AUDIT_ENABLED \
    APP_AI_AGENTS_SAFE_MODE_ENABLED APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED APP_OPENAI_ENABLED; do
    if grep -qE "^$key=" "$ENV_FILE"; then
      export "$key"="$(grep -E "^$key=" "$ENV_FILE" | head -1 | cut -d= -f2-)"
    fi
  done
fi

# ------------------------------------------------------------------------
# Advertencia: GRAFANA_ADMIN_PASSWORD requerido con perfil observability
# ------------------------------------------------------------------------
if [[ "${PROFILE_LIST[*]}" == *observability* ]] && [[ -f "$ENV_FILE" ]]; then
  if ! grep -qE '^GRAFANA_ADMIN_PASSWORD=.+' "$ENV_FILE"; then
    echo "AVISO: GRAFANA_ADMIN_PASSWORD no esta definido en .env.local. Grafana quedara con password vacio."
  fi
fi

# ------------------------------------------------------------------------
# Levantar
# ------------------------------------------------------------------------
CMD=(docker compose)
if [[ -f "$ENV_FILE" ]]; then
  CMD+=(--env-file "$ENV_FILE")
else
  echo "AVISO: .env.local no encontrado, usando defaults"
fi
CMD+=(-f "$COMPOSE_FILE")
for p in "${PROFILE_LIST[@]}"; do CMD+=(--profile "$p"); done
CMD+=(up)
[[ "$BUILD" == "1" ]] && CMD+=(--build)
[[ "$DETACH" == "1" ]] && CMD+=(-d)

echo "=== Levantando servicios ==="
echo "  Comando: ${CMD[*]}"

if ! docker "${CMD[@]}"; then
  echo "Error al levantar servicios (exit code: $?)" >&2
  exit 1
fi

echo ""
docker compose -f "$COMPOSE_FILE" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "  Frontend: http://localhost:5173"
echo "  Backend:  http://localhost:8080"
echo "  API Docs: http://localhost:8080/swagger-ui/index.html"

if [[ "$VERIFY" == "1" ]]; then
  echo ""
  bash "$(dirname "$0")/local-verify.sh" || exit 1
fi

exit 0
