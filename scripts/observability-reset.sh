#!/usr/bin/env bash
# =============================================================================
# OBSERVABILIDAD RESET - Detiene y borra datos del stack de observabilidad
# Uso: ./scripts/observability-reset.sh [-f]
#   -f  Omite la confirmacion
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/docker-compose.local.yml"
ENVFILE="$ROOT/.env.local"

if [ ! -f "$COMPOSE" ]; then
    echo "ERROR: No se encuentra $COMPOSE" >&2
    exit 1
fi

if [ "${1:-}" != "-f" ]; then
    read -r -p "Se eliminaran los datos de Prometheus, Loki, Tempo y Grafana. Confirmas? (s/N): " response
    if [ "$response" != "s" ]; then
        echo "Reset cancelado."
        exit 0
    fi
fi

echo "=== Reset de observabilidad ==="
docker compose --env-file "$ENVFILE" -f "$COMPOSE" --profile observability down -v
echo "  [OK] Observabilidad reseteada. Levantala de nuevo con:"
echo "       ./scripts/observability-start.sh -b"
