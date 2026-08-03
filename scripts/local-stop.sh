#!/usr/bin/env bash
# =============================================================================
# local-stop.sh - Detiene los servicios locales de Docker Compose
#                 (comando oficial de detencion, Linux/macOS).
#
# Detiene contenedores sin eliminar volumenes (datos persistentes).
# Incluye los perfiles opcionales para que ningun contenedor del compose
# local quede corriendo. Usa --env-file .env.local si existe.
#
# Uso:
#   bash scripts/local-stop.sh
#   bash scripts/local-stop.sh --volumes   # ademas elimina la base de datos
# =============================================================================
set -uo pipefail

VOLUMES=0
for arg in "$@"; do
  case "$arg" in
    --volumes) VOLUMES=1 ;;
    -h|--help) head -15 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.local.yml"
ENV_FILE="$ROOT_DIR/.env.local"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "No se encuentra $COMPOSE_FILE" >&2
  exit 1
fi

CMD=(docker compose)
[[ -f "$ENV_FILE" ]] && CMD+=(--env-file "$ENV_FILE")
CMD+=(-f "$COMPOSE_FILE")

# Todos los perfiles opcionales: down debe alcanzar tambien sus contenedores
for profile in observability monitoring backup public-link https; do
  CMD+=(--profile "$profile")
done

CMD+=(down)
if [[ "$VOLUMES" == "1" ]]; then
  CMD+=(-v)
  echo "=== Deteniendo servicios y eliminando volumenes ==="
  echo "ADVERTENCIA: Se eliminara la base de datos y todos sus datos."
else
  echo "=== Deteniendo servicios (volumenes preservados) ==="
fi

echo "  Comando: ${CMD[*]}"
if docker "${CMD[@]}"; then
  echo "  [OK] Servicios detenidos"
else
  echo "Error al detener servicios (exit code: $?)" >&2
  exit 1
fi
