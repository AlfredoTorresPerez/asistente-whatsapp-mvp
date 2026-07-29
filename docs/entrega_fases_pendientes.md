# Entrega Fases Pendientes

## Fase P1-1: Idempotencia funcional de reservas

Se implemento el pendiente `CAP-019` / `PRE-RES-035`: clave de idempotencia funcional para operaciones de reserva con resultado persistido.

| Elemento | Resultado |
|---|---|
| Persistencia | Nueva tabla `agenda_booking_operation_idempotency`. |
| Clave funcional | Operacion + negocio + conversacion/cliente + telefono + sede + servicio + profesional + sala + horario. |
| Clave explicita | Si la request trae `idempotencyKey`, se respeta como clave externa y se valida contra el hash de la solicitud. |
| Resultado persistido | `booking_id`, `status = COMPLETED`, `result.bookingId`, `completed_at`. |
| Reintentos | Si la misma operacion llega de nuevo, devuelve la reserva ya creada. |
| Reuso incorrecto | Si la misma clave se usa con una solicitud distinta, responde `IDEMPOTENCY_KEY_REUSED`. |

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `backend-java/src/main/resources/db/migration/V86__booking_operation_idempotency.sql` | Tabla e indices para resultado idempotente de operaciones de reserva. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java` | Metodos para consultar, reservar y completar operaciones idempotentes. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaService.java` | Idempotencia en creacion temporal de agenda, incluyendo flujo IA. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/application/PublicLandingService.java` | Idempotencia en creacion publica desde landing. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepositoryTest.java` | Cobertura SQL de reserva/completado idempotente. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaServiceTest.java` | Cobertura de reintento que devuelve reserva existente. |

## Verificacion

Comandos ejecutados en `backend-java`:

```powershell
.\mvnw.cmd spotless:apply -q
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd test -q
```

Resultado: `ALL TESTS PASSED`.

## Fase P1-2: Consentimiento informado y edad/tutor

Se implemento enforcement minimo para servicios que declaran `requires_informed_consent` y para clientes menores de edad cuando se informa fecha de nacimiento.

| Elemento | Resultado |
|---|---|
| Persistencia | Nueva migracion `V87__booking_consent_guardian.sql` con columnas en `booking`. |
| Consentimiento | Si el servicio requiere consentimiento, la reserva exige `informedConsentAccepted = true`. |
| Edad | `customerBirthDate` no puede ser futura. |
| Tutor | Si `customerBirthDate` indica menor de 18 anos al dia de la reserva, exige `guardianName` y `guardianPhone`. |
| Flujos cubiertos | Agenda interna/IA mediante `CreateTemporaryAgendaBookingRequest` y landing publica mediante `CreatePublicBookingRequest`. |
| Persistencia del resultado | Se guarda `requires_informed_consent`, `informed_consent_accepted`, `informed_consent_accepted_at`, `customer_birth_date`, `guardian_name`, `guardian_phone`. |

Archivos modificados:

| Archivo | Cambio |
|---|---|
| `backend-java/src/main/resources/db/migration/V87__booking_consent_guardian.sql` | Columnas de consentimiento y tutor en `booking`. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/api/CreateTemporaryAgendaBookingRequest.java` | Campos opcionales de consentimiento, fecha nacimiento y tutor. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/api/CreatePublicBookingRequest.java` | Campos opcionales equivalentes para landing publica. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaService.java` | Validacion y persistencia en agenda interna/IA. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/application/PublicLandingService.java` | Validacion y persistencia en landing publica. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java` | Metodo `recordBookingConsent`. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaServiceTest.java` | Test de rechazo por consentimiento faltante. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepositoryTest.java` | Test SQL de persistencia de consentimiento/tutor. |

Verificacion posterior a P1-2: `ALL TESTS PASSED`.

## Fase P1-3: Sede preferida, cercania y capacidad agregada

Se implemento priorizacion no financiera de sedes y capacidad diaria agregada por sede.

| Elemento | Resultado |
|---|---|
| Capacidad diaria de sede | Nueva columna `business_location.daily_booking_capacity`; `null` significa sin limite agregado. |
| Disponibilidad | `hasConflict(...)` considera sede llena por dia antes de evaluar profesional/sala. |
| Concurrencia | Insercion temporal y reprogramacion bloquean `business_location for update` y revalidan capacidad diaria. |
| Priorizacion publica | `GET /api/v1/public/landing` acepta `preferredLocationId`, `latitude`, `longitude`. |
| Priorizacion por servicio | `GET /api/v1/public/landing/services/{serviceId}/branches` acepta los mismos parametros. |
| Orden aplicado | Sede preferida primero, luego menor distancia, luego mayor capacidad diaria configurada, luego nombre. |
| Respuesta publica | Las sedes exponen `latitude`, `longitude`, `dailyBookingCapacity`, `distanceKm`, `preferred`. |

Archivos modificados:

| Archivo | Cambio |
|---|---|
| `backend-java/src/main/resources/db/migration/V88__location_daily_booking_capacity.sql` | Capacidad diaria agregada de sede e indice de consulta. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java` | Conflicto por capacidad diaria y locks transaccionales de sede. |
| `backend-java/src/main/java/com/asistentewhatsapp/locations/infrastructure/BusinessLocationJdbcRepository.java` | Lectura de lat/lng/capacidad diaria. |
| `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/infrastructure/AestheticCenterJdbcRepository.java` | Branches de servicio con lat/lng/capacidad diaria. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/api/PublicLandingController.java` | Query params opcionales de preferencia/cercania. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/application/PublicLandingService.java` | Ordenamiento por preferencia, distancia y capacidad. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/api/LandingLocationItemResponse.java` | Metadata publica de sede. |
| `backend-java/src/main/java/com/asistentewhatsapp/landing/api/PublicServiceBranchResponse.java` | Metadata publica de sede por servicio. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepositoryTest.java` | Cobertura SQL de capacidad diaria. |

Verificacion posterior a P1-3: `ALL TESTS PASSED`.

## Fase P1-4: Reglas avanzadas de profesional

Se implementaron reglas configurables de elegibilidad profesional sin hardcodear niveles ni tipos de certificacion.

| Elemento | Resultado |
|---|---|
| Nivel profesional | Nueva columna `aesthetic_professional.qualification_level`. |
| Certificacion vigente | Nueva columna `aesthetic_professional.certification_valid_until`. |
| Requisito por servicio | Nuevas columnas `aesthetic_service.required_professional_level` y `aesthetic_service.requires_professional_certification`. |
| Elegibilidad | `findProfessionalCandidates(...)` excluye profesionales que no cumplen nivel minimo o certificacion vigente cuando el servicio lo exige. |
| Rotacion diaria | Candidatos se ordenan por menor cantidad de reservas activas del dia antes que por nombre. |
| Compatibilidad | Servicios sin requisitos nuevos mantienen el comportamiento existente. |

Archivos modificados:

| Archivo | Cambio |
|---|---|
| `backend-java/src/main/resources/db/migration/V89__professional_qualification_rules.sql` | Campos e indices de nivel/certificacion profesional. |
| `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java` | Filtros de elegibilidad y orden por carga diaria. |
| `backend-java/src/test/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepositoryTest.java` | Cobertura SQL de nivel, certificacion y rotacion. |

Verificacion posterior a P1-4: `ALL TESTS PASSED`.

## Pendientes siguientes

Pagos, abonos, devoluciones, no transferibilidad y disputas quedan fuera de alcance de esta version por decision de producto.

| Prioridad | Pendiente |
|---|---|
| P1-5 | Pausas/colacion especificas, reloj oficial y resumen previo. |
| P1-6 | Servicios encadenados, paquetes, sesiones, reservas grupales, lista de espera y stock critico transaccional. |
