#!/usr/bin/env bash
# =============================================================================
# local-reset.sh - Reset completo: limpia artefactos, reconstruye y levanta
#                  servicios (Linux/macOS).
#
# Ejecuta en orden:
#   1. Detiene servicios Docker (con --volumes: borra volumenes)
#   2. Limpia artefactos locales (node_modules, target, dist, etc.)
#   3. Reinstala dependencias
#   4. Reconstruye backend + frontend
#   5. Levanta servicios con Docker y verifica salud
#
# Uso:
#   bash scripts/local-reset.sh
#   bash scripts/local-reset.sh --volumes
# =============================================================================
set -uo pipefail

CLEAN_VOLUMES=0
FORCE=0
for arg in "$@"; do
  case "$arg" in
    --volumes) CLEAN_VOLUMES=1 ;;
    --force) FORCE=1 ;;
    -h|--help) head -20 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS_DIR="$(dirname "$0")"

confirm() {
  if [[ "$FORCE" == "1" ]]; then return 0; fi
  read -r -p "$1 (s/N) " ans
  [[ "$ans" == "s" || "$ans" == "S" ]]
}

echo "=============================================="
echo "  RESET COMPLETO DEL ENTORNO LOCAL"
echo "=============================================="
echo ""

if ! confirm "Confirmas el reset completo?"; then
  echo "Reset cancelado."
  exit 0
fi

# ── 1. Detener Docker ──────────────────────────────────────
echo ""
echo ">>> 1/5 - Deteniendo servicios Docker..."
DOWN_CMD=(docker compose -f "$ROOT_DIR/docker-compose.local.yml")
for profile in observability monitoring backup public-link https; do
  DOWN_CMD+=(--profile "$profile")
done
DOWN_CMD+=(down)
if [[ "$CLEAN_VOLUMES" == "1" ]]; then
  DOWN_CMD+=(-v)
fi
if ! docker "${DOWN_CMD[@]}"; then
  echo "Error al detener servicios." >&2
  exit 1
fi
echo "  [OK] Servicios detenidos"

# ── 2. Limpiar artefactos ──────────────────────────────────
echo ""
echo ">>> 2/5 - Limpiando artefactos locales..."
CLEAN_ARGS=()
[[ "$CLEAN_VOLUMES" == "1" ]] && CLEAN_ARGS+=(--volumes)
[[ "$FORCE" == "1" ]] && CLEAN_ARGS+=(--force)
if ! bash "$SCRIPTS_DIR/clean-local.sh" "${CLEAN_ARGS[@]}"; then
  echo "Error en la limpieza de artefactos." >&2
  exit 1
fi
echo "  [OK] Artefactos limpios"

# ── 3. Setup (instalar + compilar + build) ─────────────────
echo ""
echo ">>> 3/5 - Reinstalando y reconstruyendo..."
SETUP_ARGS=()
[[ "$FORCE" == "1" ]] && SETUP_ARGS+=(--force)
if ! bash "$SCRIPTS_DIR/local-setup.sh" "${SETUP_ARGS[@]}"; then
  echo "Setup fallo. Revisa los errores arriba." >&2
  exit 1
fi
echo "  [OK] Setup completado"

# ── 4. Levantar Docker ─────────────────────────────────────
echo ""
echo ">>> 4/5 - Levantando servicios Docker..."
if ! bash "$SCRIPTS_DIR/local-start.sh"; then
  echo "Error al levantar servicios." >&2
  exit 1
fi
echo "  [OK] Servicios levantados"

# ── 5. Verificar salud ─────────────────────────────────────
echo ""
echo ">>> 5/5 - Verificando salud de servicios..."
sleep 5
if ! bash "$SCRIPTS_DIR/local-verify.sh"; then
  echo "Algunas verificaciones fallaron. Revisa los detalles arriba."
fi

echo ""
echo "=============================================="
echo "  RESET COMPLETADO"
echo "  http://localhost:5173"
echo "=============================================="
