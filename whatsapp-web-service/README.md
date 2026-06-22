# whatsapp-webjs-service

Servicio Node.js que conecta WhatsApp Web mediante `whatsapp-web.js`.

El directorio conserva el nombre `whatsapp-web-service` para no romper el contrato existente con el backend Java, el frontend React ni `docker-compose.yml`.

## Objetivo

Este adaptador permite:

- abrir una sesion real de WhatsApp Web;
- visualizar la ventana del navegador Chromium dentro del contenedor usando noVNC;
- escanear QR desde el panel o desde la ventana visual;
- enviar mensajes desde el numero vinculado hacia el destinatario;
- recibir respuestas y reenviarlas al backend Java como eventos internos.

## Advertencia

Este adaptador usa WhatsApp Web no oficial. Debe usarse solo para demos, validacion temprana y pilotos controlados. Para produccion comercial se recomienda WhatsApp Business Platform / Cloud API.

## Modo visual en Docker

El contenedor inicia:

- Chromium en modo visual mediante Xvfb;
- x11vnc para exponer el escritorio virtual;
- noVNC para verlo desde un navegador web.

URL visual:

```text
http://localhost:6080/vnc.html?autoconnect=true&resize=scale
```

Desde esa pantalla puedes ver WhatsApp Web, abrir el chat del destinatario y observar las respuestas que llegan.

## Endpoints

Todos los endpoints, excepto `/health`, requieren header `X-API-Key`.

- `GET /health`
- `GET /api/v1/session/status`
- `GET /api/v1/session/qr`
- `GET /api/v1/session/browser`
- `POST /api/v1/session/connect`
- `POST /api/v1/session/refresh-qr`
- `POST /api/v1/session/disconnect`
- `POST /api/v1/messages/send`
- `POST /api/v1/messages/simulate-inbound`
- `GET /whatsapp/status`
- `GET /whatsapp/qr`
- `GET /whatsapp/browser`
- `POST /whatsapp/connect`
- `POST /whatsapp/refresh-qr`
- `POST /whatsapp/disconnect`
- `POST /whatsapp/send-text`

## Prueba directa

```bash
curl -i -X POST "http://localhost:3001/whatsapp/send-text" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-whatsapp-web-key" \
  -d '{"businessId":"11111111-1111-1111-1111-111111111111","to":"56950954580","body":"Prueba real desde whatsapp-web.js"}'
```

## Persistencia de sesion

La sesion se guarda en el volumen Docker `whatsapp-webjs-session-data`, montado en `/app/.wwebjs_auth`.

Si necesitas reiniciar completamente la vinculacion QR:

```bash
docker compose down -v
docker compose up --build
```

## Compatibilidad con backend Java

El servicio mantiene los mismos endpoints y cabeceras que usaba el adaptador anterior:

- `X-API-Key` para llamadas backend -> adaptador.
- `X-WhatsApp-Web-Timestamp` para eventos adaptador -> backend.
- `X-WhatsApp-Web-Signature` con HMAC SHA-256.
- `X-WhatsApp-Web-Delivery-Id` como identificador de entrega.

Eventos emitidos hacia backend:

- `SESSION_STATUS_CHANGED`
- `QR_UPDATED`
- `MESSAGE_RECEIVED`
- `MESSAGE_ACK_UPDATED`

## Limpieza de perfil Chromium en ambiente local

El contenedor limpia bloqueos conocidos antes de lanzar `whatsapp-web.js`:

```text
WHATSAPP_WEB_CLEAN_PROFILE_LOCKS_ON_START=true
WHATSAPP_WEB_KILL_ORPHAN_CHROMIUM_ON_START=true
```

Esto mitiga errores de perfil ocupado por otro proceso de Chromium. Si el volumen persiste corrupto, eliminar los volumenes `whatsapp-webjs-session-data` y `whatsapp-webjs-cache-data` y volver a levantar el ambiente local.
