# CAMBIOS - FASE 3: Entidades canonicas y deteccion registrada (ai_canonical_entity + ai_detected_entity)

Fecha: 2026-07-31
Contexto: `docs/cambios/ANALISIS_PREVIO_CAPA_SEMANTICA_IA.md`, `docs/cambios/CAMBIOS_FASE1_CAPA_SEMANTICA_IA.md`, `docs/cambios/CAMBIOS_FASE2_CAPA_SEMANTICA_IA.md`

## Objetivo

Normalizar las entidades extraidas de cada mensaje contra un catalogo
canonico por negocio (`ai_canonical_entity`, evolucionando `ai_entity_alias`
para que sus alias apunten a entidades canonicas) y persistir las entidades
detectadas en `ai_detected_entity`, vinculadas al analisis de la FASE 2.

## Cambios

### Base de datos (Flyway)

| Migracion | Contenido |
|---|---|
| `V100__ai_canonical_entity.sql` | Tabla `ai_canonical_entity`: entity_type restringido (SERVICE/PROFESSIONAL/ROOM/LOCATION/RELATIVE_DATE/PREFERENCE/OTHER), reference_type/reference_id opcionales y acoplados (ambos null o ambos con valor), canonical_name acotado y unico por (business_id, entity_type), display_name, language/country_code, priority y version con control de concurrencia (hilo audit). Migra datos desde `ai_entity_alias` agrupando por canonical_name: SERVICEs contra `aesthetic_service` (entity_key en servicio_o_producto/categoria_servicio), PROFESSIONAL contra `aesthetic_professional` (profesional), LOCATION contra `business_location` (sede), RELATIVE_DATE desde fecha_relativa y PREFERENCE desde preferencia_horaria. Demo: 7 servicios, 0 profesionales, 1 sede, 11 fechas, 3 preferencias. |
| `V101__evolve_ai_entity_alias.sql` | Evoluciona `ai_entity_alias`: agrega `canonical_entity_id` (FK set null), `normalized_alias` (backfill lower(alias)), language default es, country_code default CL, alias_type restringido (SYNONYM/ORTHOGRAPHIC_ERROR/REGIONALISM/PREFERRED/CONTEXTUAL) default SYNONYM, confidence_base default 0.85 con check 0-1, ventanas valid_from/valid_until. Backfill de canonical_entity_id por nombre canonico coincidente e indices nuevos. |
| `V102__ai_detected_entity.sql` | Tabla `ai_detected_entity`: entity_type restringido (SERVICE/PROFESSIONAL/ROOM/LOCATION/RELATIVE_DATE/TIME/PERSON/CONTACT/PREFERENCE/OTHER), resolution_method restringido (DATABASE/PATTERN/ALIAS/LLM/HUMAN), canonical_entity_id FK set null, matched_alias, confidence 0-1, referencia opcional. Indices por analisis, negocio y canonical. |

Nota: en los selects DISTINCT de la migracion V100 se agregaron casts
explicitos `null::varchar(80)` y `null::uuid` para los campos de referencia
(error PostgreSQL: "column reference_id is of type uuid but expression is of
type text").

### Java

| Archivo | Cambio |
|---|---|
| `CanonicalEntityJdbcRepository` (nuevo) | `findActive(UUID)` (global + local al negocio, orden priority desc, length desc), `findIdByCanonicalName(UUID, String)` (local-first con limit 1) e `insertDetectedEntity(UUID, DetectedEntityRecord)`. Records `CanonicalEntityRecord` y `DetectedEntityRecord`. |
| `CanonicalEntityService` (nuevo) | Cache por negocio (ConcurrentHashMap) con TTL reutilizando `app.ai.agents.intent-catalog-cache-ttl-seconds` (300s); degrada a cache o vacio si la consulta falla. Invalida por negocio o global. |
| `EntityExtractionService` | Constructor `@Autowired` con `CanonicalEntityService` opcional (legacy de 2 args sin el bean, tests intactos). Nueva etapa `applyCanonicalEntities` antes de `applyDatabaseAliases`: matchea el texto normalizado contra canonical_name (coincidencia por substring) y puebla `servicio_o_producto` (+ `servicio_codigo` via knowledgeService), `profesional`, `sede` (+ `sede_id` si reference_type=business_location), `fecha_relativa` y `preferencia_horaria`. Usa putIfAbsent para no pisar extras especificos. |
| `DetectedEntityService` (nuevo) | Registra las entidades rastreadas (servicio_o_producto, sede, profesional, fecha, fecha_relativa, hora, tramo_horario, preferencia_horaria, nombre, telefono) en `ai_detected_entity`, vinculadas al `message_analysis_id` devuelto por FASE 2. Resuelve canonical_entity_id por nombre canonico (local-first): si existe -> DATABASE con canonical, si no -> PATTERN. |
| `MessageAnalysisService` | `record(...)` ahora retorna el UUID del analisis creado (null si no aplica) para encadenar el registro de entidades. |
| `AgentCoordinatorService` | Constructor de 8 args `@Autowired` (agrega `DetectedEntityService`) + legacy de 6 args (nulos, tests intactos). En `route()` encadena `detectedEntityService.record(analysisId, request, entities)` tras el analisis, dentro de `auditEnabled`. |

## Verificacion

1. V100-V102 validadas primero en transaccion con rollback (`BEGIN; ...;
   ROLLBACK;`) y luego aplicadas por Flyway en el rebuild del backend
   (schema_version 102; `ai_canonical_entity` = 7 servicios, 1 sede, 11
   fechas, 3 preferencias; `ai_entity_alias` = 69 registros).
2. `mvn spotless:apply` + `mvn compile` OK.
3. Suite `aiagents`: 313 tests, 37 failures + 11 errors = 48 fallos (misma
   base). No empeoro.
4. Prueba funcional en produccion local (simulador WhatsApp, session
   `demo-sales`): "quiero agendar una hora para depilacion laser esta semana"
   -> 1 `ai_message_analysis` (JAVA_FALLBACK, BOOKING_CREATE 0.83,
   ambiguity 0.17) y 3 `ai_detected_entity`:
   - SERVICE `servicio_o_producto` "Depilacion laser" (PATTERN, sin canonical:
     el matcheo canonico exige coincidencia de canonical_name).
   - RELATIVE_DATE `fecha_relativa` "esta semana" (DATABASE, con
     canonical_entity_id a "esta semana").
   - CONTACT `telefono` "+56912345678" (PATTERN).

## Notas

- `ai_entity_alias` sigue intacta en lectura: su evolucion (canonical_entity_id,
  alias_type, confidence_base) es compat de schema y queda disponible para la
  resolucion por alias en fases posteriores (FASE 4).
- La resolucion por canonical_name exige coincidencia del substring
  normalizado; los matcheos por alias (ej. "depilacion laser" -> nombre
  canonico "Depilación Láser") quedan para la FASE 4 (resolve de catalogo
  con scoring) y para el registro de entidades via alias.
- Decidido durante la implementacion: `DetectedEntityService` no crea analisis
  propio; se vincula al `ai_message_analysis` existente retornando su UUID
  desde `MessageAnalysisService.record`. El mapa entity_key -> entity_type
  (servicio_o_producto -> SERVICE, etc.) vive en ese servicio.
- El simulador del canal usa `DemoIncomingMessageRequest(sessionKey, from,
  body, externalMessageId)`; el negocio demo es `11111111-1111-1111-1111-
  111111111111` con session `demo-sales`.
