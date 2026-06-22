# Correccion ejecutada: MVP WhatsApp monocanal y centro estetico multisucursal

## Alcance corregido

La aplicacion queda alineada como **Asistente de Negocios por WhatsApp para centro estetico multisucursal**. No debe presentarse como multicanal ni omnicanal mientras el unico canal operativo real sea WhatsApp.

## Correcciones incluidas

1. Se agrega modelo de enlace de confirmacion de reserva.
2. Se agrega token seguro con hash SHA-256 en base de datos.
3. Se agrega expiracion de enlace y liberacion de cupo.
4. Se agregan estados de reserva: `TEMPORARY`, `EXPIRED`, `RELEASED`, `NO_SHOW`, `ATTENDED`.
5. Se agrega endpoint publico para consultar y confirmar reserva.
6. Se agrega pantalla publica React para confirmar reserva desde enlace.
7. Se agrega validacion de disponibilidad para evitar doble reserva por sucursal.
8. Se agrega tarea programada para expirar enlaces vencidos.
9. Se agrega boton administrativo para generar enlace de confirmacion.
10. Se corrige la semantica del producto hacia WhatsApp monocanal multisucursal.

## Flujo objetivo

1. Cliente escribe por WhatsApp.
2. Orquestador detecta intencion.
3. Agenda propone servicio, sucursal, fecha y hora.
4. Sistema crea reserva temporal.
5. Sistema genera enlace unico de confirmacion.
6. Cliente confirma desde pantalla publica.
7. Reserva pasa a `CONFIRMED`.
8. Si el enlace expira, la reserva pasa a `RELEASED` y el cupo queda disponible.

## Endpoints agregados

- `POST /api/v1/bookings/{bookingId}/confirmation-link`
- `GET /api/v1/public/booking-confirmations/{token}`
- `POST /api/v1/public/booking-confirmations/{token}/confirm`

## Ruta publica frontend

- `/reservas/confirmar/:token`

## Pendientes recomendados

1. Agregar profesional y cabina en la entidad `booking` para disponibilidad fina.
2. Integrar envio automatico del enlace por WhatsApp segun proveedor activo.
3. Agregar mensajes de recordatorio antes de la cita.
4. Agregar metricas de conversion por sucursal.
5. Agregar pruebas end-to-end de confirmacion desde enlace.
