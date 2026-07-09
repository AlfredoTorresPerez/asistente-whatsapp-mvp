#!/bin/bash
# Script para ejecutar toda la suite E2E del Asistente de Negocios WhatsApp
# Uso:
#   ./run-all.sh              # Ejecuta all-chromium
#   ./run-all.sh --smoke      # Solo smoke tests
#   ./run-all.sh --all        # Todas las suites

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
E2E_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$E2E_DIR")"

echo "============================================"
echo " Asistente de Negocios - Suite E2E"
echo "============================================"
echo "Directorio: $PROJECT_DIR"
echo ""

cd "$PROJECT_DIR"

# Detener frontend container si está corriendo
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'asistente-whatsapp-frontend'; then
  echo "[INFO] Deteniendo contenedor asistente-whatsapp-frontend..."
  docker stop asistente-whatsapp-frontend 2>/dev/null || true
  echo "[INFO] Contenedor detenido. Playwright usará su propio dev server."
fi

case "${1:-}" in
  --smoke)
    PROJECT=smoke
    ;;
  --all)
    PROJECT=
    ;;
  *)
    PROJECT=all-chromium
    ;;
esac

if [ -n "$PROJECT" ]; then
  echo "[EXEC] npx playwright test --project=$PROJECT"
  npx playwright test --project="$PROJECT"
else
  echo "[EXEC] npx playwright test"
  npx playwright test
fi

echo ""
echo "[REPORT] Generando reporte consolidado..."
npx ts-node "$SCRIPT_DIR/generate-report.ts"

echo ""
echo "[DONE] Reporte disponible en: e2e/reports/"
echo "       HTML: e2e/reports/html-report/index.html"
echo "       JSON: e2e/reports/test-results.json"
echo "       MD:   e2e/reports/test-status-report.md"
