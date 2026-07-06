# Cambios v23.4.11 - Enlace navegable por WhatsApp

## Objetivo

Hacer que el enlace de confirmacion enviado por WhatsApp sea navegable desde el telefono del cliente y no dependa de `localhost`.

## Cambios aplicados

1. `frontend-react/nginx.conf`
   - Se agrego proxy de `/api/v1/` hacia `backend-java:8080/api/v1/`.
   - Permite que el frontend publico y la API usen el mismo origen.

2. `frontend-react/Dockerfile`
   - Se cambio el valor por defecto de `VITE_API_BASE_URL` a `/api/v1`.

3. `docker-compose.local.yml`
   - Se agregaron variables `APP_BOOKING_CONFIRMATION_*` al backend.
   - Se agrego el servicio opcional `public-tunnel` con perfil `public-link`.
   - Se cambio `VITE_API_BASE_URL` a `/api/v1`.

4. `docker-compose.yml` y `docker-compose.prod.yml`
   - Se alinearon variables de confirmacion y API relativa.

5. `scripts/start_mvp_public_link.sh`
   - Nuevo script para levantar el MVP, crear tunel publico HTTPS, guardar la URL y reiniciar backend.

6. `docs/ENLACE_NAVEGABLE_WHATSAPP_V23_4_11.md`
   - Nueva guia tecnica de ejecucion y validacion.

## Resultado esperado

La plantilla de WhatsApp debe contener un enlace de este tipo:

```text
https://xxxx.trycloudflare.com/reservas/confirmar/<token>
```

Ese enlace abre desde el telefono del cliente y permite confirmar la reserva desde la pagina publica.

## Correccion Windows PowerShell

Se agrega `scripts/start_mvp_public_link.ps1` para ejecutar el flujo de enlace navegable desde PowerShell sin usar `chmod`.

Comando:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\start_mvp_public_link.ps1
```
