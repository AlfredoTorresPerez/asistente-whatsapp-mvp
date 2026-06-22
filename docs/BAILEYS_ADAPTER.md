# OBSOLETO: reemplazado por whatsapp-web.js visual

Este documento queda como referencia historica. El ZIP actual usa `whatsapp-web.js` con navegador visual por noVNC. Ver `docs/WHATSAPP_WEBJS_VISUAL_ADAPTER.md`.

---

# Adaptador Baileys para WhatsApp Web

## Objetivo

Reemplazar el adaptador anterior basado en `whatsapp-web.js` por un servicio Node.js basado en Baileys, conservando el contrato actual del backend Java.

## Decision arquitectonica

Se mantiene el directorio `whatsapp-web-service` y los endpoints existentes porque el backend Java ya consume ese contrato mediante `WhatsAppWebSessionGatewayClient` y `WhatsAppWebAdapter`.

El cambio queda encapsulado en el servicio Node.js:

```text
Backend Java
  -> MessagingChannel
  -> WhatsAppWebAdapter
  -> HTTP interno
  -> whatsapp-web-service
  -> Baileys
  -> WhatsApp Web
```

## Cambios aplicados

- `package.json` reemplaza `whatsapp-web.js` por `@whiskeysockets/baileys`.
- `Dockerfile` elimina Chromium, Puppeteer y dependencias graficas.
- `server.js` usa `makeWASocket`, `useMultiFileAuthState` y eventos de Baileys.
- `docker-compose.yml` conserva el servicio `whatsapp-web-service`, pero el contenedor ahora se llama `asistente-whatsapp-baileys`.
- La sesion se guarda en `/app/.baileys-session`.
- El volumen ahora es `whatsapp-baileys-session-data`.

## Endpoints conservados

- `GET /health`
- `GET /api/v1/session/status`
- `GET /api/v1/session/qr`
- `POST /api/v1/session/connect`
- `POST /api/v1/session/refresh-qr`
- `POST /api/v1/session/disconnect`
- `POST /api/v1/messages/send`
- `POST /api/v1/messages/simulate-inbound`
- `GET /whatsapp/status`
- `GET /whatsapp/qr`
- `POST /whatsapp/connect`
- `POST /whatsapp/refresh-qr`
- `POST /whatsapp/disconnect`
- `POST /whatsapp/send-text`

## Endpoints nuevos de alias Baileys

- `GET /baileys/status`
- `GET /baileys/qr`
- `POST /baileys/connect`
- `POST /baileys/reconnect`
- `POST /baileys/refresh-qr`
- `POST /baileys/disconnect`
- `POST /baileys/send-text`
- `POST /baileys/simulate-inbound`

## Eventos hacia backend

El adaptador sigue enviando eventos firmados al backend mediante el mismo webhook:

```text
POST /api/v1/integrations/whatsapp-web/webhook
```

Cabeceras:

- `X-WhatsApp-Web-Timestamp`
- `X-WhatsApp-Web-Signature`
- `X-WhatsApp-Web-Delivery-Id`

Eventos:

- `SESSION_STATUS_CHANGED`
- `QR_UPDATED`
- `MESSAGE_RECEIVED`
- `MESSAGE_ACK_UPDATED`

## Estados de sesion

- `DISCONNECTED`: sesion no iniciada.
- `SYNCING`: Baileys esta conectando.
- `QR_PENDING`: QR disponible para escaneo.
- `CONNECTED`: sesion lista para enviar y recibir.
- `ERROR`: error de conexion o envio.

## Envio de mensajes

Solicitud compatible:

```json
{
  "businessId": "11111111-1111-1111-1111-111111111111",
  "to": "56950954580",
  "body": "Prueba real desde Baileys"
}
```

Respuesta esperada:

```json
{
  "messageId": "BAE...",
  "status": "SENT",
  "acceptedAt": "2026-05-30T00:00:00.000Z",
  "chatId": "56950954580@s.whatsapp.net",
  "adapterMode": "EXPERIMENTAL_REAL_BAILEYS"
}
```

## Consideraciones

- Este adaptador no es oficial de Meta.
- Debe usarse en demo, validacion temprana o piloto controlado.
- Para produccion comercial, usar WhatsApp Business Platform / Cloud API.
- No usar para spam, envios masivos no solicitados ni automatizacion abusiva.
