#!/bin/bash
# test-webhook-local.sh - Test manual del webhook WhatsApp Web local
# Uso: ./scripts/test-webhook-local.sh

set -euo pipefail

# Configuración
WEBHOOK_URL="http://localhost:8080/api/v1/integrations/whatsapp-web/webhook"
WEBHOOK_SECRET="${WHATSAPP_WEB_WEBHOOK_SECRET:-dev-whatsapp-web-webhook-secret}"
SESSION_KEY="${WHATSAPP_WEB_SESSION_ID:-demo-sales}"
DELIVERY_ID="$(uuidgen)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)"

# Payload de prueba (MESSAGE_RECEIVED)
read -r -d '' PAYLOAD <<EOF
{
  "eventType": "MESSAGE_RECEIVED",
  "deliveryId": "${DELIVERY_ID}",
  "occurredAt": "${TIMESTAMP}",
  "sessionKey": "${SESSION_KEY}",
  "payload": {
    "from": "56950954580",
    "to": "56927305158",
    "body": "Hola, quiero agendar una hora",
    "externalMessageId": "test-msg-$(date +%s)",
    "chatId": "56950954580@c.us",
    "messageType": "text",
    "hasMedia": false
  }
}
EOF

# Calcular firma HMAC-SHA256
SIGNATURE="sha256=$(printf '%s' "${TIMESTAMP}.${PAYLOAD}" | openssl dgst -sha256 -hmac "${WEBHOOK_SECRET}" -binary | xxd -p -c 256)"

echo "=== Test Webhook WhatsApp Web Local ==="
echo "URL: ${WEBHOOK_URL}"
echo "Delivery-ID: ${DELIVERY_ID}"
echo "Timestamp: ${TIMESTAMP}"
echo ""

# Enviar request
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${WEBHOOK_URL}" \
  -H "Content-Type: application/json" \
  -H "X-WhatsApp-Web-Timestamp: ${TIMESTAMP}" \
  -H "X-WhatsApp-Web-Signature: ${SIGNATURE}" \
  -H "X-WhatsApp-Web-Delivery-Id: ${DELIVERY_ID}" \
  -d "${PAYLOAD}")

HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
BODY=$(echo "${RESPONSE}" | head -n -1)

echo "HTTP Status: ${HTTP_CODE}"
echo "Response: ${BODY}"
echo ""

if [[ "${HTTP_CODE}" == "200" ]]; then
    echo "✅ Webhook procesado correctamente"
else
    echo "❌ Error en webhook (HTTP ${HTTP_CODE})"
    exit 1
fi

echo ""
echo "=== Verificación en BD ==="
echo "Ejecuta estos comandos para verificar:"
echo ""
echo "# Eventos de canal recibidos:"
echo "docker compose -f docker-compose.local.yml exec postgres psql -U assistant -d asistente_whatsapp -c \"SELECT delivery_id, event_type, processing_status, received_at FROM channel_event_log ORDER BY received_at DESC LIMIT 5;\""
echo ""
echo "# Mensajes en outbox IA:"
echo "docker compose -f docker-compose.local.yml exec postgres psql -U assistant -d asistente_whatsapp -c \"SELECT id, conversation_id, status, attempts, created_at FROM ai_reply_outbox ORDER BY created_at DESC LIMIT 5;\""
echo ""
echo "# Conversaciones creadas:"
echo "docker compose -f docker-compose.local.yml exec postgres psql -U assistant -d asistente_whatsapp -c \"SELECT id, customer_id, status, unread_count, last_message_at FROM conversation ORDER BY created_at DESC LIMIT 5;\""
echo ""
echo "# Mensajes inbound:"
echo "docker compose -f docker-compose.local.yml exec postgres psql -U assistant -d asistente_whatsapp -c \"SELECT id, conversation_id, direction, body, status, received_at FROM message WHERE direction = 'INBOUND' ORDER BY received_at DESC LIMIT 5;\""