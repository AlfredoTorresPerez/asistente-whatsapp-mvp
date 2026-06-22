# Cambios v23.4.14 - Confirmacion publica de reserva

## Objetivo

Corregir el flujo de confirmacion publica de reservas cuando el enlace abre correctamente la pantalla publica, pero el boton **Confirmar reserva** recibe `403 Forbidden`.

## Correcciones

1. Se agrego una cadena de seguridad dedicada para rutas publicas:
   - `/api/v1/public/booking-confirmations/**`
   - webhooks publicos
   - health checks

2. La cadena publica no ejecuta el filtro JWT y permite llamadas publicas GET y POST.

3. Se agrego sanitizacion defensiva de `APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL` para evitar enlaces contaminados por variables `.env` concatenadas.

4. Se corrigieron los scripts de tunel publico para reescribir `.env` con lineas limpias.

5. Se agregaron scripts de desarrollo para liberar reservas temporales de prueba:
   - `scripts/dev_release_temporary_bookings.ps1`
   - `scripts/dev_release_temporary_bookings.sh`

## Validacion esperada

El enlace debe quedar asi:

```text
https://<tunel>.trycloudflare.com/reservas/confirmar/<token>
```

El boton de confirmacion debe llamar:

```text
POST /api/v1/public/booking-confirmations/<token>/confirm
```

Sin exigir inicio de sesion.

## Nota para pruebas locales

Si se configuran 12 horas de expiracion, cada reserva temporal bloquea el cupo durante 720 minutos. Para pruebas repetidas, liberar reservas temporales antes de volver a usar el mismo horario.
