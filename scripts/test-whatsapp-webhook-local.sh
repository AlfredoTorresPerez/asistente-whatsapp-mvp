#!/usr/bin/env bash
# test-whatsapp-webhook-local.sh
# Test rápido webhook: HMAC válido -> 200 -> channel_event_log insertado

set -euo pipefail

TOKEN="${1:-}"
[[ -n "$TOKEN" ]] || { echo "Uso: $0 <BEARER_TOKEN>"; exit 1; }

BASE_URL="http://localhost:8080"
WEBHOOK_SECRET="${APP_WHATSAPP_WEB_WEBHOOK_SECRET:-dev-whatsapp-web-webhook-secret}"
AUTH_HEADER="Authorization: Bearer $TOKEN"

# HMAC helper
hmac_sha256() {
    local key="$1" msg="$2"
    echo -n "$msg" | openssl dgst -sha256 -hmac "$key" -binary | xxd -p -c 256 | tr -d '\n'
}

echo "=== Test Webhook WhatsApp Web ==="

# Payload
PHONE="56950954580"
DELIVERY_ID=$(uuidgen)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")
SESSION_KEY="demo-sales"

PAYLOAD=$(cat <<EOF | jq -c .
{
  "eventType": "MESSAGE_RECEIVED",
  "deliveryId": "$DELIVERY_ID",
  "occurredAt": "$TIMESTAMP",
  "sessionKey": "$SESSION_KEY",
  "payload": {
    "from": "$PHONE",
    "body": "Test webhook local $(date -u +%H:%M:%S)",
    "to": "56900000000",
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

echo "Enviando webhook..."
RESP=$(curl -sf -X POST "$BASE_URL/api/v1/integrations/whatsapp-web/webhook" \
    -H "Content-Type: application/json" \
    -H "X-WhatsApp-Web-Timestamp: $TIMESTAMP" \
    -H "X-WhatsApp-Web-Signature: $SIG" \
    -H "X-WhatsApp-Web-Delivery-Id: $DELIVERY_ID" \
    -d "$BODY")

echo "Response: $RESP"

# Verificar channel_event_log
sleep 1
EVENTS=$(curl -sf -H "$AUTH_HEADER" "$BASE_URL/api/v1/whatsapp-web/status" | jq '.recentEvents // []')
COUNT=$(echo "$EVENTS" | jq 'length')
echo "Eventos en channel_event_log: $COUNT"

# Buscar nuestro deliveryId
FOUND=$(echo "$EVENTS" | jq --arg id "$DELIVERY_ID" '.[] | select(.deliveryId == $id) | .processingStatus')
if [[ "$FOUND" == '"PROCESSED"' ]]; then
    echo "✅ Webhook test PASSED"
    exit 0
else
    echo "❌ Evento no encontrado o no PROCESSED: $FOUND"
    exit 1
fi