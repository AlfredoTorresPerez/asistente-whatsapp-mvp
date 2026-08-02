#!/usr/bin/env bash
# =============================================================================
# OBSERVABILIDAD STOP - Detiene el stack de observabilidad local
# Uso: ./scripts/observability-stop.sh [-v]
#   -v  Elimina tambien los volumenes de datos (Prometheus, Loki, Tempo, Grafana)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/docker-compose.local.yml"
ENVFILE="$ROOT/.env.local"

if [ ! -f "$COMPOSE" ]; then
    echo "ERROR: No se encuentra $COMPOSE" >&2
    exit 1
fi

ARGS=(docker compose --env-file "$ENVFILE" -f "$COMPOSE" --profile observability)

if [ "${1:-}" = "-v" ]; then
    echo "=== Deteniendo observabilidad y eliminando volumenes ==="
    "${ARGS[@]}" down -v
else
    echo "=== Deteniendo observabilidad (volumenes preservados) ==="
    "${ARGS[@]}" stop prometheus loki tempo alloy grafana
fi

echo "  [OK] Observabilidad detenida"
