# Enlace navegable para confirmacion de reserva por WhatsApp

> **ESTADO: HISTÓRICO** (v23.4.11). Los scripts `start_mvp_public_link.*` fueron eliminados; las utilidades vigentes son `scripts/start-public-link.ps1`, `scripts/stop-public-link.ps1` y `scripts/check-public-link.ps1`. Ver `docs/USO_WINDOWS_ENLACE_NAVEGABLE.md`.

## Problema corregido

Un enlace con `localhost` no es navegable desde el telefono del cliente. En un telefono, `localhost` apunta al propio dispositivo del cliente, no al computador donde corre el MVP.

## Solucion implementada

Se agrego soporte para usar una URL publica HTTPS hacia el frontend y para que el frontend proxy pase las llamadas `/api/v1` hacia el backend.

Con esto, el enlace enviado al cliente queda con este formato:

```text
https://<url-publica>/reservas/confirmar/<token>
```

Y la pagina publica de confirmacion puede consultar y confirmar la reserva usando:

```text
https://<url-publica>/api/v1/public/booking-confirmations/<token>
```

## Ejecucion recomendada en local

```bash
./scripts/start_mvp_public_link.sh
```

El script realiza estos pasos:

1. Levanta el MVP local con Docker Compose.
2. Levanta un tunel publico HTTPS usando Cloudflare Tunnel.
3. Obtiene la URL publica generada.
4. Escribe `APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL` en `.env`.
5. Reinicia el backend para que los nuevos enlaces usen la URL publica.

## Validacion

Verifica la variable activa en el backend:

```bash
docker compose -f docker-compose.local.yml exec backend-java printenv | grep APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL
```

Debe mostrar algo como:

```text
APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL=https://xxxx.trycloudflare.com/reservas/confirmar
```

## Nota productiva

Para produccion no se recomienda usar un tunel temporal. Debe configurarse un dominio real, por ejemplo:

```env
APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL=https://agenda.tu-dominio.cl/reservas/confirmar
```
