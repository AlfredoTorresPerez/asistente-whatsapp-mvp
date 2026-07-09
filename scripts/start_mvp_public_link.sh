#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="docker-compose.local.yml"
ENV_FILE=".env"
PROFILE="public-link"
MAX_ATTEMPTS="90"

log() {
  printf '%s\n' "$1"
}

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "No se encontro el comando requerido: $1"
}

update_env_value() {
  local key="$1"
  local value="$2"
  local file="$3"
  local tmp_file
  touch "$file"
  tmp_file="$(mktemp)"
  if grep -q "^${key}=" "$file"; then
    awk -v key="$key" -v value="$value" 'BEGIN { done = 0 } $0 ~ "^" key "=" { print key "=" value; done = 1; next } { print } END { if (done == 0) print key "=" value }' "$file" > "$tmp_file"
  else
    cat "$file" > "$tmp_file"
    printf '%s=%s\n' "$key" "$value" >> "$tmp_file"
  fi
  mv "$tmp_file" "$file"
}

extract_tunnel_url() {
  docker compose -f "$COMPOSE_FILE" logs --no-color public-tunnel 2>/dev/null \
    | grep -Eo 'https://[-a-zA-Z0-9.]+\.trycloudflare\.com' \
    | grep -v '^https://api\.trycloudflare\.com$' \
    | tail -n 1 || true
}

require_command docker

log "Levantando MVP local..."
docker compose -f "$COMPOSE_FILE" up -d --build

log "Levantando tunel publico HTTPS para el frontend..."
docker compose -f "$COMPOSE_FILE" --profile "$PROFILE" up -d public-tunnel

log "Esperando URL publica del tunel..."
public_url=""
for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  public_url="$(extract_tunnel_url)"
  if [ -n "$public_url" ]; then
    break
  fi
  sleep 2
done

[ -n "$public_url" ] || fail "No se pudo obtener URL publica. Revisa: docker compose -f $COMPOSE_FILE logs public-tunnel"

confirmation_url="${public_url%/}/reservas/confirmar"
reschedule_url="${public_url%/}/reservas/reprogramar"
cancellation_url="${public_url%/}/reservas/cancelar"
payment_url="${public_url%/}/reservas/pagar"

cat > "$ENV_FILE" <<EOF
APP_FRONTEND_PUBLIC_BASE_URL=$public_url
APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL=$confirmation_url
APP_BOOKING_RESCHEDULE_PUBLIC_BASE_URL=$reschedule_url
APP_BOOKING_CANCELLATION_PUBLIC_BASE_URL=$cancellation_url
APP_BOOKING_PAYMENT_CHECKOUT_PUBLIC_BASE_URL=$payment_url
VITE_API_BASE_URL=/api/v1
APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES=60
TZ=America/Santiago
JAVA_TOOL_OPTIONS=-Duser.timezone=America/Santiago
SPRING_JACKSON_TIME_ZONE=America/Santiago
APP_TIME_ZONE=America/Santiago
APP_METHOD_TRACING_MAX_PAYLOAD_LENGTH=600
EOF

log "URL publica frontend: $public_url"
log "URL publica de confirmacion: $confirmation_url"
log "URL publica de reprogramacion: $reschedule_url"
log "URL publica de pago: $payment_url"
log "Recreando backend con URL navegable..."
docker compose -f "$COMPOSE_FILE" up -d --force-recreate backend-java whatsapp-web-service frontend-react

log "Listo. Los enlaces enviados por WhatsApp usaran: $confirmation_url/{token}"
log "Listo. Los enlaces de reserva asistida usaran: $public_url/reservar?token=..."
log "Para ver el tunel: docker compose -f $COMPOSE_FILE logs -f public-tunnel"
