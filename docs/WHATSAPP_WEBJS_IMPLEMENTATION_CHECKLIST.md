# Checklist implementacion whatsapp-web.js visual

## Servicio Node.js

- [x] Reemplazada dependencia Baileys por `whatsapp-web.js`.
- [x] Agregado `LocalAuth` para persistencia de sesion.
- [x] Agregado soporte de QR como imagen base64.
- [x] Agregado envio real mediante `client.sendMessage`.
- [x] Agregada recepcion real mediante evento `message`.
- [x] Agregado mapeo de confirmaciones mediante evento `message_ack`.
- [x] Conservados endpoints compatibles con backend Java.

## Navegador visual

- [x] Instalado Chromium en la imagen Docker.
- [x] Agregado Xvfb para escritorio virtual.
- [x] Agregado x11vnc para compartir escritorio virtual.
- [x] Agregado noVNC para verlo desde navegador web.
- [x] Expuesto puerto `6080` para visualizacion.
- [x] Agregado endpoint `/whatsapp/browser`.

## Docker Compose

- [x] Expuesto `3001` para API del adaptador.
- [x] Expuesto `6080` para visor visual.
- [x] Agregado `shm_size: 1gb` para estabilidad de Chromium.
- [x] Agregados volumenes `whatsapp-webjs-session-data` y `whatsapp-webjs-cache-data`.

## Verificacion sugerida

```bash
node --check whatsapp-web-service/src/server.js
docker compose config --quiet
docker compose build --no-cache whatsapp-web-service
docker compose up
```

## URL visual

```text
http://localhost:6080/vnc.html?autoconnect=true&resize=scale
```
