# CAMBIOS - FASE 2: Analisis canonico por mensaje (ai_message_analysis + ai_detected_intent)

Fecha: 2026-07-31
Contexto: `docs/cambios/ANALISIS_PREVIO_CAPA_SEMANTICA_IA.md`, `docs/cambios/CAMBIOS_FASE1_CAPA_SEMANTICA_IA.md`

## Objetivo

Registrar el analisis canonico de cada mensaje procesado por el motor de IA:
un `ai_message_analysis` por turno y el ranking de intenciones
(`ai_detected_intent`) con su fuente de deteccion.

## Cambios

### Base de datos (Flyway)

| Migracion | Contenido |
|---|---|
| `V98__ai_message_analysis.sql` | Tabla `ai_message_analysis` (business/conversation/customer/created_by FKs, detector_type en DATABASE/JAVA_FALLBACK/AI_MODEL/HUMAN_VALIDATION, language, country_code, message_normalized, message_tokens, ambiguity_score = 1 - confianza, payload jsonb). `message_id` nullable SIN FK: el analisis es por turno de IA, no por fila de mensaje (el id del mensaje no viaja en `AgentConversationRequest`). Indices por business/created_at, conversation y detector_type. |
| `V99__ai_detected_intent.sql` | Tabla `ai_detected_intent` (FK a ai_message_analysis cascade y a ai_intent restrict, rank >= 1, unique (message_analysis_id, rank), is_primary, confidence 0-1, method_source, matched_expression). Indices de lookup y primario. |

### Java

| Archivo | Cambio |
|---|---|
| `IntentDetectionResult` | Nuevo campo `detectorSource` con constructor adicional de 7 args (el de 6 args asigna null; sin romper llamadas existentes). |
| `IntentDetectorService` | El retorno de BD usa `detectorSource="DATABASE"`; el resto queda null (= JAVA_FALLBACK). |
| `AiAgentJdbcRepository` | `insertMessageAnalysis(MessageAnalysisRecord)` (retorna UUID via `returning id`) e `insertDetectedIntent(UUID, DetectedIntentRecord)` (resuelve intent_id por code global de `ai_intent`). Records anidados `MessageAnalysisRecord` y `DetectedIntentRecord`. |
| `MessageAnalysisService` (nuevo) | Orquesta el registro: normaliza, calcula tokens y ambiguedad, mapea `AgentIntent` a code de catalogo (mapa inverso, 26 valores; AMBIGUOUS->UNKNOWN, BOOKING_REQUEST->BOOKING_CREATE, BOOKING_CHANGE->BOOKING_RESCHEDULE, etc.), registra analisis + intent primario (rank 1) y secundario (rank 2, confianza * 0.8). Traza `MESSAGE_ANALYSIS_RECORDED`. |
| `AgentCoordinatorService` | Constructor de 7 args `@Autowired` + constructor legacy de 6 args (messageAnalysisService = null, tests intactos). En `route()` registra el analisis dentro de `auditEnabled`, con guard null. Solo `route()` (flujo real), no `preview()`. |

## Verificacion

1. `mvn compile` + `mvn test-compile` OK (spotless:apply aplicado).
2. Suite `aiagents`: 313 tests, 48 fallos (misma base). No empeoro.
3. Prueba funcional en produccion local:
   - "quiero agendar una hora" -> analisis con `detector_type=DATABASE`, rank 1
     `BOOKING_CREATE` primary 0.9.
   - "quiero cambiar mi hora" -> `detector_type=JAVA_FALLBACK` (el catalogo
     JSON `taxonomia_conversacion:reprogramar` intercepta antes que la BD;
     correcto segun el orden: catalog safety -> negacion -> BD -> reglas Java),
     rank 1 `BOOKING_RESCHEDULE` 0.72, ambiguity 0.28, 4 tokens.

## Notas

- `aesthetic_intent_log` se mantiene como log LLM paralelo.
- El campo `detector_type` distingue DATABASE vs JAVA_FALLBACK; AI_MODEL y
  HUMAN_VALIDATION quedan reservados para fases futuras.
- Decidido durante la implementacion: un unico `MessageAnalysisService`
  (en lugar de MessageAnalysisService + DetectedIntentService del plan)
  porque el segundo no aportaba logica propia; el ranking se registra en el
  mismo servicio.
