#!/usr/bin/env bash
# test-ai-auto-reply-local.sh
# Test IA Auto-reply: webhook -> ai_reply_outbox -> PROCESSED -> outbound_message

set -euo pipefail

TOKEN="${1:-}"
[[ -n "$TOKEN" ]] || { echo "Uso: $0 <BEARER_TOKEN>"; exit 1; }

BASE_URL="http://localhost:8080"
WEBHOOK_SECRET="${APP_WHATSAPP_WEB_WEBHOOK_SECRET:-dev-whatsapp-web-webhook-secret}"
AUTH_HEADER="Authorization: Bearer $TOKEN"

hmac_sha256() {
    local key="$1" msg="$2"
    echo -n "$msg" | openssl dgst -sha256 -hmac "$key" -binary | xxd -p -c 256 | tr -d '\n'
}

echo "=== Test IA Auto-reply Local ==="

# 1. Stats outbox ANTES
STATS_BEFORE=$(curl -sf -H "$AUTH_HEADER" "$BASE_URL/api/v1/ai/outbox/stats")
echo "Outbox ANTES: $(echo "$STATS_BEFORE" | jq -c '.')"

# 2. Enviar mensaje via webhook
PHONE="56950954580"
DELIVERY_ID=$(uuidgen)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")
SESSION_KEY="demo-sales"
MSG="Hola, quiero agendar una limpieza facial para mañana a las 10:00 en Providencia"

PAYLOAD=$(cat <<EOF | jq -c .
{
  "eventType": "MESSAGE_RECEIVED",
  "deliveryId": "$DELIVERY_ID",
  "occurredAt": "$TIMESTAMP",
  "sessionKey": "$SESSION_KEY",
  "payload": {
    "from": "$PHONE",
    "body": "$MSG",
    "to": "56927305158",
    "externalMessageId": "ext-$DELIVERY_ID",
    "chatId": "$PHONE@c.us",
    "hasMedia": false,
    "messageType": "text",
    "timestamp": $(date -u +%s)
  }
}
EOF
)

BODY="$PAYLOAD"
SIG="sha256=$(hmac_sha256 "$WEBHOOK_SECRET" "$TIMESTAMP.$BODY")"

echo "Enviando mensaje para trigger IA..."
curl -sf -X POST "$BASE_URL/api/v1/integrations/whatsapp-web/webhook" \
    -H "Content-Type: application/json" \
    -H "X-WhatsApp-Web-Timestamp: $TIMESTAMP" \
    -H "X-WhatsApp-Web-Signature: $SIG" \
    -H "X-WhatsApp-Web-Delivery-Id: $DELIVERY_ID" \
    -d "$BODY" >/dev/null

# 3. Poll outbox hasta PROCESSED (max 40s)
echo "Polling ai_reply_outbox..."
for i in {1..20}; do
    sleep 2
    STATS=$(curl -sf -H "$AUTH_HEADER" "$BASE_URL/api/v1/ai/outbox/stats")
    PENDING=$(echo "$STATS" | jq '.pending')
    PROCESSING=$(echo "$STATS" | jq '.processing')
    FAILED=$(echo "$STATS" | jq '.failed')
    echo "  [$((i*2))s] pending=$PENDING processing=$PROCESSING failed=$FAILED"
    
    if [[ "$PENDING" -eq 0 && "$PROCESSING" -eq 0 && "$FAILED" -eq 0 ]]; then
        echo "✅ Outbox procesado completamente"
        break
    fi
    
    if [[ $i -eq 20 ]]; then
        echo "❌ Timeout esperando outbox"
        exit 1
    fi
done

# 4. Verificar outbound_message (via channel_event_log)
EVENTS=$(curl -sf -H "$AUTH_HEADER" "$BASE_URL/api/v1/whatsapp-web/status" | jq '.recentEvents // []')
OUTBOUND_COUNT=$(echo "$EVENTS" | jq '[.[] | select(.eventType == "MESSAGE_SENT")] | length')
echo "Mensajes salientes (MESSAGE_SENT) en eventos: $OUTBOUND_COUNT"

if [[ "$OUTBOUND_COUNT" -ge 1 ]]; then
    echo "✅ IA Auto-reply test PASSED"
    exit 0
else
    echo "❌ No se detectó MESSAGE_SENT en eventos"
    exit 1
fi