#!/usr/bin/env bash
# =============================================================================
# local-reset-demo.sh - Regenera los datos demo del ambiente local desde cero
#                       (Linux/macOS).
#
# Reconstruye la base de datos local (postgres) para que las migraciones
# Flyway vuelvan a aplicar los seeds demo y el LocalDataInitializer refresque
# las fechas de las reservas de ejemplo. No toca codigo, node_modules, target
# ni dist. No borra backups ni observabilidad.
#
# Uso:
#   bash scripts/local-reset-demo.sh
#   bash scripts/local-reset-demo.sh --force
# =============================================================================
set -uo pipefail

FORCE=0
for arg in "$@"; do
  case "$arg" in
    --force) FORCE=1 ;;
    -h|--help) head -15 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.local.yml"
ENV_FILE="$ROOT_DIR/.env.local"
VOLUME_NAME="asistente_postgres-data"

confirm() {
  if [[ "$FORCE" == "1" ]]; then return 0; fi
  read -r -p "$1 (s/N) " ans
  [[ "$ans" == "s" || "$ans" == "S" ]]
}

echo "=============================================="
echo "  REGENERACION DE DATOS DEMO LOCALES"
echo "=============================================="
echo ""
echo "Esto elimina el volumen $VOLUME_NAME (base de datos local) y la"
echo "recrea aplicando las migraciones Flyway (seeds demo) y el"
echo "LocalDataInitializer (fechas de reservas de ejemplo)."
echo "No se eliminan: codigo, node_modules, target, dist, backups ni"
echo "observabilidad."
echo ""

if ! confirm "Confirmas la regeneracion de datos demo?"; then
  echo "Regeneracion cancelada."
  exit 0
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "No se encuentra $COMPOSE_FILE" >&2
  exit 1
fi

DOCKER=(docker compose)
[[ -f "$ENV_FILE" ]] && DOCKER+=(--env-file "$ENV_FILE")
DOCKER+=(-f "$COMPOSE_FILE")

# ── 1. Detener postgres y backend ───────────────────────────
echo ""
echo ">>> 1/5 - Deteniendo postgres y backend..."
docker "${DOCKER[@]}" stop postgres backend-java >/dev/null 2>&1 || echo "  (los servicios podian no estar corriendo)"
echo "  [OK] Servicios detenidos"

# ── 2. Eliminar contenedor postgres ─────────────────────────
echo ""
echo ">>> 2/5 - Eliminando contenedor postgres..."
docker "${DOCKER[@]}" rm -f postgres >/dev/null 2>&1 || true
echo "  [OK] Contenedor postgres eliminado"

# ── 3. Eliminar volumen de datos ────────────────────────────
echo ""
echo ">>> 3/5 - Eliminando volumen $VOLUME_NAME..."
docker volume rm "$VOLUME_NAME" >/dev/null 2>&1 || true
echo "  [OK] Volumen eliminado (o inexistente)"

# ── 4. Levantar postgres y esperar healthy ──────────────────
echo ""
echo ">>> 4/5 - Levantando postgres (Flyway aplicara seeds al iniciar backend)..."
if ! docker "${DOCKER[@]}" up -d postgres >/dev/null; then
  echo "No se pudo levantar postgres" >&2
  exit 1
fi

DEADLINE=$(( $(date +%s) + 90 ))
HEALTH=""
while [[ "$HEALTH" != "healthy" && "$HEALTH" != "none" && $(date +%s) -lt $DEADLINE ]]; do
  sleep 3
  HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' asistente-postgres 2>/dev/null || echo "")
done
echo "  [OK] postgres listo (health: $HEALTH)"

# ── 5. Reiniciar backend (Flyway + LocalDataInitializer) ───
echo ""
echo ">>> 5/5 - Reiniciando backend (migraciones + datos demo)..."
if ! docker "${DOCKER[@]}" restart backend-java >/dev/null 2>&1; then
  echo "No se pudo reiniciar backend" >&2
  exit 1
fi

DEADLINE=$(( $(date +%s) + 180 ))
HEALTH=""
while [[ "$HEALTH" != "healthy" && $(date +%s) -lt $DEADLINE ]]; do
  sleep 5
  HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' asistente-backend 2>/dev/null || echo "")
done

if [[ "$HEALTH" != "healthy" ]]; then
  echo "AVISO: el backend no llego a healthy en 180s."
  echo "Accion: bash scripts/local-verify.sh o docker logs asistente-backend"
else
  echo "  [OK] backend healthy (datos demo regenerados)"
fi

# ── Verificar ────────────────────────────────────────────────
echo ""
echo "=== Verificando el entorno ==="
if ! bash "$(dirname "$0")/local-verify.sh"; then
  echo "AVISO: algunas verificaciones fallaron. Accion: bash scripts/diagnose-local.sh"
fi

echo ""
echo "=============================================="
echo "  DATOS DEMO REGENERADOS"
echo "  Frontend: http://localhost:5173"
echo "=============================================="
exit 0
