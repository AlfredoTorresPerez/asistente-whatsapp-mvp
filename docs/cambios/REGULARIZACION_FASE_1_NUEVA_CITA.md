# Regularizacion Fase 1 - Nueva cita

Fecha: 2026-08-03

## Alcance implementado

- La pantalla `Nueva cita` queda en un flujo guiado de cuatro pasos: cliente, servicio y sucursal, fecha y hora, resumen.
- La creacion ya no usa el endpoint generico de citas; ahora usa la creacion transaccional de agenda.
- La cita creada persiste sucursal, servicio, profesional, cabina, fecha de inicio, fecha de termino, duracion, origen y relacion de cliente.
- El servidor vuelve a validar disponibilidad dentro de la operacion transaccional de agenda.
- La UI vuelve a consultar disponibilidad cuando cambia sucursal, servicio, profesional, cabina o fecha.
- Si el horario seleccionado deja de estar disponible, la seleccion se limpia y se muestra un mensaje comprensible.
- El buscador de clientes acepta telefono, nombre y correo electronico.
- El telefono se normaliza a E.164 para Chile antes de crear o reutilizar cliente.
- La creacion evita duplicados por telefono al reutilizar clientes existentes encontrados con formatos equivalentes.
- El flujo permite registrar autorizacion de comunicaciones y controla el envio de confirmacion por WhatsApp.
- El resumen muestra cliente, contacto, sucursal, servicio, profesional, cabina, fecha, hora de inicio, hora de termino, duracion, precio, abono, saldo, politica de cancelacion, origen y notificaciones.

## Controles de servidor utilizados

- `CompleteDigitalAgendaService.createTemporaryBooking` conserva la validacion final antes de insertar.
- `CompleteAgendaJdbcRepository.insertTemporaryBooking` conserva bloqueos transaccionales de capacidad de sucursal, profesional y cabina.
- La idempotencia acepta hashes antiguos de telefono y graba nuevos intentos con telefono normalizado.
- Las llamadas existentes mantienen compatibilidad mediante sobrecargas.

## Validacion ejecutada

- Backend: `.\mvnw.cmd -DskipTests compile`
- Frontend: `pnpm build`
- Backend focalizado: `.\mvnw.cmd "-Dtest=CompleteDigitalAgendaServiceTest,CompleteAgendaJdbcRepositoryTest,CustomerSearchServiceIntegrationTest,PhoneUtilsTest" test`

## Pendientes para fases siguientes

- Exponer monto estructurado de abono en el servicio administrativo o en la respuesta de disponibilidad.
- Centralizar traducciones visibles de estado de cita y pago.
- Completar cancelacion, reprogramacion, historial y notificaciones en agenda y citas.
- Revisar restricciones PostgreSQL adicionales para solapamientos cuando el negocio requiera exclusion por combinacion especifica de profesional y cabina.
