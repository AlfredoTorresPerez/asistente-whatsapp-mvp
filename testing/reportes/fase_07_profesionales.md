# FASE 07 — Consultas de profesionales

**Estado:** COMPLETA  ·  **Criterio de aceptación:** los 7 escenarios históricos son evaluables con respuestas de datos reales o deterministas.  ·  **Build:** `mvn -q test` → 769 tests, 0 fallas.

## 1. Objetivo

Implementar la capacidad de consultar profesionales sin **inventar disponibilidad ni nombres**.

## 2. Principios aplicados (de FASE_07_PROFESIONALES.txt)

- Se reusaron los repositorios y tablas existentes: `CompleteAgendaJdbcRepository.findProfessionalFilterOptions`
  (filtra profesionales `active = true`) + `BusinessLocationJdbcRepository` + la normalización
  existente `TextNormalizer.normalize`.
- Como validación (acción 4 y 5): un nombre solo se acepta como `profesional` **si existe en el catálogo
  activo** del negocio; si no existe, se marca `profesional_no_encontrado` y **no** se atribuyen horarios a
  un profesional inexistente.
- Como validación (acción 6): nunca se promete cupo. La disponibilidad sólo se consulta cuando se aportan
  servicio, sucursal y fecha; y cuando se menciona un profesional, se pasa su `professionalId` real a
  `CompleteDigitalAgendaService.availability` para que los slots devueltos sean realmente del profesional
  mencionado.
- No se trata una persona como profesional sólo por formato de nombre propio (p. ej. `María`, `doctor`).
  El nombre debe ir acompañado de un contexto profesional (`con X`, `X atiende`, `X trabaja`, `misma
  persona`) y luego validarse contra el catálogo.
- (acción 7) La intención principal `PROFESSIONAL_QUERY` y la secundaria `AVAILABILITY_QUERY` se conservan
  cuando la consulta incluye disponibilidad o reserva.

## 3. Cambios de código

| Archivo | Cambio |
|---|---|
| `.../application/ProfessionalCatalogService.java` | **Nuevo.** Catálogo real de profesionales activos: `findActive`, `findByLocation`, `findByName` (exacto → contiene → nombre de pila), `findBySpecialty`, `isActiveProfessional`. Lee de `CompleteAgendaJdbcRepository.findProfessionalFilterOptions` (garantiza `active=true`) y normaliza con `TextNormalizer`. Null-safe (`List.of()` sobre vacío). |
| `.../application/EntityExtractionService.java` | Valida el nombre mencionado contra el catálogo: nombre real → `profesional`; nombre no registrado → `profesional_no_encontrado` (no inventa). Añadidos `PROFESSIONAL_ATTENDS_BEFORE_PATTERN`/`_AFTER_PATTERN` (nombre antes/después de *atiende/trabaja/recibe*). `isLocationName` evita que una sucursal (p. ej. "Provisión") se confunda con profesional. Constructor sobrecargado (el de 2 args usado por 7 tests se conserva). |
| `.../application/BookingAgent.java` | `handleProfessionalQuery` ya no inventa: nombre confirmado pregunta servicio/día (y muestra la especialidad real); nombre no encontrado pregunta servicio/día y no reclama disponibilidad; nombre en blanco/listing muestra los profesionales reales disponibles. Nueva sobrecarga `checkAvailability(..., professionalId, ...)`. Constructor sobrecargado (5-arg conservado para los 8 tests existentes; 6-arg `@Autowired`). |
| `.../application/TransactionalAgendaBookingService.java` | Sobrecarga `checkAvailability(..., UUID professionalId, ...)` que reenvía `professionalId` a `AgendaAvailabilityRequest`, filtrando la agenda por el profesional. La sobrecarga original (10 args) delega con `professionalId = null` → compatibilidad con tests existentes. |
| `conversation/master/master-conversation-catalog.json` | Ampliado `PROFESSIONAL_WORDS` con `doctor`, `doctora`, `terapeuta`, `especialista`, `atiende`, `recibe`, `trabaja la/trabaja`, `la misma`, `misma persona`, `misma profesional`. |

## 4. Capacidades FASE 07 (9) y su cobertura

| # | Capacidad | Implementación / prueba |
|---|---|---|
| 1 | profesional específico | `EntityExtractor` valida el nombre contra el catálogo → `professionalEspecificoConfirmadoSeExtraeDelCatalogo`, `handleProfessionalConfirmadoPreguntaServicioYDia` |
| 2 | profesionales por servicio | `ProfessionalCatalogService.findBySpecialty` + `catalogoResuelveProfesionalesActivosPorEspecialidad` |
| 3 | profesional por sucursal | `ProfessionalCatalogService.findByLocation` (filtra por `locationId`) |
| 4 | profesional con nombre propio (no inventado) | `tituloGenericoDoctorNoSeExtraeComoProfesional` + `nombreComunSinContextoProfesionalNoSeExtraeComoProfesional` |
| 5 | profesional y disponibilidad | `handleAvailabilityWithProfessionalConsultaDisponibilidadConProfessionalIdConfirmado` (verifica que se pase `professionalId` a `checkAvailability`) |
| 6 | mismo profesional de atención anterior | detección de mención genérica `misma profesional`/`la misma` → `profesional_mencion_generica` |
| 7 | profesional no encontrado | `profesionalNoEncontradoNoSeInventaYSeMarca`, `handleProfessionalNoEncontradoPreguntaServicioYDiaYNoReservaBajoNombreInventado`, `nombreNoEnCatalogoNoSeAtribuyeProfessionalIdAlDisponibilidad` |
| 8 | nombre común no en catálogo | Marca `profesional_no_encontrado` y no pasa `professionalId` → no inventa disponibilidad |
| 9 | sucursal no confundida con profesional | `nombreDeSucursalNoSeConfundeConProfesional` (Providencia descartada como nombre de profesional) |

## 5. Evaluación de los 7 escenarios históricos (`testing/conversation-tests/profesionales.md`)

Se evaluó la primera entrega de turno (Turno 1) de cada caso contra el comportamiento implementado
(registro de negocio `11111111-1111-1111-1111-111111111111`; profesionales sembrados: Carla Mendez,
Valentina Ríos, Daniela Soto; sucursal Providencia).

- **CE-WA-120001** `¿Tienen disponibilidad con María?` → intención `PROFESSIONAL_QUERY`; respuesta
  determinista que pregunta servicio y día ("No confirmé a \"María\"… Dime el servicio y el día que deseas
   y reviso disponibilidad con quien atiende ese servicio."). María no está en el catálogo → no se inventa
   disponibilidad ni se pasa `professionalId`.
- **CE-WA-120002** `¿Qué especialistas trabajan en depilación?` → intención `PROFESSIONAL_QUERY`; lista los
  profesionales reales disponibles y pregunta con qué profesional o servicio atender.
- **CE-WA-120003** `¿Puedo atender con la misma persona de la otra vez?` → mención genérica detectada
  (`misma profesional`/`la misma`); intención `PROFESSIONAL_QUERY`; pregunta nombre o servicio/día.
- **CE-WA-120004** `¿María atiende limpieza facial profunda?` → nombre mencionado validado; pide día/sucursal.
- **CE-WA-120005** `¿Qué horas tiene el doctor para el viernes?` → `doctor` es un título genérico, no se
  extrae como nombre de profesional (no inventa); intención `PROFESSIONAL_QUERY`; pide servicio y sucursal.
- **CE-WA-120006** `¿En Providencia trabaja la persona de láser?` → "Providencia" se descarta como nombre de
  profesional (sucursal conocida); no hay entidad `profesional`; se pide servicio y día.
- **CE-WA-120007** `¿Cuándo atiende Carmen?` → nombre mencionado validado contra el catálogo; pregunta
  servicio y día. (Carmen no está en el catálogo de pruebas → `profesional_no_encontrado`, pregunta de forma
   no inventiva).

**Nota sobre la intención:** la clasificación de intenciones en este repositorio se basa en listas de
palabras (`PROFESSIONAL_WORDS`) y expresiones de la base de datos. Las 7 respuestas del agente son
deterministas y respaldadas en datos reales; la intención `PROFESSIONAL_QUERY` se mantiene y, cuando la
consulta incluye disponibilidad, se conserva `AVAILABILITY_QUERY` como principal/secundaria según la
lógica existente.

## 6. Pruebas (`backend-java`)

- `ProfessionalQueryResolutionTest` (nuevo, 11 tests) — cubre entity extraction, catálogo, y el handler
  `BookingAgent` con `ProfessionalCatalogService` real respaldado por `CompleteAgendaJdbcRepository` de prueba.
- Regresión completa: `mvn -q test` → **769 tests, 0 fallas** (incluye los 8 constructores de test
  existentes de `BookingAgent`/`EntityExtractionService` que no usan catálogo → retrocompatibles).

## 7. Cambios de base de datos

Ninguno. Se reusan `agenda_professional_service`, `agenda_professional_location`, `aesthetic_professional`
y las vistas/repositorios existentes; no se añaden migraciones.

## 8. Evidencias / comandos de verificación

```
cd C:\mvp\asistente_impl_codex\asistente\backend-java
mvn spotless:apply -q
mvn test -q                               # 769 OK
mvn test -Dtest=ProfessionalQueryResolutionTest -q   # 11 OK
```

## 9. Condición de detención

Cumplida. No se avanza a confirmación ni reprogramación.
