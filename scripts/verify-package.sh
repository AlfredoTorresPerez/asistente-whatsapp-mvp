#!/usr/bin/env bash
# =============================================================================
# verify-package.sh - Prueba automatica de reconstruccion desde el paquete
#                     distribuible (Linux/macOS).
#
# Toma el ZIP generado por local-package.sh, lo extrae en un directorio
# temporal limpio y verifica:
#   1. Integridad: cada archivo del manifiesto coincide con su SHA-256
#   2. Ausencia de artefactos empaquetados accidentalmente
#   3. Reconstruccion del frontend: pnpm install --frozen-lockfile
#   4. Reconstruccion del backend: mvnw package -DskipTests
#   5. Build del frontend: pnpm build
#   6. docker compose config --quiet
#
# Uso:
#   bash scripts/verify-package.sh
#   bash scripts/verify-package.sh <ruta-al-zip>
#   bash scripts/verify-package.sh <ruta-al-zip> --skip-rebuild
# =============================================================================
set -uo pipefail

SKIP_REBUILD=0
PACKAGE=""
for arg in "$@"; do
  case "$arg" in
    --skip-rebuild) SKIP_REBUILD=1 ;;
    -h|--help) head -20 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) PACKAGE="$arg" ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FAILED=0

ok()   { echo "  [OK]   $1"; }
fail() { echo "  [FAIL] $1"; FAILED=1; }

if [[ -z "$PACKAGE" ]]; then
  PACKAGE=$(ls -t "$ROOT_DIR"/target/package/asistente-package-*.zip 2>/dev/null | head -1 || true)
fi
if [[ -z "$PACKAGE" || ! -f "$PACKAGE" ]]; then
  fail "No se encontro el paquete. Ejecuta primero scripts/local-package.sh"
  exit 1
fi

WORK="$(mktemp -d)/verify-package"
mkdir -p "$WORK"
echo "=== VERIFICACION DEL PAQUETE: $(basename "$PACKAGE") ==="
echo "  Extraido en: $WORK"

# ── 1. Extraer ──────────────────────────────────────────────
echo ""
echo "1/6 - Extrayendo el paquete..."
if ! (cd "$WORK" && unzip -q "$PACKAGE"); then
  fail "No se pudo extraer el paquete"
  exit 1
fi
if [[ ! -f "$WORK/metadata/manifest.json" ]]; then
  fail "Falta metadata/manifest.json en el paquete"
  exit 1
fi
REVISION=$(grep '"revision"' "$WORK/metadata/manifest.json" | head -1 | sed -E 's/.*"revision": "([^"]*)".*/\1/')
TOTAL_FILES=$(grep '"totalFiles"' "$WORK/metadata/manifest.json" | head -1 | grep -oE '[0-9]+')
ok "Manifiesto leido (revision $REVISION, $TOTAL_FILES archivos)"

# ── 2. Verificar SHA-256 de cada archivo ───────────────────
echo ""
echo "2/6 - Verificando SHA-256 del manifiesto..."
MISMATCHES=0
CHECKED=0
while IFS= read -r line; do
  path=$(echo "$line" | sed -nE 's/.*"path": "([^"]*)".*/\1/p')
  sha=$(echo "$line" | sed -nE 's/.*"sha256": "([^"]*)".*/\1/p')
  [[ -z "$path" ]] && continue
  CHECKED=$((CHECKED+1))
  actual=$(sha256sum "$WORK/$path" | awk '{print $1}')
  if [[ "$actual" != "$sha" ]]; then
    fail "SHA-256 no coincide: $path"
    MISMATCHES=$((MISMATCHES+1))
  fi
done < <(grep -oE '\{ "path": "[^"]*", "size": [0-9]+, "sha256": "[a-f0-9]+" \}' "$WORK/metadata/manifest.json")
if [[ "$MISMATCHES" == "0" ]]; then
  ok "SHA-256 verificado para $CHECKED archivos"
fi

# ── 3. Ausencia de artefactos accidentalmente empaquetados ─
echo ""
echo "3/6 - Verificando exclusiones (lista blanca)..."
VIOLATIONS=0
for pat in "node_modules" "target" "dist" ".git" ".env.local" ".env.qa" ".env.production" "MEMORY.md" "logs" "observabilidad-capturas" "registro_ejecucion_IA.json" "e2e/reports" "e2e/screenshots" "playwright-report" "test-results"; do
  # coincidencia por segmento de ruta: "(^|/)pat($|/)"
  if (cd "$WORK" && find . -type f | grep -E "(^|/)$pat($|/)" | grep -qv '^\./'); then
    fail "Artefacto empaquetado accidentalmente: $pat"
    VIOLATIONS=$((VIOLATIONS+1))
  fi
done
if [[ "$VIOLATIONS" == "0" ]]; then
  ok "Sin artefactos empaquetados accidentalmente (15 patrones auditados)"
fi

# ── 4. Reconstruccion frontend (install limpio) ─────────────
echo ""
echo "4/6 - Reconstruyendo frontend (pnpm install --frozen-lockfile)..."
if [[ "$SKIP_REBUILD" == "1" ]]; then
  echo "  [--] Omitido (--skip-rebuild)"
else
  if (cd "$WORK/frontend-react" && pnpm install --frozen-lockfile >/dev/null 2>&1); then
    ok "pnpm install --frozen-lockfile"
  else
    fail "pnpm install fallo"
  fi
fi

# ── 5. Reconstruccion backend ───────────────────────────────
echo ""
echo "5/6 - Reconstruyendo backend (mvnw package -DskipTests)..."
if [[ "$SKIP_REBUILD" == "1" ]]; then
  echo "  [--] Omitido (--skip-rebuild)"
else
  if (cd "$WORK/backend-java" && ./mvnw -B -DskipTests package >/dev/null 2>&1); then
    ok "mvnw package (jar generado)"
  else
    fail "mvnw package fallo"
  fi
fi

# ── 6. Build frontend + compose config ─────────────────────
echo ""
echo "6/6 - Build frontend y config compose..."
if [[ "$SKIP_REBUILD" == "1" ]]; then
  echo "  [--] Build omitido (--skip-rebuild)"
else
  if (cd "$WORK/frontend-react" && pnpm build >/dev/null 2>&1); then
    ok "pnpm build (dist generado)"
  else
    fail "pnpm build fallo"
  fi
fi
if [[ -f "$WORK/docker-compose.local.yml" ]]; then
  if (cd "$WORK" && docker compose -f docker-compose.local.yml config --quiet 2>/dev/null); then
    ok "docker compose config valido"
  else
    fail "docker compose config fallo"
  fi
else
  fail "Falta docker-compose.local.yml en el paquete"
fi

rm -rf "$(dirname "$WORK")"

echo ""
if [[ "$FAILED" == "1" ]]; then
  echo "VERIFICACION DEL PAQUETE: FALLOS"
  exit 1
fi
echo "VERIFICACION DEL PAQUETE: TODO OK"
exit 0
