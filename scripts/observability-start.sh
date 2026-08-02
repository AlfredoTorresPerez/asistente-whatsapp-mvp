#!/usr/bin/env bash
# =============================================================================
# OBSERVABILIDAD START - Levanta el stack de observabilidad local
# Uso: ./scripts/observability-start.sh [-b]
#   -b  Reconstruye imagenes antes de levantar
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/docker-compose.local.yml"
ENVFILE="$ROOT/.env.local"
BUILD=""

if [ "${1:-}" = "-b" ]; then
    BUILD="--build"
fi

if [ ! -f "$COMPOSE" ]; then
    echo "ERROR: No se encuentra $COMPOSE" >&2
    exit 1
fi

if [ ! -f "$ENVFILE" ] || ! grep -q '^GRAFANA_ADMIN_PASSWORD=..*' "$ENVFILE"; then
    echo "ERROR: GRAFANA_ADMIN_PASSWORD no esta definido en .env.local" >&2
    exit 1
fi

CMD=(docker compose --env-file "$ENVFILE" -f "$COMPOSE" --profile observability up $BUILD -d prometheus loki tempo alloy grafana)
echo "=== Levantando stack de observabilidad ==="
echo "  Comando: ${CMD[*]}"
"${CMD[@]}"

echo ""
echo "  Grafana:    http://localhost:3000"
echo "  Prometheus: http://localhost:9090"
echo "  Loki:       http://localhost:3100"
echo "  Tempo:      http://localhost:3200"
echo "  Verificacion: ./scripts/observability-verify.sh"
