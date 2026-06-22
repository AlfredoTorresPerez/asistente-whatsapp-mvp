#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
BACKEND="$ROOT/backend-java"
cd "$BACKEND"
if [ "${1:-}" = "--strict" ]; then
  ./mvnw -Dtest=AiExcelMatrixOrchestratorCoverageTest -Dai.matrix.strict=true test
else
  ./mvnw -Dtest=AiExcelMatrixOrchestratorCoverageTest test
fi
printf '%s\n' "Reporte: $BACKEND/target/ai-matrix/reporte_matriz_excel_ia_v23_4_16.md"
