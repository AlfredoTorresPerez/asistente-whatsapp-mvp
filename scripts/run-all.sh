#!/usr/bin/env bash
# =============================================================================
# run-all.sh — Central task runner (Linux/macOS)
# Ejecuta validaciones en todos los componentes autorizados.
# Se detiene al primer error.
# =============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo ""
echo "=========================================="
echo "  Backend: verify"
echo "=========================================="
cd "$ROOT_DIR/backend-java"
./mvnw verify -B -Dspring.profiles.active=test

echo ""
echo "=========================================="
echo "  Frontend: format check"
echo "=========================================="
cd "$ROOT_DIR/frontend-react"
pnpm format:check

echo ""
echo "=========================================="
echo "  Frontend: lint"
echo "=========================================="
cd "$ROOT_DIR/frontend-react"
pnpm lint

echo ""
echo "=========================================="
echo "  Frontend: test"
echo "=========================================="
cd "$ROOT_DIR/frontend-react"
pnpm test -- --run

echo ""
echo "=========================================="
echo "  Frontend: build"
echo "=========================================="
cd "$ROOT_DIR/frontend-react"
pnpm build

echo ""
echo "=========================================="
echo "  TODOS LOS COMPONENTES AUTORIZADOS OK"
echo "=========================================="
