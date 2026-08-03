#!/usr/bin/env bash
# =============================================================================
# clean-local.sh - Limpia artefactos regenerables del proyecto (Linux/macOS).
#
# Elimina node_modules, target, dist, cobertura, caches, logs y archivos
# temporales. NO elimina datos fuente, migraciones, pruebas ni .env.local.
# Requiere confirmacion antes de eliminar volumenes Docker.
#
# Uso:
#   bash scripts/clean-local.sh
#   bash scripts/clean-local.sh --volumes --force
# =============================================================================
set -uo pipefail

CLEAN_VOLUMES=0
FORCE=0
for arg in "$@"; do
  case "$arg" in
    --volumes) CLEAN_VOLUMES=1 ;;
    --force) FORCE=1 ;;
    -h|--help) head -15 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

confirm() {
  if [[ "$FORCE" == "1" ]]; then return 0; fi
  read -r -p "$1 (s/N) " ans
  [[ "$ans" == "s" || "$ans" == "S" ]]
}

echo "=== Limpieza de artefactos regenerables ==="
echo ""

# ── Frontend ──────────────────────────────────────────────
if [[ -d "$ROOT_DIR/frontend-react/node_modules" ]] && confirm "Eliminar node_modules del frontend?"; then
  rm -rf "$ROOT_DIR/frontend-react/node_modules"
  echo "  [OK] node_modules eliminado"
fi

for d in dist coverage .vite playwright-report test-results; do
  if [[ -d "$ROOT_DIR/frontend-react/$d" ]]; then
    rm -rf "$ROOT_DIR/frontend-react/$d"
    echo "  [OK] frontend-react/$d eliminado"
  fi
done

if [[ -d "$ROOT_DIR/frontend-react/e2e/screenshots" ]]; then
  rm -rf "$ROOT_DIR/frontend-react/e2e/screenshots"
  echo "  [OK] e2e/screenshots eliminado"
fi

# ── Backend ────────────────────────────────────────────────
if [[ -d "$ROOT_DIR/backend-java/target" ]]; then
  rm -rf "$ROOT_DIR/backend-java/target"
  echo "  [OK] target eliminado"
fi

# ── Logs ───────────────────────────────────────────────────
rm -f "$ROOT_DIR"/*.log
echo "  [OK] archivos .log eliminados"
if [[ -d "$ROOT_DIR/backend-java/logs" ]]; then
  rm -rf "$ROOT_DIR/backend-java/logs"/*
  echo "  [OK] backend-java/logs limpiados"
fi

# ── TypeScript ─────────────────────────────────────────────
find "$ROOT_DIR" -name "*.tsbuildinfo" -type f -delete 2>/dev/null
echo "  [OK] archivos .tsbuildinfo eliminados"

# ── Docker volumes ─────────────────────────────────────────
if [[ "$CLEAN_VOLUMES" == "1" ]]; then
  echo ""
  echo "=== LIMPIEZA DE VOLUMENES DOCKER ==="
  echo "ADVERTENCIA: Esto eliminara la base de datos local y todos sus datos."
  if confirm "Confirmas eliminar los volumenes Docker?"; then
    docker compose -p asistente -f "$ROOT_DIR/docker-compose.local.yml" \
      --profile observability --profile monitoring --profile backup \
      --profile public-link --profile https down -v >/dev/null 2>&1
    echo "  [OK] volumenes Docker eliminados"
  fi
fi

# ── Lock files huerfanos ──────────────────────────────────
for f in package-lock.json yarn.lock; do
  if [[ -f "$ROOT_DIR/frontend-react/$f" ]]; then
    rm -f "$ROOT_DIR/frontend-react/$f"
    echo "  [OK] $f huerfano eliminado"
  fi
done

echo ""
echo "=== Limpieza completada ==="
