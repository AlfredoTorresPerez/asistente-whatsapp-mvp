# Agenda digital completa WhatsApp multisucursal

## Alcance corregido

La aplicacion queda orientada como **Asistente de Negocios por WhatsApp para centro estetico multisucursal con agenda digital completa**.

No se implementan canales adicionales. Instagram, Facebook, Web chat o telefono quedan solo como evolucion futura no incluida en este MVP.

## Capacidades agregadas

1. Disponibilidad real por sucursal, servicio, profesional y cabina.
2. Horarios comerciales por sucursal.
3. Horarios laborales por profesional.
4. Cabinas compatibles con servicios.
5. Profesionales habilitados para servicios.
6. Bloqueos manuales.
7. Feriados por negocio o sucursal.
8. Reserva temporal con expiracion.
9. Bloqueo de cupo mientras la reserva temporal esta vigente.
10. Enlace publico de confirmacion.
11. Programacion de recordatorios WhatsApp.
12. Historial de estados de reserva.
13. Vista administrativa de agenda completa.
14. Puntos de acceso para disponibilidad, calendario, bloqueos, reprogramacion y cancelacion.

## Flujo coordinado entre agentes

1. El cliente escribe por WhatsApp.
2. El asistente tecnico registra mensaje y telefono.
3. El orquestador detecta intencion y entidades.
4. El agente de negocios valida si corresponde venta, consulta o agenda.
5. El disenador de experiencia define el siguiente mensaje breve.
6. El agente de agenda consulta disponibilidad real.
7. El agente de profesionales y recursos valida profesional y cabina.
8. El orquestador propone horarios.
9. El cliente elige horario.
10. El agente de agenda crea reserva temporal.
11. El agente de confirmacion genera enlace seguro.
12. WhatsApp envia enlace al cliente.
13. El cliente confirma desde pantalla publica.
14. El sistema confirma reserva, registra auditoria y programa recordatorios.
15. Si hay baja confianza, reclamo o error, se deriva a humano.

## Puntos de acceso principales

- `POST /api/v1/agenda/availability`: consulta disponibilidad.
- `POST /api/v1/agenda/temporary-bookings`: crea reserva temporal y opcionalmente enlace.
- `GET /api/v1/agenda/calendar`: consulta calendario operativo.
- `PATCH /api/v1/agenda/bookings/{bookingId}/reschedule`: reprograma con validacion.
- `PATCH /api/v1/agenda/bookings/{bookingId}/cancel`: cancela con motivo.
- `POST /api/v1/agenda/blocks`: crea bloqueo manual.
- `GET /api/v1/public/booking-confirmations/{token}`: consulta enlace publico.
- `POST /api/v1/public/booking-confirmations/{token}/confirm`: confirma reserva.

## Modelo de datos agregado

- `agenda_room`
- `agenda_room_service`
- `agenda_professional_service`
- `agenda_business_hours`
- `agenda_professional_hours`
- `agenda_holiday`
- `agenda_block`
- `booking_status_history`
- `booking_reminder`

Ademas, `booking` se extiende con servicio, profesional, cabina, termino, expiracion temporal, canal de origen, motivo de cancelacion, motivo de reprogramacion y estado de pago.

## Criterios de aceptacion cubiertos

- WhatsApp se mantiene como canal unico.
- La agenda calcula disponibilidad antes de reservar.
- La reserva temporal bloquea cupo.
- La reserva temporal expira y libera cupo.
- La confirmacion por enlace funciona con pantalla publica.
- La agenda considera sucursal, servicio, profesional y cabina.
- Los cambios quedan registrados en historial y auditoria.
- La interfaz incluye pantalla de agenda completa.
