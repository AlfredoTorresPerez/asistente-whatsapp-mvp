#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

info() {
  echo "OK: $1"
}

[ -x backend-java/mvnw ] || fail "backend-java/mvnw no tiene permiso de ejecucion"
info "backend-java/mvnw tiene permiso de ejecucion"

[ -x scripts/start_mvp_public_link.sh ] || fail "scripts/start_mvp_public_link.sh no tiene permiso de ejecucion"
info "script de enlace publico tiene permiso de ejecucion"

grep -q '"whatsapp-web.js": "1.34.7"' whatsapp-web-service/package.json \
  || fail "whatsapp-web.js no esta fijado en 1.34.7 dentro de package.json"
info "whatsapp-web.js esta fijado en 1.34.7"

if find frontend-react/src -name '*.bak' | grep -q .; then
  fail "existen archivos .bak dentro de frontend-react/src"
fi
info "no hay archivos .bak dentro de frontend-react/src"

grep -q 'no usar localhost' .env.example \
  || fail ".env.example no contiene advertencia sobre localhost en enlaces reales"
info ".env.example contiene advertencia sobre localhost"

grep -q 'APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL' docker-compose.local.yml \
  || fail "docker-compose.local.yml no expone APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL"
info "docker-compose.local.yml permite configurar URL publica de confirmacion"

grep -q 'public-tunnel' docker-compose.local.yml \
  || fail "docker-compose.local.yml no contiene servicio public-tunnel"
info "docker-compose.local.yml contiene servicio public-tunnel"

grep -q 'TZ: ${TZ:-America/Santiago}' docker-compose.local.yml \
  || fail "docker-compose.local.yml no configura TZ America/Santiago"
info "docker-compose.local.yml configura zona horaria America/Santiago"

grep -q 'APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES: ${APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES:-720}' docker-compose.local.yml \
  || fail "docker-compose.local.yml no deja expiracion de confirmacion en 720 minutos por defecto"
info "docker-compose.local.yml deja enlaces de confirmacion en 12 horas por defecto"

grep -q 'location /api/v1/' frontend-react/nginx.conf \
  || fail "frontend-react/nginx.conf no proxifica /api/v1 hacia backend"
info "frontend-react/nginx.conf proxifica /api/v1 hacia backend"

grep -q 'ARG VITE_API_BASE_URL=/api/v1' frontend-react/Dockerfile \
  || fail "frontend-react/Dockerfile no usa API relativa /api/v1 por defecto"
info "frontend-react/Dockerfile usa API relativa /api/v1 por defecto"

if command -v node >/dev/null 2>&1; then
  node --check whatsapp-web-service/src/server.js >/dev/null
  info "sintaxis basica de whatsapp-web-service/src/server.js valida"
else
  echo "WARN: Node.js no disponible; se omite node --check"
fi

echo "Validacion local rapida finalizada."
