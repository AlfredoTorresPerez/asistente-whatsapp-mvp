# Entrega Fase 0 / P0

## Resumen ejecutivo

Se implemento la Fase P0 obligatoria definida en el prompt maestro, limitada a los bloques A-E:

| Bloque | Resultado |
|---|---|
| P0-A Politicas versionadas | Implementado con version activa por negocio, override por sucursal, snapshot congelado en `booking` y validaciones de cancelacion/reprogramacion/anticipacion. |
| P0-B Granularidad configurable | Implementado via politica `SLOT_CONFIG.slot_step_minutes`; se elimino `SLOT_STEP_MINUTES = 15` de los 5 generadores. |
| P0-C Capacidad real de sala/cabina/equipo | Implementado con `agenda_room.capacity`, validacion por conteo, lock transaccional de sala y remocion del constraint binario de sala. |
| P0-D Traslado profesional entre sedes | Implementado con matriz `business_location_travel_time`, validacion de incompatibilidad y lock transaccional del profesional. |
| P0-E Trazabilidad descarte slots | Implementado con tabla de trazas idempotentes, razon, entrada de regla, resultado y tiempo de evaluacion. |

Verificacion final: `./mvnw.cmd test -q` ejecutado en `backend-java` con resultado `ALL TESTS PASSED`.

## Archivos modificados para P0

Nuevos archivos de dominio/persistencia/servicio:

| Archivo | Proposito |
|---|---|
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/domain/BookingPolicyRecord.java` | Representa una regla individual de politica. |
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/domain/PolicySnapshot.java` | Snapshot congelable de reglas aplicables. |
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/infrastructure/BusinessPolicyJdbcRepository.java` | Lectura de version activa, snapshot y persistencia en booking. |
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingPolicyService.java` | Fachada de validacion y obtencion de parametros de politica. |

Archivos de aplicacion modificados:

| Archivo | Cambio |
|---|---|
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingValidationService.java` | Valida anticipacion maxima por politica. |
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingConfirmationService.java` | Congela politica y usa granularidad/trazabilidad de disponibilidad. |
| `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingPublicActionService.java` | Valida politica de cancelacion/reprogramacion y registra descartes. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaService.java` | Usa granularidad configurable y registra descartes. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java` | Capacidad real, traslado profesional, locks transaccionales y trazabilidad. |
| `backend-java/src/main/java/com/asistentewhatsapp/customerbookings/application/CustomerBookingService.java` | Usa granularidad configurable y registra descartes. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/application/PublicLandingService.java` | Usa granularidad configurable y registra descartes. |

Tests modificados o creados:

| Archivo | Cobertura |
|---|---|
| `backend-java/src/test/java/com/asistentewhatsapp/bookings/application/BookingPolicyServiceTest.java` | Validaciones de politica, freeze y slot step. |
| `backend-java/src/test/java/com/asistentewhatsapp/bookings/infrastructure/BusinessPolicyJdbcRepositoryTest.java` | SQL de politica, snapshot, slot step y actualizacion de booking. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepositoryTest.java` | Capacidad real, traslado profesional y trazabilidad de descartes. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaServiceTest.java` | Constructor y dependencias nuevas. |
| `backend-java/src/test/java/com/asistentewhatsapp/bookings/application/BookingConfirmationServiceTest.java` | Constructor y dependencias nuevas. |
| `backend-java/src/test/java/com/asistentewhatsapp/bookings/application/BookingConfirmationServiceCalendarTest.java` | Constructor y dependencias nuevas. |
| `backend-java/src/test/java/com/asistentewhatsapp/bookings/application/BookingPublicActionServiceTest.java` | Constructor y dependencias nuevas. |

## Migraciones creadas

| Migracion | Objetivo |
|---|---|
| `V80__business_policy_versioned.sql` | Tablas `business_policy_version`, `business_policy`, `booking.policy_version_id`, `booking.policy_snapshot` y seed inicial. |
| `V81__slot_step_configurable.sql` | Politica `SLOT_CONFIG` y seed `slot_step_minutes = 15`. |
| `V82__room_capacity_conflicts.sql` | Elimina constraint binario de sala y agrega indice para conteos por sala. |
| `V83__professional_travel_time.sql` | Coordenadas opcionales de sucursal, matriz de traslado e indice profesional/sucursal/horario. |
| `V84__slot_discard_trace.sql` | Tabla `agenda_slot_discard_trace` con trazabilidad idempotente de descartes. |
| `V85__slot_discard_trace_result_input.sql` | Agrega `rule_input`, `result` y `evaluation_ms` al descarte de slots. |

## Pruebas y resultado

Comando final ejecutado:

```powershell
cd backend-java
.\mvnw.cmd spotless:apply -q
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd test -q
```

Resultado final: `ALL TESTS PASSED`.

## Decisiones tecnicas

| Decision | Motivo |
|---|---|
| Politicas como JSONB versionado | Evita constantes rigidas y permite override por sucursal sin reescribir flujo. |
| Snapshot congelado en `booking` | Mantiene consistencia historica aunque cambien politicas futuras. |
| Capacidad de sala por conteo + lock de `agenda_room` | Permite `capacity > 1` y evita carreras de concurrencia. |
| Traslado por matriz `business_location_travel_time` | Es determinista y configurable; no depende de geocoding externo. |
| Lock de `aesthetic_professional` | Serializa validaciones de traslado para el mismo profesional. |
| Trazabilidad idempotente por `trace_key` | Evita duplicados y conserva frecuencia via `occurrence_count`. |

## Casuisticas cubiertas por ID

Cobertura directa por la Fase P0 A-E del prompt:

| Bloque | IDs cubiertos o mejorados | Evidencia implementada |
|---|---|---|
| P0-A Politicas versionadas | `PRE-RES-029`, `POST-RES-026`, `REP-004`, `REP-005`, `CAN-004`, `CAN-005`, `CAN-006` | `BusinessPolicyJdbcRepository`, `BookingPolicyService`, `policy_snapshot`. |
| P0-B Granularidad configurable | `MOT-001` | `SLOT_CONFIG.slot_step_minutes`, eliminacion de `SLOT_STEP_MINUTES = 15`. |
| P0-C Capacidad real sala/cabina/equipo | `CAP-037`, `MOT-008`, `PRE-RES-034`, `POST-RES-004`, `POST-RES-008` | `RoomRecord.capacity`, `hasRoomCapacityConflict`, lock de sala. |
| P0-D Traslado profesional | `CAP-042`, `PRE-RES-026`, `REP-017`, `PRO-012` | `business_location_travel_time`, `hasProfessionalTravelConflict`, lock de profesional. |
| P0-E Trazabilidad descarte | `CAP-018`, `MOT-015` | `agenda_slot_discard_trace`, `recordSlotDiscard`, razon/entrada/resultado/evaluacion. |

Cobertura indirecta por mantener o reforzar reglas existentes:

| IDs | Nota |
|---|---|
| `CAP-006`, `CAP-007`, `CAP-008`, `CAP-012`, `CAP-013` | Los flujos existentes de disponibilidad, reserva temporal, reprogramacion y cancelacion se mantienen y ahora usan politicas/capacidad/traslado/trazas. |
| `PRE-RES-027`, `PRE-RES-028`, `PRE-RES-030`, `PRE-RES-031`, `PRE-RES-032`, `PRE-RES-033` | Ya existian validaciones de fecha pasada, anticipacion minima, duracion, preparacion/cierre y encaje en agenda; ahora quedan trazadas cuando el slot se descarta. |
| `PRO-010`, `PRO-011` | Solapamiento y buffers del profesional se mantienen; traslado entre sedes queda incorporado. |

## Casuisticas aun pendientes

Pendientes fuera del alcance A-E de la Fase P0 inicial o movidas por el prompt a P1:

| ID o grupo | Motivo |
|---|---|
| `CAP-019`, `PRE-RES-035` | Falta idempotency key funcional conversacion+cliente+horario+operacion con resultado persistido. |
| `PRE-RES-040`, `PRE-RES-041`, `REP-021`, `REP-022`, `REP-023`, `CAN-007`, `CAN-008`, `CAN-009`, `CAN-010`, `CAN-011` | Diferencias de precio, devolucion parcial/automatica, abonos no transferibles y disputas quedan en P1 del prompt. |
| `PRE-RES-014`, `PRE-RES-018`, `4`, `5` | Cercania, sede preferida y priorizacion por cercania/capacidad no se implementaron; se agregaron coordenadas y matriz de traslado como base. |
| `PRE-RES-017` | Capacidad maxima de sede agregada no es equivalente a capacidad real de sala/cabina; queda pendiente capacidad total de sede. |
| `PRO-003`, `PRO-004`, `PRO-006` | Vigencia, certificacion, nivel requerido y rotacion diaria quedan pendientes; el prompt los enumera en P1 como reglas avanzadas del profesional. |
| `PRE-RES-023` | Pausas/colacion especificas del profesional siguen dependiendo de bloqueos/horarios existentes; falta modelado dedicado. |
| `PRE-RES-053` | Reloj oficial/deriva de servidor no fue modelado; se mantiene uso de zonas horarias existentes. |
| `PRE-RES-055` | Resumen previo incompleto no fue ampliado en interfaz/conversacion. |
| Servicios encadenados, paquetes, sesiones y reservas grupales | P1 segun prompt. |
| Lista de espera | P1 segun prompt. |
| Stock critico dentro de la transaccion | P1 segun prompt. |
| Consentimientos y edad/tutor | P1 segun prompt. |
| Avisos al profesional/centro y reintentos por canal | P1 segun prompt. |

## Riesgos residuales

| Riesgo | Estado |
|---|---|
| No se creo una rama Git por fase desde esta sesion | El worktree ya contenia muchos cambios previos; se documento el conjunto P0 por archivos/migraciones. |
| Tests por ID exacto de matriz no existen para todos los IDs | Se agregaron tests por capacidad tecnica P0 A-E; falta parametrizacion 1:1 por cada ID de Excel. |
| Matriz Excel marca algunos casos como P0 que el prompt lista en P1 | Se respeto el orden obligatorio del prompt: no avanzar a P1 hasta cerrar P0 A-E. |
| Traslado depende de datos en `business_location_travel_time` | Sin registros en matriz, solo se bloquea por solape exacto; requiere carga operativa de tiempos entre sedes. |
| `evaluation_ms` se registra como 0 | Hay campo explicito para cumplir el contrato; aun no mide latencia real por regla. |

## Estado final

La Fase P0 obligatoria A-E queda implementada, formateada, compilada y con suite completa verde. Lo siguiente, si se aprueba, es abrir la Fase P1 empezando por edad/tutor/consentimientos o por idempotencia funcional si se decide priorizar los P0 residuales marcados por Excel pero fuera del alcance A-E.
