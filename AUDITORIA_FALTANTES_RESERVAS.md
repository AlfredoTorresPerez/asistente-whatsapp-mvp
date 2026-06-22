# AUDITORIA_FALTANTES_RESERVAS

Fecha de auditoria: 2026-06-20  
Alcance: aplicacion de reservas por WhatsApp, enlaces publicos, agenda, reprogramacion, cancelacion, pagos, auditoria, frontend, backend, base de datos y pruebas.

## 1. Resumen ejecutivo

La aplicacion no es un cascaron: existe una implementacion amplia con backend Java/Spring Boot, frontend React/Vite, servicio Node para WhatsApp Web, migraciones Flyway sobre PostgreSQL, adaptadores WhatsApp Web/Cloud, agenda digital, enlaces publicos de confirmacion/reprogramacion/cancelacion, recordatorios y auditoria basica.

El estado actual es funcionalmente avanzado, pero no alcanza aun un nivel robusto de produccion para reservas transaccionales. Las brechas mas importantes estan en la proteccion contra doble reserva, normalizacion de estados, reglas de transicion, pagos asociados a reserva, auditoria enriquecida y pruebas concurrentes.

Riesgo principal: la disponibilidad se valida por consultas de aplicacion, no por una restriccion fuerte de base de datos. Hay `FOR UPDATE` sobre enlaces y reservas en algunos flujos publicos, pero no existe `EXCLUDE`/`tstzrange`/GiST ni versionado optimista que impida solapes bajo concurrencia real. Esto deja abierta la posibilidad de doble booking cuando dos procesos crean, confirman o reprograman slots cercanos al mismo tiempo.

## 2. Alcance del proyecto detectado

Stack detectado:

- Backend: Java 21, Spring Boot 3.5.14, Spring Security, Spring MVC, Spring JDBC/JPA, Flyway, PostgreSQL.
- Frontend: React 18, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod.
- WhatsApp: servicio Node/Express con `whatsapp-web.js`, firma HMAC hacia backend, modo WhatsApp Web experimental; backend tambien contiene adaptador Cloud API configurable.
- Persistencia: PostgreSQL con migraciones `V1` a `V32`.
- Infra: `docker-compose.yml` con PostgreSQL, backend, frontend y servicio WhatsApp Web.
- Pruebas: JUnit/Spring backend, Vitest frontend configurado, pruebas de canales/IA/repositorios.

Modulos relevantes:

- Agenda completa: `backend-java/src/main/java/com/asistentewhatsapp/agenda`.
- Reservas y enlaces publicos: `backend-java/src/main/java/com/asistentewhatsapp/bookings`.
- IA transaccional WhatsApp: `backend-java/src/main/java/com/asistentewhatsapp/aiagents`.
- Canales WhatsApp: `backend-java/src/main/java/com/asistentewhatsapp/channels`.
- Frontend de agenda/reservas publicas: `frontend-react/src/modules/agenda`, `frontend-react/src/modules/bookings`.
- Servicio WhatsApp Web: `whatsapp-web-service/src/server.js`.

Evidencia de verificacion:

- `backend-java`: `mvn test` exitoso, 279 tests, 0 fallos.
- `frontend-react`: no se ejecuto `pnpm test` porque no existe `node_modules` en el entorno local.
- `whatsapp-web-service`: `node --check src/server.js` exitoso.

## 3. Matriz de trazabilidad

| Requisito | Estado | Evidencia | Riesgo | Faltante tecnico |
|---|---|---|---|---|
| A. Reserva temporal desde WhatsApp | Parcialmente implementado | `TransactionalAgendaBookingService` crea reserva temporal; `CompleteAgendaJdbcRepository.insertTemporaryBooking` usa `PENDIENTE_CONFIRMACION`, expiracion y link; `CompleteDigitalAgendaService.createTemporaryBooking` puede generar link. | Alto | Blindar creacion con constraint DB anti-solape y estado canonico. |
| B. Enlace seguro de confirmacion | Implementado parcial | `booking_confirmation_link` tiene `token_hash`, expiracion, estados y unique token; `BookingConfirmationService.confirm` usa token hash y `findByTokenHashForUpdate`. | Medio | Agregar version de reserva/link, metadata de consumidor, IP/user-agent y pruebas de replay/concurrencia. |
| C. Confirmacion publica de reserva | Parcialmente implementado | `/api/v1/public/booking-confirmations/{token}/confirm`; marca link `CONFIRMED` y booking `CONFIRMED`. | Alto | El cambio de estado no exige version/precondicion fuerte; conflicto se valida por query, no por constraint DB. |
| D. Liberacion de cupo por vencimiento | Parcialmente implementado | Jobs `expireDueLinks` y `releaseExpiredTemporaryBookings`; actualiza a `EXPIRADA` y expira links. | Medio | Usar actualizaciones idempotentes con filtro de estado y bloqueo/lotes; cubrir carrera pago-confirmacion-expiracion. |
| E. No doble reserva | No suficientemente implementado | `hasConflict` y `hasOverlappingActiveBooking` comparan rangos; no se encontro `EXCLUDE`, `tstzrange`, `btree_gist` ni constraint equivalente. | Critico | Agregar constraints PostgreSQL por profesional y sala, y revisar multi-sede. |
| F. Reprogramacion por cliente/admin/WhatsApp | Parcialmente implementado | Public controllers y frontend; IA llama `completeDigitalAgendaService.reschedule`; repositorio usa `REPROGRAMADA`. | Alto | Transiciones sin estado esperado/version; reglas de politica/ventana no centralizadas; links no integrados a un state machine unico. |
| G. Cancelacion por cliente/admin/WhatsApp | Parcialmente implementado | Public cancellation links, pagina publica y flujo WhatsApp con confirmacion; `cancelBooking` marca `CANCELADA`. | Alto | `cancelBooking` actualiza sin precondicion de estado; falta politica central, actor/canal enriquecido y eventos idempotentes. |
| H. Pagos/senal de reserva | Parcial | `booking` tiene `requires_deposit`, `deposit_amount`, `payment_status`; pagos reales estan modelados para `orders`. | Alto | Falta entidad/transaccion de pago de reserva, webhook de proveedor, idempotencia de pago, firma y reglas pago-vs-expiracion. |
| I. Estados canonicos de reserva | Parcial | Conviven `REQUESTED`, `TEMPORARY`, `PENDIENTE_CONFIRMACION`, `CONFIRMED`, `REPROGRAMADA`, `CANCELADA`, `EXPIRADA`, etc. | Alto | Definir enum canonico unico, migrar aliases y crear maquina de estados. |
| J. Auditoria | Parcial | `audit_log` y `booking_status_history`; muchos `auditService.record`. | Medio | `AuditService` guarda metadata `{}`; historial no contiene IP, user-agent, message id, link id, version o payload de decision. |
| K. WhatsApp desacoplado e idempotente | Parcialmente implementado | Servicio Node firma HMAC; backend tiene canal Web/Cloud y log de eventos; V16/V32 agregan idempotencia parcial. | Medio | Completar deduplicacion por mensaje entrante/saliente y asociar eventos a acciones de reserva con trace end-to-end. |
| L. Frontend de reserva/agenda | Parcialmente implementado | Paginas de agenda, confirmacion, reprogramacion, cancelacion, detalle de cita y API client. | Medio | Estados inconsistentes con backend; falta verificacion automatizada ejecutada en este entorno. |
| M. Pruebas de calidad | Parcial | Backend pasa 279 tests. Hay tests de repositorio/canales/IA. | Alto | Faltan pruebas concurrentes/integracion DB para doble reserva, confirmacion simultanea, expiracion, pagos y reintentos WhatsApp. |

## 4. Brechas criticas

### 4.1 Doble reserva no bloqueada a nivel base de datos

La validacion principal esta en `CompleteAgendaJdbcRepository.hasConflict`, con filtro por `business_id`, `status`, `location_id` y solape horario. Esto es util para UX, pero no suficiente bajo concurrencia.

Evidencia:

- `CompleteAgendaJdbcRepository.java:368` define `hasConflict`.
- `CompleteAgendaJdbcRepository.java:376` exige `b.location_id = :locationId`.
- `BookingConfirmationJdbcRepository.java:202` define `hasOverlappingActiveBooking`.
- `BookingConfirmationJdbcRepository.java:210` tambien filtra por `location_id`.
- Las migraciones crean indices de ayuda (`idx_booking_slot_guard_public_flow`), pero no constraints de exclusion.

Impacto:

- Dos transacciones pueden validar disponibilidad al mismo tiempo y luego insertar/confirmar/reprogramar.
- Un profesional que trabaja en mas de una sucursal podria quedar doblemente asignado si el solape esta en distinta `location_id`.
- La sala/cabina tampoco esta protegida por exclusion DB.

### 4.2 Estados mixtos y sin maquina central

El sistema mezcla estados ingles/espanol y aliases. Ejemplos: `REQUESTED`, `TEMPORARY`, `PENDIENTE_CONFIRMACION`, `CONFIRMED`, `RESCHEDULED`, `REPROGRAMADA`, `CANCELLED`, `CANCELADA`, `EXPIRADA`, `EXPIRED`, `RELEASED`, `ATTENDED`.

Evidencia:

- `V30__public_reschedule_cancellation_email_reminders.sql:18` redefine `chk_booking_status`.
- `CompleteAgendaJdbcRepository.java:598` fuerza `REPROGRAMADA`.
- `CompleteAgendaJdbcRepository.java:637` fuerza `CANCELADA`.
- `BookingService` normaliza algunos aliases, pero no hay maquina de estados unica.

Impacto:

- Es facil permitir transiciones invalidas.
- Frontend y backend pueden interpretar distinto estados equivalentes.
- Reportes, filtros y jobs de expiracion pueden dejar reservas en estados no contemplados.

### 4.3 Cancelacion y reprogramacion actualizan sin precondicion fuerte

`updateBookingSchedule` y `cancelBooking` consultan el estado anterior para historico, pero el `update booking` no exige `status in (...)`, `version = :expectedVersion` ni bloqueo explicito propio en todos los caminos.

Evidencia:

- `CompleteAgendaJdbcRepository.java:598` asigna `REPROGRAMADA`.
- `CompleteAgendaJdbcRepository.java:637` asigna `CANCELADA`.
- `BookingPublicActionService` usa `findBookingForUpdate` en links publicos, pero los metodos base siguen permitiendo cambios directos desde admin/agenda/IA sin una regla central.

Impacto:

- Se podria cancelar una reserva ya atendida, expirada o confirmada por otro proceso si el flujo invocador no valida correctamente.
- La logica queda duplicada entre servicios.

### 4.4 Pagos de reserva incompletos

El modelo de reserva tiene campos de deposito, pero los pagos persistentes y endpoints encontrados pertenecen a `orders`, no a `booking`.

Evidencia:

- `V20__complete_digital_agenda.sql:203` agrega `payment_status` a `booking`.
- `V20__complete_digital_agenda.sql:224` define estados de pago de booking.
- `OrderController` expone `/orders/{id}/payment`; no se detecto endpoint equivalente para pagos de reserva.

Impacto:

- No hay garantia de que una reserva que requiere senal quede confirmada solo tras pago valido.
- No hay webhook firmado/idempotente para pagos de reservas.
- Faltan reglas para pago tardio vs expiracion de link/cupo.

### 4.5 Auditoria insuficiente para trazabilidad forense

Existe auditoria, pero el registro queda demasiado liviano para depurar disputas de reserva.

Evidencia:

- `AuditService.record` guarda metadata `"{}"` siempre.
- `audit_log` tiene columna `metadata`, pero no se llena con contexto.
- `booking_status_history` guarda estado anterior/nuevo, razon, actor y source, pero no link id, token hash, IP, user-agent, message id, correlation id ni version.

Impacto:

- Ante disputa, no se puede reconstruir con precision que mensaje/link/webhook provoco cada cambio.
- Los reintentos de WhatsApp o pago no quedan conectados end-to-end con la mutacion de agenda.

## 5. Brechas por modulo

### Backend agenda/reservas

- Falta restriccion DB anti-overlap para `professional_id` y `room_id`.
- Falta `booking.version` o similar para optimistic locking.
- Falta servicio de dominio `BookingStateMachine` con transiciones permitidas.
- Falta que `cancelBooking` y `updateBookingSchedule` usen `where status in (...) and version = ...`.
- Falta normalizar estados canonicos; los aliases deberian estar en una capa de compatibilidad, no como estados persistidos nuevos.

### Enlaces publicos

- Confirmacion usa token hash, expiracion y `FOR UPDATE`, lo cual es correcto como base.
- Faltan pruebas de doble clic simultaneo, token expirado simultaneo y confirmacion contra slot tomado.
- Faltan metadatos de uso: IP, user-agent, correlation id y resultado de validacion.
- Reprogramacion/cancelacion publica existen, pero deben apoyarse en maquina de estados y locks/versions comunes.

### WhatsApp/IA

- Existen flujos transaccionales para crear reserva temporal, cancelar y reprogramar desde WhatsApp.
- El servicio Node firma webhooks con HMAC y el backend registra eventos.
- Falta garantizar idempotencia completa por mensaje entrante y saliente asociada a la accion de reserva.
- Hay evidencia de inconsistencia documental/test-data: la matriz QA indica que cancelacion no ejecuta flujo transaccional completo, mientras el codigo actual si llama a `completeDigitalAgendaService.cancel`.

### Pagos

- Hay pagos para pedidos, no para reservas.
- `booking.payment_status` es un campo de estado, pero no hay modelo de transaccion de pago de booking.
- Falta proveedor/webhook de pago, firma, idempotency key, conciliacion y comportamiento ante pago despues de expiracion.

### Frontend

- Existen paginas publicas y privadas para agenda, detalle, confirmacion, cancelacion y reprogramacion.
- Los estados visibles incluyen aliases que no coinciden completamente con un enum canonico.
- No se pudo ejecutar test frontend por falta de dependencias instaladas en el entorno.

### Base de datos

- Buen avance: tablas de links, indices, historial, recordatorios, email log.
- Faltante critico: constraints de exclusion por rango temporal.
- Faltante recomendado: columnas de version, request id/idempotency key y metadatos de auditoria.

### Tests

- Backend pasa 279 tests.
- Cobertura especifica de riesgos criticos no es suficiente: hay tests unitarios de SQL, pero no pruebas de concurrencia real con PostgreSQL/Testcontainers ni carreras de confirmacion/expiracion/pago.

## 6. Recomendaciones de implementacion

1. Crear una maquina de estados de reserva:
   - Estados canonicos sugeridos: `SOLICITADA`, `PENDIENTE_CONFIRMACION`, `PENDIENTE_PAGO`, `CONFIRMADA`, `REPROGRAMACION_PENDIENTE`, `REPROGRAMADA`, `CANCELADA`, `EXPIRADA`, `ATENDIDA`, `NO_ASISTE`.
   - Mapear aliases actuales solo para compatibilidad de entrada/salida.
   - Prohibir persistir nuevos estados fuera del enum canonico.

2. Agregar proteccion anti-overlap en PostgreSQL:
   - Habilitar `btree_gist`.
   - Usar `tstzrange(starts_at, ends_at, '[)')`.
   - Crear constraints de exclusion parciales para reservas activas por `business_id + professional_id` y por `business_id + room_id`.
   - Considerar si `professional_id` debe ser global entre sucursales o si hay reglas explicitas por sede.

3. Incorporar versionado de reserva:
   - `booking.version integer not null default 0`.
   - Toda confirmacion, cancelacion, reprogramacion y expiracion debe actualizar con `version = version + 1`.
   - Las operaciones deben incluir estado esperado y/o version esperada.

4. Centralizar mutaciones:
   - Crear un servicio/repositorio unico para `confirm`, `expire`, `cancel`, `reschedule`, `mark_no_show`, `attend`.
   - Evitar updates directos de estado en repositorios no especializados.

5. Completar pagos de reserva:
   - Crear `booking_payment` o extender `payment` con `booking_id`.
   - Agregar provider transaction id, idempotency key, estado, monto, moneda, firma y raw payload.
   - Definir reglas: si pago llega tarde, si link expiro, si slot ya fue tomado.

6. Enriquecer auditoria:
   - Usar `audit_log.metadata` con payload estructurado.
   - Agregar `booking_status_history` campos: `correlation_id`, `message_id`, `link_id`, `ip_address`, `user_agent`, `version_before`, `version_after`, `idempotency_key`.
   - Asociar acciones WhatsApp, links y pagos al mismo trace.

7. Endurecer WhatsApp:
   - Deduplicar mensajes entrantes por `external_message_id`/delivery id.
   - Deduplicar acciones de reserva generadas por IA con idempotency key semantica.
   - Confirmar que Cloud API sea el modo productivo y WhatsApp Web quede marcado como experimental/local.

## 7. Plan por fases

### Fase 1 - Blindaje transaccional critico

- Definir estados canonicos y tabla/migracion de compatibilidad.
- Agregar `booking.version`.
- Crear `BookingStateMachine`.
- Refactorizar confirmacion, cancelacion, reprogramacion y expiracion para usar estado esperado/version.
- Agregar constraints `EXCLUDE` anti-solape.
- Crear tests de concurrencia con PostgreSQL real.

### Fase 2 - Pagos de reserva

- Modelar pagos asociados a booking.
- Implementar webhook firmado/idempotente.
- Integrar pago con confirmacion/expiracion.
- Cubrir carreras pago vs expiracion y pago duplicado.

### Fase 3 - Auditoria y trazabilidad

- Poblar metadata de `audit_log`.
- Extender `booking_status_history`.
- Asociar message id/link id/payment id/correlation id.
- Crear vista o endpoint de timeline forense por reserva.

### Fase 4 - Frontend y experiencia operativa

- Normalizar labels de estado.
- Bloquear acciones segun estado canonico.
- Mostrar timeline de reserva.
- Ejecutar y estabilizar Vitest/build frontend.

### Fase 5 - QA end-to-end

- Pruebas E2E de WhatsApp simulado: reserva, confirmacion, expiracion, cancelacion, reprogramacion.
- Pruebas de reintentos webhooks/pagos.
- Pruebas de carga minima sobre slots concurrentes.

## 8. Tests faltantes

Criticos:

- Dos clientes intentan crear reserva temporal para mismo profesional/hora.
- Dos clientes confirman links que compiten por el mismo slot.
- Confirmacion simultanea del mismo link desde dos requests.
- Reprogramacion simultanea al mismo slot desde admin y cliente.
- Cancelacion y confirmacion simultaneas sobre la misma reserva.
- Expiracion automatica y confirmacion publica al mismo tiempo.
- Pago recibido despues de expiracion.
- Pago duplicado con mismo provider transaction id.
- Mensaje WhatsApp duplicado que intenta repetir una cancelacion.
- Profesional asignado en dos sedes al mismo horario.
- Sala/cabina asignada en dos reservas simultaneas.

Importantes:

- Migracion de aliases de estados antiguos a canonicos.
- Frontend: paginas publicas con link expirado/usado/cancelado/reprogramado.
- Auditoria: cada mutacion de estado genera evento con correlation id y metadata.
- Recordatorios: se cancelan/reprograman sin duplicados.

## 9. Conclusion tecnica

El proyecto esta en una etapa avanzada de MVP/funcionalidad operativa, no en cero. Tiene agenda, enlaces publicos, WhatsApp, frontend y pruebas backend sanas. Sin embargo, para considerarlo listo como sistema de reservas confiable falta cerrar el nucleo transaccional: constraints anti-solape, versionado, maquina de estados, pagos de reserva y auditoria enriquecida.

La prioridad debe ser evitar doble reserva y estados inconsistentes antes de seguir agregando funcionalidades visibles. Despues de eso, pagos y auditoria completan el nivel necesario para operar con seguridad.

¿Quieres que inicie la segunda fase e implemente primero las brechas criticas de doble reserva, estados y versionado?
