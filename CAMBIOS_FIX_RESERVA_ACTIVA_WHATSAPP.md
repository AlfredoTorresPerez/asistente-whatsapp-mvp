# Correccion de busqueda de reserva activa por WhatsApp

## Problema corregido

Al solicitar cancelar o reprogramar una reserva desde la conversacion de WhatsApp, el asistente podia responder que no encontraba una reserva activa asociada al numero, aunque la reserva estuviera visible en agenda.

## Causa tecnica

La busqueda dependia demasiado de la asociacion exacta entre conversacion, cliente y telefono normalizado. Si la reserva estaba creada con otro `customer_id`, otro `conversation_id` o un telefono con formato distinto, el flujo no encontraba la cita aunque coincidieran servicio, fecha, hora y sede.

## Cambios aplicados

- Se amplio la busqueda por contexto de cliente para usar tambien los ultimos 4 digitos contra el nombre visible del cliente.
- Se agrego una busqueda operativa alternativa por fecha, hora, servicio y sede aunque no exista enlace exacto con la conversacion.
- Se agrego busqueda por nombre de cliente y ultimos digitos de telefono como respaldo.
- Se amplio la ventana horaria interna para evitar fallos por zona horaria.
- Se agrego soporte para fechas escritas como `12 de junio`.
- Se mejoro la trazabilidad del flujo con capas de busqueda diferenciadas.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`

## Validacion

No fue posible compilar con Maven dentro del entorno porque el envoltorio del proyecto intenta descargar Maven desde internet y la descarga esta bloqueada.
