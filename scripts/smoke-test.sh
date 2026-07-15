#!/usr/bin/env bash
# =============================================================================
# SMOKE TEST - Verifica que todos los servicios respondan correctamente
# Uso: ./scripts/smoke-test.sh
# =============================================================================
set -euo pipefail

echo "=== Smoke Test: Asistente WhatsApp ==="
echo ""

FAILED=0

check_service() {
    local name=$1
    local url=$2
    local expected=$3
    local response

    echo -n "  [$name] $url ... "
    response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")
    if [ "$response" = "$expected" ]; then
        echo "OK ($response)"
    else
        echo "FAIL (expected $expected, got $response)"
        FAILED=$((FAILED + 1))
    fi
}

check_body() {
    local name=$1
    local url=$2
    local pattern=$3
    local response

    echo -n "  [$name] $url ... "
    response=$(curl -s --max-time 5 "$url" 2>/dev/null || echo "")
    if echo "$response" | grep -q "$pattern"; then
        echo "OK"
    else
        echo "FAIL (no contiene '$pattern')"
        echo "    Response: ${response:0:200}"
        FAILED=$((FAILED + 1))
    fi
}

echo "--- Backend ---"
check_service  "Health"    "http://localhost:8080/actuator/health" "200"
check_body    "Status"    "http://localhost:8080/api/v1/health"   "UP"

echo "--- Frontend ---"
check_service  "Frontend" "http://localhost:5173/" "200"

echo ""
if [ $FAILED -eq 0 ]; then
    echo "=== RESULTADO: Todos los servicios responden correctamente ==="
    exit 0
else
    echo "=== RESULTADO: $FAILED servicio(s) fallaron ==="
    exit 1
fi
