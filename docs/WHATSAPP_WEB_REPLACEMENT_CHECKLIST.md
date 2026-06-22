# OBSOLETO: reemplazado por adaptador visual whatsapp-web.js

Este documento queda como referencia historica. El ZIP actual usa `whatsapp-web.js` con Chromium visual por noVNC. Ver `docs/WHATSAPP_WEBJS_VISUAL_ADAPTER.md`.

---

# Checklist de reemplazo del adaptador WhatsApp Web

## Objetivo aplicado

El servicio `whatsapp-web-service` conserva el contrato HTTP usado por backend Java y frontend React, pero su implementacion interna actual ahora usa Baileys.

## Evolucion historica

1. Adaptador inicial experimental.
2. Reemplazo anterior basado en `whatsapp-web.js`.
3. Reemplazo actual basado en Baileys.

## Cambios principales actuales

- Servicio externo conservado: `whatsapp-web-service`.
- Implementacion interna actual: Baileys.
- Dependencia principal actual: `@whiskeysockets/baileys`.
- Dependencias eliminadas: `whatsapp-web.js`, Puppeteer y Chromium.
- Generacion de QR mediante `qrcode`.
- Autenticacion persistente mediante `useMultiFileAuthState`.
- Sesion persistida en `/app/.baileys-session`.
- Volumen Docker actual: `whatsapp-baileys-session-data`.

## Endpoints internos conservados para backend

- `GET /api/v1/session/status`
- `GET /api/v1/session/qr`
- `POST /api/v1/session/connect`
- `POST /api/v1/session/refresh-qr`
- `POST /api/v1/session/disconnect`
- `POST /api/v1/messages/send`
- `POST /api/v1/messages/simulate-inbound`

## Endpoints directos conservados

- `GET /whatsapp/status`
- `GET /whatsapp/qr`
- `POST /whatsapp/connect`
- `POST /whatsapp/refresh-qr`
- `POST /whatsapp/disconnect`
- `POST /whatsapp/send-text`

## Endpoints directos nuevos

- `GET /baileys/status`
- `GET /baileys/qr`
- `POST /baileys/connect`
- `POST /baileys/refresh-qr`
- `POST /baileys/disconnect`
- `POST /baileys/send-text`

## Backend Java

- Configuracion conservada en `app.channels.whatsapp-web`.
- Variables conservadas como `APP_WHATSAPP_WEB_*`.
- Controlador administrativo conservado en `/api/v1/whatsapp-web/*`.
- Webhook interno conservado en `/api/v1/integrations/whatsapp-web/webhook`.
- Encabezados de webhook conservados:
  - `X-WhatsApp-Web-Timestamp`
  - `X-WhatsApp-Web-Signature`
  - `X-WhatsApp-Web-Delivery-Id`

## Frontend React

- Ruta administrativa conservada: `/admin/whatsapp-web`.
- Pantalla conservada: `WhatsAppWebConnectionPage`.
- Etiqueta visual conservada: `WhatsApp Web`, porque Baileys tambien opera contra WhatsApp Web.

## Docker Compose

- Servicio: `whatsapp-web-service`.
- Contenedor: `asistente-whatsapp-baileys`.
- Volumen: `whatsapp-baileys-session-data`.
- Puerto expuesto: `3001`.
- Variables compatibles: `WHATSAPP_WEB_*`.
- Variables opcionales nuevas: `BAILEYS_*`.

## Validaciones realizadas

- `node --check whatsapp-web-service/src/server.js`: correcto.
- `docker-compose.yml`: parseo YAML correcto.
- `package.json` del servicio validado visualmente.

## Validaciones pendientes en ambiente local

Ejecutar con internet para descargar dependencias:

```bash
docker compose down -v
docker compose build --no-cache whatsapp-web-service
docker compose up
```

Validar frontend y backend:

```bash
corepack pnpm --dir frontend-react install
corepack pnpm --dir frontend-react build
./backend-java/mvnw test
```

## Nota funcional

Baileys usa WhatsApp Web no oficial. Debe tratarse como adaptador experimental para demo, pilotos y validacion temprana. Para produccion comercial estable se recomienda evaluar la plataforma oficial de WhatsApp Business.
