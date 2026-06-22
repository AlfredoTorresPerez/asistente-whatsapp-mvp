# Adaptador visual whatsapp-web.js

## Resumen

Se reemplazo el adaptador Baileys por `whatsapp-web.js` para permitir una experiencia visual similar a WhatsApp Web.

El servicio sigue llamandose `whatsapp-web-service` para mantener compatibilidad con el backend Java y el frontend React.

## Como se visualiza el navegador

En Docker no se puede abrir una ventana de navegador del host directamente desde el contenedor. Para resolverlo se agrego una capa grafica interna:

```text
Chromium -> Xvfb -> x11vnc -> noVNC -> navegador del usuario
```

URL para abrir la vista visual:

```text
http://localhost:6080/vnc.html?autoconnect=true&resize=scale
```

Desde ahi se ve la sesion de WhatsApp Web y se puede abrir el chat del destinatario.

## Flujo de uso

1. Ejecutar `docker compose up --build`.
2. Abrir `http://localhost:6080/vnc.html?autoconnect=true&resize=scale`.
3. Ejecutar conexion desde el panel o por API: `POST http://localhost:3001/whatsapp/connect`.
4. Escanear el QR con el celular emisor.
5. Enviar mensajes desde el MVP o desde el endpoint `/whatsapp/send-text`.
6. Ver en la ventana visual el chat del destinatario y sus respuestas.

## Variables principales

| Variable | Valor por defecto | Uso |
|---|---|---|
| `WHATSAPP_WEB_HEADLESS` | `false` | Permite que Chromium se ejecute en modo visual. |
| `WHATSAPP_WEB_VISUAL_MODE` | `true` | Inicia Xvfb, x11vnc y noVNC. |
| `WHATSAPP_WEB_BROWSER_VIEWER_URL` | `http://localhost:6080/vnc.html?autoconnect=true&resize=scale` | URL que informa el servicio para abrir el visor. |
| `WHATSAPP_WEB_SESSION_DATA_PATH` | `/app/.wwebjs_auth` | Persistencia de sesion. |
| `WHATSAPP_WEB_CACHE_PATH` | `/app/.wwebjs_cache` | Cache local de WhatsApp Web. |

## Endpoints nuevos o relevantes

```text
GET  /whatsapp/browser
GET  /whatsapp/status
GET  /whatsapp/qr
POST /whatsapp/connect
POST /whatsapp/send-text
```

## Limitaciones

- No es API oficial de Meta.
- Puede requerir reescaneo de QR.
- Puede fallar si WhatsApp cambia su cliente web.
- No debe usarse para mensajeria masiva ni spam.
- Para produccion comercial debe evaluarse WhatsApp Business Platform / Cloud API.
