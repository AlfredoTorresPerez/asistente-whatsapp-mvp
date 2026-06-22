# Cambios: cancelar y reprogramar reservas desde WhatsApp

Se incorporo el flujo conversacional para que el cliente pueda cancelar o reprogramar una reserva existente desde WhatsApp sin que el sistema lo trate como una nueva reserva.

## Cambios principales

- Se corrigio la resolucion contextual de intenciones para preservar `BOOKING_CANCEL` y `BOOKING_CHANGE`.
- Se agregaron manejadores especificos para cancelacion y reprogramacion desde WhatsApp.
- Se agrego busqueda de reservas activas por negocio, cliente, conversacion, telefono, fecha, hora, servicio y sede.
- Se agrego seleccion de reserva cuando hay multiples candidatas.
- Se agrego confirmacion previa antes de cancelar.
- Se agrego reprogramacion con consulta de disponibilidad real antes de guardar.
- Se evito crear reserva temporal cuando la intencion sea cancelar o reprogramar.
- Se aislaron las operaciones transaccionales de agenda para evitar `UnexpectedRollbackException` en el envio del mensaje conversacional.
- Se registra trazabilidad con fuente `WHATSAPP` cuando el actor es el cliente.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentCoordinatorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/IntentDetectorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java`

## Casos cubiertos

- `Quiero cancelar la hora de hoy de las 14:00.`
- `Cancela mi cita.`
- `Quiero reprogramar mi limpieza facial de hoy a las 14:00 para manana a las 16:00.`
- `Quiero cambiar mi hora.`
- Seleccion numerica cuando existen varias reservas activas.

## Validacion realizada

No fue posible compilar el backend porque el envoltorio de Maven intento descargar Maven desde internet y el entorno no tiene acceso a esa descarga.

No fue posible compilar el frontend porque faltan definiciones de tipos locales (`vite/client`, `vitest/globals`, `node`). No se modifico codigo del frontend para este cambio.
