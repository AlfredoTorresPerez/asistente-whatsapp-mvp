#!/usr/bin/env bash
# =============================================================================
# local-package.sh - Genera el paquete distribuible del proyecto (Linux/macOS).
#
# Pasos:
#   1. Compilar backend (mvnw clean package -DskipTests)
#   2. Construir frontend (pnpm build)
#   3. Registrar dependencias (mvn dependency:list)
#   4. Copiar la fuente con lista blanca (git ls-files): nunca incluye
#      node_modules, target, dist, .git, logs, capturas, archivos de
#      ejecucion, .env.* ni MEMORY.md
#   5. Adjuntar artefactos compilados (jar, dist) y configuracion
#   6. Generar manifiesto (revision, versiones fijadas, SHA-256 por archivo)
#   7. Empaquetar en un unico ZIP y emitir SHA256SUMS del ZIP
#
# Uso:
#   bash scripts/local-package.sh
#   bash scripts/local-package.sh --skip-build
# =============================================================================
set -euo pipefail

SKIP_BUILD=0
OUTPUT_DIR=""
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=1 ;;
    --output=*) OUTPUT_DIR="${arg#--output=}" ;;
    -h|--help) head -25 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$ROOT_DIR/target/package}"

REVISION=$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || echo "sin-git")
STAGING="$OUTPUT_DIR/staging"
rm -rf "$STAGING"
mkdir -p "$STAGING/metadata" "$STAGING/artifacts"

echo "=============================================="
echo "  EMPAQUETADO DE ARTEFACTOS (Fase 6)"
echo "  Revision: $REVISION"
echo "  OutputDir: $OUTPUT_DIR"
echo "=============================================="

# ── 1. Compilar backend ────────────────────────────────────
if [[ "$SKIP_BUILD" == "0" ]]; then
  echo ""
  echo ">>> 1/6 - Compilando backend..."
  (cd "$ROOT_DIR/backend-java" && ./mvnw clean package -B -DskipTests)
  echo "  [OK] Backend compilado"
fi

# ── 2. Construir frontend ──────────────────────────────────
if [[ "$SKIP_BUILD" == "0" ]]; then
  echo ""
  echo ">>> 2/6 - Construyendo frontend..."
  (cd "$ROOT_DIR/frontend-react" && pnpm build)
  echo "  [OK] Frontend construido"
fi

# ── 3. Registrar dependencias ──────────────────────────────
echo ""
echo ">>> 3/6 - Registrando dependencias (mvn dependency:list)..."
(cd "$ROOT_DIR/backend-java" && ./mvnw -B -q dependency:list -DincludeScope=runtime \
  "-DoutputFile=$STAGING/metadata/dependencies.txt")
DEP_COUNT=$(grep -c ':jar:' "$STAGING/metadata/dependencies.txt" || true)
echo "  [OK] $DEP_COUNT dependencias registradas"

# ── 4. Copiar fuente (lista blanca: git ls-files) ──────────
echo ""
echo ">>> 4/6 - Copiando fuente con lista blanca (git ls-files)..."
SOURCE_COUNT=0
while IFS= read -r rel; do
  [[ -z "$rel" ]] && continue
  target="$STAGING/$rel"
  mkdir -p "$(dirname "$target")"
  cp "$ROOT_DIR/$rel" "$target"
  SOURCE_COUNT=$((SOURCE_COUNT+1))
done < <(git -C "$ROOT_DIR" ls-files)
echo "  [OK] $SOURCE_COUNT archivos de fuente copiados (sin node_modules/target/dist/.git/.env.*)"

# ── 5. Adjuntar artefactos y configuracion ─────────────────
echo ""
echo ">>> 5/6 - Adjuntando artefactos y configuracion..."
JAR=$(ls "$ROOT_DIR/backend-java/target/"*.jar 2>/dev/null | grep -v -E 'sources|javadoc' | head -1 || true)
if [[ -n "$JAR" ]]; then
  cp "$JAR" "$STAGING/artifacts/"
  echo "  [OK] JAR del backend en artifacts/"
else
  echo "AVISO: no hay JAR en backend-java/target/ (usa --skip-build solo si ya compilaste)"
fi

if [[ -d "$ROOT_DIR/frontend-react/dist" ]]; then
  (cd "$ROOT_DIR/frontend-react/dist" && zip -qr "$STAGING/artifacts/frontend-dist.zip" .)
  echo "  [OK] frontend-dist.zip en artifacts/"
else
  echo "AVISO: frontend-react/dist no encontrado"
fi

cp "$ROOT_DIR/docker-compose.local.yml" "$STAGING/docker-compose.local.yml"
[[ -f "$ROOT_DIR/.env.local.template" ]] && cp "$ROOT_DIR/.env.local.template" "$STAGING/.env.local.template"

# ── 6. Manifiesto y ZIP ────────────────────────────────────
echo ""
echo ">>> 6/6 - Generando manifiesto y ZIP..."

NODE_VER=$(node --version 2>/dev/null || echo "n/a")
PNPM_VER=$(pnpm --version 2>/dev/null || echo "n/a")
JAVA_VER=$(java -version 2>&1 | head -1 || echo "n/a")
MVN_VER=$(cd "$ROOT_DIR/backend-java" && ./mvnw --version 2>/dev/null | head -1 || echo "n/a")
DOCKER_VER=$(docker --version 2>/dev/null || echo "n/a")
COMPOSE_VER=$(docker compose version 2>/dev/null || echo "n/a")

MANIFEST="$STAGING/metadata/manifest.json"
{
  echo "{"
  echo "  \"format\": \"asistente-package-manifest-v1\","
  echo "  \"revision\": \"$REVISION\","
  echo "  \"generated\": \"$(date -Iseconds)\","
  echo "  \"tools\": {"
  echo "    \"node\": \"$NODE_VER\","
  echo "    \"pnpm\": \"$PNPM_VER\","
  echo "    \"java\": \"$JAVA_VER\","
  echo "    \"maven\": \"$MVN_VER\","
  echo "    \"docker\": \"$DOCKER_VER\","
  echo "    \"compose\": \"$COMPOSE_VER\""
  echo "  },"
  echo "  \"sourceFiles\": $SOURCE_COUNT,"
  echo "  \"dependenciesRuntime\": $DEP_COUNT,"
  echo "  \"totalSizeBytes\": $(du -sb "$STAGING" | awk '{print $1}'),"
  echo "  \"files\": ["
  FIRST=1
  while IFS= read -r f; do
    [[ "$FIRST" == "0" ]] && echo "    ,"
    FIRST=0
    REL="${f#$STAGING/}"
    SIZE=$(stat -c %s "$f")
    SHA=$(sha256sum "$f" | awk '{print $1}')
    printf '    { "path": "%s", "size": %s, "sha256": "%s" }' "$REL" "$SIZE" "$SHA"
  done < <(find "$STAGING" -type f | sort)
  echo ""
  echo "  ]"
  echo "}"
} > "$MANIFEST"
echo "  [OK] Manifiesto generado: $MANIFEST"

ZIP_NAME="asistente-package-$REVISION.zip"
ZIP_PATH="$OUTPUT_DIR/$ZIP_NAME"
rm -f "$ZIP_PATH"
(cd "$STAGING" && zip -qr "$ZIP_PATH" .)
ZIP_SHA=$(sha256sum "$ZIP_PATH" | awk '{print $1}')
echo "$ZIP_SHA  $ZIP_NAME" > "$OUTPUT_DIR/SHA256SUMS.txt"
rm -rf "$STAGING"

ZIP_SIZE_MB=$(du -m "$ZIP_PATH" | awk '{print $1}')
echo ""
echo "  [OK] $ZIP_NAME (${ZIP_SIZE_MB} MB)"
echo "  [OK] SHA256SUMS.txt ($ZIP_SHA)"
echo ""
echo "=============================================="
echo "  EMPAQUETADO COMPLETADO"
echo "  $ZIP_PATH"
echo "=============================================="
