#!/usr/bin/env bash
# =============================================================================
# local-setup.sh - Verifica prerequisitos, instala dependencias y construye
#                  backend + frontend para entorno local (Linux/macOS).
#
# Uso:
#   bash scripts/local-setup.sh
#   bash scripts/local-setup.sh --skip-backend
# =============================================================================
set -euo pipefail

SKIP_BACKEND=0
SKIP_FRONTEND=0
SKIP_INSTALL=0

for arg in "$@"; do
  case "$arg" in
    --skip-backend) SKIP_BACKEND=1 ;;
    --skip-frontend) SKIP_FRONTEND=1 ;;
    --skip-install) SKIP_INSTALL=1 ;;
    -h|--help) head -20 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opcion desconocida: $arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

fail() { echo "  [FAIL] $1"; [[ -n "${2:-}" ]] && echo "  Accion: $2"; exit 1; }
ok()   { echo "  [OK]   $1"; }

# ── 0. Verificar plataforma ────────────────────────────────
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) fail "Este script es para Linux/macOS. En Windows usa scripts/local-setup.ps1" ;;
esac

# ── 1. Verificar prerequisitos ─────────────────────────────
echo ""
echo ">>> Verificando prerequisitos..."

# Java 21+
if command -v java >/dev/null 2>&1; then
  JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
  if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] && (( JAVA_MAJOR >= 21 )); then
    ok "Java $JAVA_MAJOR"
  else
    fail "Se requiere Java 21+, detectado: $(java -version 2>&1 | head -1)" "Actualiza tu JDK a 21+ (Temurin)"
  fi
else
  fail "Java no encontrado" "Instala Java 21+ (Eclipse Temurin recomendado)"
fi

# Docker
if command -v docker >/dev/null 2>&1; then
  ok "Docker: $(docker --version)"
  if docker compose version >/dev/null 2>&1; then
    ok "Compose: $(docker compose version)"
  else
    fail "Plugin Docker Compose no disponible" "Actualiza Docker Engine (incluye el plugin compose)"
  fi
else
  fail "Docker no encontrado" "Instala Docker Engine"
fi

# Node 20.19+
if command -v node >/dev/null 2>&1; then
  NODE_VER=$(node --version | tr -d 'v')
  NODE_MAJOR=${NODE_VER%%.*}
  NODE_MINOR=$(echo "$NODE_VER" | cut -d. -f2)
  if (( NODE_MAJOR > 20 )) || (( NODE_MAJOR == 20 && NODE_MINOR >= 19 )); then
    ok "Node: v$NODE_VER"
  else
    fail "Se requiere Node 20.19+, detectado: v$NODE_VER" "Instala Node 20.19+ (ver frontend-react/.nvmrc)"
  fi
else
  fail "Node.js no encontrado" "Instala Node 20.19+ (ver frontend-react/.nvmrc)"
fi

# pnpm 10.x
if command -v pnpm >/dev/null 2>&1; then
  PNPM_MAJOR=$(pnpm --version | cut -d. -f1)
  if [[ "$PNPM_MAJOR" == "10" ]]; then
    ok "pnpm: $(pnpm --version)"
  else
    fail "Se requiere pnpm 10.x, detectado: $(pnpm --version)" "corepack prepare pnpm@10.18.3 --activate"
  fi
else
  fail "pnpm no encontrado" "corepack enable && corepack prepare pnpm@10.18.3 --activate"
fi

# Lockfile del frontend
if [[ -f "$ROOT_DIR/frontend-react/pnpm-lock.yaml" ]]; then
  ok "Lockfile presente: frontend-react/pnpm-lock.yaml"
else
  fail "Falta frontend-react/pnpm-lock.yaml" "El lockfile es obligatorio para instalar con --frozen-lockfile"
fi

# ── 2. Instalar dependencias ───────────────────────────────
if [[ "$SKIP_INSTALL" == "1" ]]; then
  echo "  --skip-install: se omite la instalacion de dependencias"
else
  echo ""
  echo ">>> Instalando dependencias del frontend (pnpm install --frozen-lockfile)..."
  cd "$ROOT_DIR/frontend-react"
  if [[ -d node_modules ]]; then
    echo "  node_modules existe: se revalida contra el lockfile (--frozen-lockfile es idempotente)"
  fi
  pnpm install --frozen-lockfile
  ok "pnpm install --frozen-lockfile"
fi

# ── 3. Compilar backend ────────────────────────────────────
if [[ "$SKIP_BACKEND" == "1" ]]; then
  echo "  --skip-backend: se omite la compilacion del backend"
else
  echo ""
  echo ">>> Compilando backend (mvnw clean package -DskipTests)..."
  cd "$ROOT_DIR/backend-java"
  ./mvnw clean package -DskipTests -B
  ok "Backend compilado"
fi

# ── 4. Construir frontend ──────────────────────────────────
if [[ "$SKIP_FRONTEND" == "1" ]]; then
  echo "  --skip-frontend: se omite el build del frontend"
else
  echo ""
  echo ">>> Construyendo frontend (pnpm build)..."
  cd "$ROOT_DIR/frontend-react"
  pnpm build
  ok "Frontend construido"
fi

# ── Resumen ─────────────────────────────────────────────────
echo ""
echo "=============================================="
echo "  SETUP COMPLETADO"
echo "  Usa scripts/local-start.sh para"
echo "  levantar los servicios con Docker."
echo "=============================================="
