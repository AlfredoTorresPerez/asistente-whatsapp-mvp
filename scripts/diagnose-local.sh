#!/usr/bin/env bash
# =============================================================================
# diagnose-local.sh - Diagnostico del ambiente local de desarrollo (Linux/macOS)
#
# Verifica toolchain, recursos, puertos, configuracion, secretos (solo
# presencia de variables, nunca valores), estado del stack Docker y estado
# del frontend. Produce un reporte sanitizado compartible con -o <archivo>.
#
# Uso:
#   bash scripts/diagnose-local.sh
#   bash scripts/diagnose-local.sh -o local-diagnostics.txt
#
# Exit code: 0 = sin errores criticos; 1 = existen errores criticos.
#
# Nota: en Linux/macOS los secretos se leen de .env.local (no existe
# Credential Manager). Solo se reporta presencia de claves, nunca valores.
# =============================================================================

OUT_FILE=""
ERRORS=0
WARNINGS=0
OKS=0
RESULTS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -o|--outfile) OUT_FILE="$2"; shift 2 ;;
    -h|--help) head -30 "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Uso: $0 [-o archivo]"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

ok()   { echo "  [OK]   $1"; OKS=$((OKS+1)); }
warn() { echo "  [AVISO] $1"; WARNINGS=$((WARNINGS+1)); RESULTS+=("WARN|$2|$1|$3"); }
fail() { echo "  [ERROR] $1"; ERRORS=$((ERRORS+1)); RESULTS+=("FAIL|$2|$1|$3"); }
info() { echo "  $1"; }

# -----------------------------------------------------------------------------
# A. Toolchain
# -----------------------------------------------------------------------------
echo ""
echo "== A. Toolchain ====================================================="

if command -v java >/dev/null 2>&1; then
  JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
  if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] && (( JAVA_MAJOR >= 21 )); then
    ok "Java: $(java -version 2>&1 | head -1)"
  else
    fail "Java version detectada: $(java -version 2>&1 | head -1) (se requiere 21+)" "Toolchain" "Instalar JDK 21 (Temurin)"
  fi
else
  fail "Java no encontrado en el PATH" "Toolchain" "Instalar JDK 21 (Temurin)"
fi

if [[ -f "$ROOT_DIR/backend-java/mvnw" ]]; then
  ok "Wrapper Maven presente (backend-java/mvnw)"
else
  fail "Falta backend-java/mvnw" "Toolchain" "Restaurar el wrapper Maven"
fi

if command -v node >/dev/null 2>&1; then
  NODE_VER=$(node --version | tr -d 'v')
  NODE_MAJOR=${NODE_VER%%.*}
  NODE_MINOR=$(echo "$NODE_VER" | cut -d. -f2)
  if (( NODE_MAJOR > 20 )) || (( NODE_MAJOR == 20 && NODE_MINOR >= 19 )); then
    ok "Node: v$NODE_VER"
  else
    fail "Node version baja: v$NODE_VER (se requiere 20.19+)" "Toolchain" "Instalar Node 20.19+ (ver frontend-react/.nvmrc)"
  fi
else
  fail "Node no encontrado en el PATH" "Toolchain" "Instalar Node 20.19+ (ver frontend-react/.nvmrc)"
fi

if command -v pnpm >/dev/null 2>&1; then
  PNPM_MAJOR=$(pnpm --version | cut -d. -f1)
  if [[ "$PNPM_MAJOR" == "10" ]]; then
    ok "pnpm: $(pnpm --version)"
  else
    fail "pnpm version: $(pnpm --version) (se requiere 10.x)" "Toolchain" "corepack prepare pnpm@10.18.3 --activate"
  fi
else
  fail "pnpm no encontrado en el PATH" "Toolchain" "corepack enable && corepack prepare pnpm@10.18.3 --activate"
fi

if command -v docker >/dev/null 2>&1; then
  ok "Docker CLI: $(docker --version)"
  if docker compose version >/dev/null 2>&1; then
    ok "Docker Compose: $(docker compose version)"
  else
    fail "Docker Compose plugin no disponible" "Toolchain" "Actualizar Docker Engine (incluye plugin compose)"
  fi
else
  fail "Docker no encontrado en el PATH" "Toolchain" "Instalar Docker Engine"
fi

# -----------------------------------------------------------------------------
# B. Recursos del sistema
# -----------------------------------------------------------------------------
echo ""
echo "== B. Recursos del sistema ==========================================="

if command -v free >/dev/null 2>&1; then
  TOTAL_GB=$(free -g | awk '/^Mem:/{print $2}')
  if (( TOTAL_GB < 4 )); then
    fail "RAM insuficiente ($TOTAL_GB GB): el stack completo requiere 8+ GB" "Recursos" "Ampliar RAM o cerrar aplicaciones"
  elif (( TOTAL_GB < 8 )); then
    warn "RAM reducida ($TOTAL_GB GB): recomendado 8+ GB" "Recursos" "Cerrar aplicaciones para liberar memoria"
  else
    ok "RAM suficiente: $TOTAL_GB GB (total)"
  fi
else
  warn "free no disponible; RAM no consultable" "Recursos" ""
fi

FREE_DISK_GB=$(df -BG "$ROOT_DIR" | awk 'NR==2{print $4}' | tr -d 'G')
if [[ -n "$FREE_DISK_GB" ]] && (( FREE_DISK_GB < 10 )); then
  warn "Disco libre reducido: ${FREE_DISK_GB} GB (recomendado 10+ GB)" "Recursos" "Liberar espacio en disco"
else
  ok "Disco libre en $ROOT_DIR: ${FREE_DISK_GB} GB"
fi

# -----------------------------------------------------------------------------
# C. Puertos en uso (informativos)
# -----------------------------------------------------------------------------
echo ""
echo "== C. Puertos del stack local ========================================"
for PORT in 5433 8080 5173 8025 3000 9090 3100 3200 3001; do
  if ss -ltn 2>/dev/null | grep -q ":$PORT "; then
    info "Puerto $PORT ocupado"
  else
    info "Puerto $PORT libre"
  fi
done

# -----------------------------------------------------------------------------
# D. Configuracion del ambiente
# -----------------------------------------------------------------------------
echo ""
echo "== D. Configuracion del ambiente ===================================="

if [[ -f "$ROOT_DIR/docker-compose.local.yml" ]]; then
  ok "docker-compose.local.yml presente"
else
  fail "Falta docker-compose.local.yml" "Configuracion" "Restaurar el archivo"
fi

if [[ -f "$ROOT_DIR/.env.local" ]]; then
  KEYS=$(grep -E '^\s*[A-Za-z0-9_]+=' "$ROOT_DIR/.env.local" | sed -E 's/^\s*([A-Za-z0-9_]+)=.*/\1/' | tr '\n' ' ')
  ok ".env.local presente ($(echo $KEYS | wc -w) variables definidas)"
  info "Variables definidas (solo nombres, sin valores): $KEYS"
  if ! echo "$KEYS" | grep -q "APP_JWT_SECRET"; then
    warn "APP_JWT_SECRET no esta definido en .env.local" "Configuracion" "Copiar .env.local.template a .env.local y definir APP_JWT_SECRET"
  fi
else
  fail "Falta .env.local (no se puede levantar el stack)" "Configuracion" "Copiar .env.local.template a .env.local"
fi

if [[ -f "$ROOT_DIR/caddy/Caddyfile.local" ]]; then
  ok "caddy/Caddyfile.local presente (perfil https)"
else
  info "caddy/Caddyfile.local ausente (perfil https no disponible)"
fi

for S in local-start.sh local-stop.sh local-verify.sh local-setup.sh local-reset.sh clean-local.sh diagnose-local.sh; do
  [[ -f "$ROOT_DIR/scripts/$S" ]] || warn "Falta script core: scripts/$S" "Configuracion" "Restaurar el script desde el repositorio"
done

# -----------------------------------------------------------------------------
# E. Secretos (solo presencia de claves en .env.local, nunca valores)
# -----------------------------------------------------------------------------
echo ""
echo "== E. Secretos ======================================================="
echo "  Linux/macOS: los secretos se leen de .env.local."
echo "  Solo se reporta presencia de claves; nunca valores."
if [[ -f "$ROOT_DIR/.env.local" ]]; then
  for KEY in APP_JWT_SECRET APP_WHATSAPP_CLOUD_API_APP_SECRET APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN APP_OPENAI_API_KEY; do
    if grep -qE "^\s*$KEY=" "$ROOT_DIR/.env.local"; then
      ok "Clave presente en .env.local: $KEY"
    else
      warn "Clave ausente en .env.local: $KEY" "Secretos" "Definir $KEY en .env.local segun .env.local.template"
    fi
  done
else
  fail "No se puede verificar secretos sin .env.local" "Secretos" "Crear .env.local desde .env.local.template"
fi

# -----------------------------------------------------------------------------
# F. Stack Docker
# -----------------------------------------------------------------------------
echo ""
echo "== F. Stack Docker ==================================================="

if docker info >/dev/null 2>&1; then
  ok "Daemon Docker activo"
  CONTAINERS=$(docker ps -a --format '{{.Names}}|{{.Status}}' | grep '^asistente-' || true)
  if [[ -n "$CONTAINERS" ]]; then
    while IFS= read -r LINE; do
      NAME="${LINE%%|*}"
      STATUS="${LINE#*|}"
      if [[ "$STATUS" == Up* ]]; then
        HEALTH=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$NAME")
        if [[ "$HEALTH" == "healthy" || "$HEALTH" == "none" ]]; then
          ok "$NAME -> $STATUS (health: $HEALTH)"
        else
          warn "$NAME -> $STATUS (health: $HEALTH)" "Stack" "Revisar docker logs $NAME"
        fi
      else
        warn "$NAME -> $STATUS" "Stack" "Ejecutar scripts/local-start.sh"
      fi
    done <<< "$CONTAINERS"
  else
    warn "No hay contenedores del stack local (asistente-*)" "Stack" "Ejecutar scripts/local-start.sh"
  fi
else
  fail "Daemon Docker no disponible" "Stack" "Iniciar Docker Engine y esperar a que este listo"
fi

# -----------------------------------------------------------------------------
# G. Frontend
# -----------------------------------------------------------------------------
echo ""
echo "== G. Frontend ======================================================="

if [[ -f "$ROOT_DIR/frontend-react/pnpm-lock.yaml" ]]; then
  ok "pnpm-lock.yaml presente"
else
  fail "Falta frontend-react/pnpm-lock.yaml" "Frontend" "Ejecutar scripts/local-setup.sh"
fi

if [[ -d "$ROOT_DIR/frontend-react/node_modules" ]]; then
  ok "node_modules presente (frontend)"
else
  warn "node_modules ausente (se instalara con local-setup.sh)" "Frontend" "Ejecutar scripts/local-setup.sh"
fi

if [[ -f "$ROOT_DIR/frontend-react/package.json" ]] && grep -q '"node": ">=20.19.0"' "$ROOT_DIR/frontend-react/package.json"; then
  ok "engines.node en package.json: >=20.19.0"
else
  warn "package.json sin engines.node >=20.19.0" "Frontend" "Agregar engines.node al package.json"
fi

# -----------------------------------------------------------------------------
# Reporte
# -----------------------------------------------------------------------------
echo ""
echo "== Resumen =========================================================="
echo "  OK: $OKS | AVISOS: $WARNINGS | ERRORES: $ERRORS"

if [[ "$ERRORS" -gt 0 ]]; then
  echo ""
  echo "  Acciones requeridas:"
  for R in "${RESULTS[@]}"; do
    IFS='|' read -r _ SECTION MSG ACTION <<< "$R"
    echo "    - [$SECTION] $MSG"
    [[ -n "$ACTION" ]] && echo "      Accion: $ACTION"
  done
fi

if [[ -n "$OUT_FILE" ]]; then
  {
    echo "Diagnostico del ambiente local - $(date '+%Y-%m-%d %H:%M')"
    echo "Host: $(hostname) | OS: $(uname -sr)"
    echo "NOTA: reporte sanitizado; no incluye valores de secretos ni de .env.local."
    echo ""
    for R in "${RESULTS[@]}"; do
      IFS='|' read -r STATUS SECTION MSG ACTION <<< "$R"
      echo "[$STATUS] $SECTION: $MSG"
      [[ -n "$ACTION" ]] && echo "    Accion: $ACTION"
    done
    echo ""
    echo "Resumen: $OKS OK | $WARNINGS avisos | $ERRORS errores"
  } > "$OUT_FILE"
  echo ""
  echo "Reporte sanitizado guardado en: $OUT_FILE"
fi

if [[ "$ERRORS" -gt 0 ]]; then
  echo ""
  echo "Resultado: ERRORES CRITICOS (corregir antes de continuar)."
  exit 1
fi
echo ""
echo "Resultado: ambiente OK (pueden existir avisos no bloqueantes)."
exit 0
