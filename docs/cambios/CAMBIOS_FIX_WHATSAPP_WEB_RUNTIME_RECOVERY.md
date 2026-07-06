# Fix: estabilidad del adaptador WhatsApp Web

## Problema detectado

El contenedor `whatsapp-web-service` podía finalizar con `exit code 1` cuando Chromium/whatsapp-web.js emitía un error transitorio de Puppeteer:

```text
Execution context was destroyed, most likely because of a navigation.
```

Este error ocurre cuando WhatsApp Web navega, recarga o reemplaza el contexto de ejecución mientras `whatsapp-web.js` intenta inyectar scripts internos. En Node.js 24 el rechazo no controlado puede terminar el proceso.

## Cambios aplicados

1. Se agregó manejo global de errores de ejecución:
   - `process.on("unhandledRejection", ...)`
   - `process.on("uncaughtException", ...)`

2. Se agregó detección de errores recuperables de Chromium/Puppeteer:
   - `Execution context was destroyed`
   - `Target closed`
   - `Session closed`
   - `Protocol error`
   - `Browser has disconnected`
   - `Page crashed`

3. Se agregó recuperación automática del cliente WhatsApp Web:
   - destruye la instancia actual si quedó inconsistente;
   - limpia locks de Chromium;
   - limpia `/tmp/whatsapp-web-profile`;
   - vuelve a inicializar el cliente tras `WHATSAPP_WEB_RUNTIME_RECOVERY_DELAY_MS`.

4. Se protegieron handlers asíncronos de eventos:
   - `qr`
   - `message`
   - `message_create`
   - `message_ack`

5. Se cambió la imagen base del adaptador de Node.js 24 a Node.js 22 LTS para reducir incompatibilidades con librerías de navegador.

6. Se agregó `restart: unless-stopped` al servicio `whatsapp-web-service` como segunda línea de defensa.

## Archivos modificados

- `whatsapp-web-service/src/server.js`
- `whatsapp-web-service/Dockerfile`
- `docker-compose.local.yml`

## Validación local realizada

Se validó sintaxis JavaScript con:

```bash
node --check whatsapp-web-service/src/server.js
```

Resultado: sin errores de sintaxis.
