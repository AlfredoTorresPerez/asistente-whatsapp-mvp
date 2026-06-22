# Cambios: cancelacion y reprogramacion desde enlace publico de cliente

## Alcance implementado

Se agrego soporte para que el cliente pueda gestionar su reserva desde la misma pagina publica de confirmacion `/reservas/confirmar/:token`.

## Frontend

Archivo principal modificado:

- `frontend-react/src/modules/bookings/pages/BookingConfirmationPage.tsx`

Cambios aplicados:

- Se agrego boton `Reprogramar reserva`.
- Se agrego boton `Cancelar reserva`.
- Se agrego formulario publico de cancelacion con motivo obligatorio.
- Se agrego selector de fecha para reprogramacion.
- Se agrego consulta de horarios disponibles usando el token publico.
- Se agrego seleccion de horario disponible para confirmar nueva fecha.
- Se agregaron mensajes de estado para reserva confirmada, cancelada, reprogramada y expirada.
- Se bloqueo cancelacion y reprogramacion si faltan menos de 24 horas.

Archivos de API modificados:

- `frontend-react/src/services/api/bookingsApi.ts`
- `frontend-react/src/services/api/types.ts`

Nuevas funciones de cliente publico:

- `getPublicBookingConfirmationAvailabilityRequest`
- `reschedulePublicBookingFromConfirmationRequest`
- `cancelPublicBookingFromConfirmationRequest`

## Backend

Controlador modificado:

- `backend-java/src/main/java/com/asistentewhatsapp/bookings/api/PublicBookingConfirmationController.java`

Nuevos endpoints publicos basados en el token de confirmacion:

- `GET /api/v1/public/booking-confirmations/{token}/availability`
- `POST /api/v1/public/booking-confirmations/{token}/reschedule`
- `POST /api/v1/public/booking-confirmations/{token}/cancel`

Servicio modificado:

- `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingConfirmationService.java`

Cambios aplicados:

- Consulta publica de disponibilidad manteniendo servicio y sucursal de la reserva original.
- Reprogramacion publica con validacion de disponibilidad real.
- Cancelacion publica con motivo obligatorio.
- Validacion de enlace expirado o invalidado.
- Validacion de estados finales no modificables.
- Validacion de ventana minima de 24 horas.
- Registro de auditoria para acciones realizadas por cliente.
- Recalculo de recordatorios al reprogramar.

Repositorio modificado:

- `backend-java/src/main/java/com/asistentewhatsapp/bookings/infrastructure/BookingConfirmationJdbcRepository.java`

Cambios aplicados:

- Se agregaron identificadores de servicio, profesional y cabina al detalle recuperado por token publico.

Archivo nuevo:

- `backend-java/src/main/java/com/asistentewhatsapp/bookings/api/PublicBookingRescheduleRequest.java`

## Validacion realizada

- `npm run build` ejecutado correctamente en `frontend-react`.
- La validacion del backend con Maven no pudo ejecutarse porque el wrapper intento descargar Maven desde internet y el entorno no permitio esa descarga.
