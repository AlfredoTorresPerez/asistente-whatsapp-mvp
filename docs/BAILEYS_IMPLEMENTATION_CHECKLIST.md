# OBSOLETO: reemplazado por whatsapp-web.js visual

Este documento queda como referencia historica. El ZIP actual usa `whatsapp-web.js` con navegador visual por noVNC. Ver `docs/WHATSAPP_WEBJS_VISUAL_ADAPTER.md`.

---

# Checklist de implementacion Baileys

## Resultado

Se reemplazo la implementacion interna del servicio `whatsapp-web-service` por Baileys, manteniendo compatibilidad con backend Java y frontend React.

## Cambios realizados

- [x] Reemplazada dependencia `whatsapp-web.js` por `@whiskeysockets/baileys`.
- [x] Eliminadas dependencias de Puppeteer y Chromium del Dockerfile.
- [x] Agregado uso de `makeWASocket`.
- [x] Agregado uso de `useMultiFileAuthState` para persistencia de sesion.
- [x] Agregado QR en formato Data URL para la pantalla actual.
- [x] Mantenidos endpoints `/whatsapp/*` existentes.
- [x] Agregados endpoints alias `/baileys/*`.
- [x] Conservada firma HMAC hacia backend Java.
- [x] Conservados eventos `SESSION_STATUS_CHANGED`, `QR_UPDATED`, `MESSAGE_RECEIVED` y `MESSAGE_ACK_UPDATED`.
- [x] Actualizado `docker-compose.yml` para volumen `/app/.baileys-session`.
- [x] Actualizado README del servicio.
- [x] Agregado documento tecnico `docs/BAILEYS_ADAPTER.md`.

## Verificaciones locales realizadas

- [x] `node --check whatsapp-web-service/src/server.js`.
- [x] Revision estatica de `docker-compose.yml`.
- [x] Revision de variables de entorno del servicio.

## Verificaciones pendientes en ambiente del usuario

- [ ] `docker compose build --no-cache whatsapp-web-service`.
- [ ] `docker compose up`.
- [ ] Abrir `/admin/whatsapp-web`.
- [ ] Escanear QR.
- [ ] Enviar mensaje de prueba a un numero real.
- [ ] Recibir mensaje entrante y confirmar creacion de conversacion.

## Comandos sugeridos

```bash
docker compose down -v
docker compose build --no-cache whatsapp-web-service
docker compose up
```

Prueba directa:

```bash
curl -i -X POST "http://localhost:3001/whatsapp/send-text" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-whatsapp-web-key" \
  -d '{"businessId":"11111111-1111-1111-1111-111111111111","to":"56950954580","body":"Prueba real desde Baileys"}'
```
