#!/usr/bin/env bash
# =============================================================================
# OBSERVABILIDAD VERIFY - Verifica el estado del stack de observabilidad local
# Uso: ./scripts/observability-verify.sh
# Comprueba contenedores, endpoints, targets de Prometheus y trazas en Tempo.
# =============================================================================
set -uo pipefail

FAILED=0

ok()   { echo "  [OK] $1"; }
fail() { echo "  [FAIL] $1"; FAILED=$((FAILED + 1)); }

echo "=================================================="
echo "  VERIFICACION DE OBSERVABILIDAD LOCAL"
echo "=================================================="

echo ""
echo "1/6 - Verificando contenedores Docker..."
for name in asistente-prometheus asistente-loki asistente-tempo asistente-alloy asistente-grafana; do
    state=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "ausente")
    if [ "$state" = "healthy" ]; then
        ok "$name = healthy"
    else
        fail "$name = $state"
    fi
done

echo ""
echo "2/6 - Verificando backend..."
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "http://localhost:8080/actuator/health" || echo "000")
if [ "$code" = "200" ]; then
    ok "Backend /actuator/health = 200"
else
    fail "Backend /actuator/health = $code"
fi

metrics=$(curl -s --max-time 10 "http://localhost:8080/actuator/prometheus" || echo "")
if echo "$metrics" | grep -q "assistente_whatsapp_mensajes_recibidos_total"; then
    ok "Metricas funcionales asistente_* expuestas"
else
    fail "Metricas asistente_* no encontradas en /actuator/prometheus"
fi

echo ""
echo "3/6 - Verificando Prometheus..."
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "http://localhost:9090/-/healthy" || echo "000")
if [ "$code" = "200" ]; then
    ok "Prometheus /-/healthy = 200"
else
    fail "Prometheus /-/healthy = $code"
fi

echo ""
echo "4/6 - Verificando Loki..."
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "http://localhost:3100/ready" || echo "000")
if [ "$code" = "200" ]; then
    ok "Loki /ready = 200"
else
    fail "Loki /ready = $code"
fi

echo ""
echo "5/6 - Verificando Tempo..."
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "http://localhost:3200/ready" || echo "000")
if [ "$code" = "200" ]; then
    ok "Tempo /ready = 200"
else
    fail "Tempo /ready = $code"
fi

traces=$(curl -s --max-time 10 "http://localhost:9090/api/v1/query?query=tempo_request_duration_seconds_count" || echo "")
if echo "$traces" | grep -q '"result":\[\]'; then
    fail "Tempo sin trazas recibidas (alerta TempoSinTrazas se activaria)"
else
    ok "Tempo ha recibido trazas"
fi

echo ""
echo "6/6 - Verificando Grafana..."
health=$(curl -s --max-time 10 "http://localhost:3000/api/health" || echo "")
if echo "$health" | grep -q '"database":"ok"'; then
    ok "Grafana /api/health = ok"
else
    fail "Grafana health no valida: $health"
fi

echo ""
if [ "$FAILED" -eq 0 ]; then
    echo "=================================================="
    echo "  VERIFICACION COMPLETADA: TODO OK"
    echo "=================================================="
    exit 0
else
    echo "=================================================="
    echo "  VERIFICACION COMPLETADA CON FALLOS ($FAILED)"
    echo "=================================================="
    exit 1
fi
