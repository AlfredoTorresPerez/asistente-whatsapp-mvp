# ANÁLISIS PREVIO — CAPA SEMÁNTICA NORMALIZADA DE IA CONVERSACIONAL

Fecha: 2026-07-31
Alcance: backend-java (Java 21, Spring Boot 3.5.14, PostgreSQL 16, Flyway V1..V95)
Regla principal respetada: NO se reemplaza la arquitectura transaccional de agenda/reservas.

---

## 1. Componentes encontrados (estado actual)

### 1.1 Mecanismos de detección de intención — 3 paralelos

| # | Mecanismo | Ubicación | Uso actual | Observaciones |
|---|---|---|---|---|
| 1 | Reglas Java hardcodeadas | `aiagents/application/IntentDetectorService.java` (~400 frases en 25 listas + regex + heurísticas) | PRINCIPAL (bloqueante) | `normalize()` corrige typos (reserbar→reservar, ajendar→agendar) |
| 2 | Catálogo JSON | `resources/conversation/intents.json` (42 intents, ~250 frases) vía `ConversationSpecCatalog` | Parcial | Solo pasan 4 intents: AMBIGUOUS, BOOKING_CHANGE, HUMAN_REQUEST, COMPLAINT (`shouldUseCatalogIntent`, IntentDetectorService:501-505) |
| 3 | LLM (OpenAI) | `aesthetic/application/AestheticCenterService.analyzeInboundMessage()` + `OpenAiIntentClient` → `aesthetic_intent_log` | Paralelo, no bloqueante | Invocado desde `AiReplyOutboxProcessor:145` antes del routing |

### 1.2 Detección de entidades

- `EntityExtractionService` (aiagents/application): extrae hora, fecha, nombre, monto, y **aliases de BD** vía `applyDatabaseAliases()` (líneas 105-113) leyendo `ai_entity_alias`.
- Datos operativos reales: `aesthetic_service`, `aesthetic_professional`, `agenda_room`, `business_location` (sucursales).

### 1.3 Orquestación

- `AgentCoordinatorService.route()` (líneas 63-127): guardas → contexto previo (`ai_conversation_context`) → detect → extraer entidades → merge contexto → resolver intención con contexto (`resolveContextAwareIntent`, 376-473) → `AgentRegistry.resolve()` → handler → persistir (`upsertConversationContext`, `insertDecisionLog`, `incrementMetric`, `insertHumanHandoff`) → responder.
- `AgentRegistry.resolveAgentType()`: mapa AgentIntent → AgentType (RECEPTION/SALES/BOOKING/PAYMENTS/SUPPORT/KNOWLEDGE/FOLLOW_UP/HUMAN_HANDOFF).
- `AgentIntent` (enum, 26 valores): GREETING, THANKS_OR_FAREWELL, COMMERCIAL_INQUIRY, SERVICE_INFORMATION, SERVICE_RECOMMENDATION, AVAILABILITY_QUERY, PROFESSIONAL_QUERY, LOCATION_QUERY, BUSINESS_HOURS_QUERY, PRICE_REQUEST, QUOTE_REQUEST, BOOKING_REQUEST, BOOKING_CHANGE, BOOKING_CANCEL, BOOKING_STATUS, PAYMENT_INQUIRY, PAYMENT_PROBLEM, SUPPORT_GENERAL, TECHNICAL_MESSAGE, KNOWLEDGE_QUERY, FOLLOW_UP, COMPLAINT, HUMAN_REQUEST, AMBIGUOUS, COMMERCIAL_AND_BOOKING, WAITLIST_QUERY.

### 1.4 Persistencia IA existente

| Tabla | Migración | Estado | Uso Java |
|---|---|---|---|
| `intencion_expresion` | V48 (112 expresiones, 4 intenciones canónicas) | **HUÉRFANA — no la lee ninguna clase Java** | Ninguno |
| `ai_entity_alias` | V14, V21-V26 (alias, entity_key, entity_value, priority) | Activa | `JdbcAiKnowledgeRepository.findActiveEntityAliases()` |
| `ai_agent_decision_log` | V11 + V94 (source, reviewed*, + tabla `ai_decision_review`) | Activa | Solo INSERT (`insertDecisionLog`) |
| `ai_conversation_context` | V11 + V79 (conversation_state, state_payload) | Activa | Upsert + snapshot de 5 columnas |
| `aesthetic_intent_log` | V7 (log LLM por mensaje: source_message, intent, confidence, entities, model_name) | Activa | `AestheticCenterService` (OpenAI) |
| `ai_prompt_template` | V22 + V93 | Documentada como no leída en runtime; V93 la reactiva | V93 la puebla; handlers usan `aesthetic_business_rule` |
| `business_ai_settings` | V93 (mode, language, escalation_threshold, allow_*) | Activa | `BusinessAiSettingsService` (`isBusinessAiActive`, `autoReplyEnabled`) |
| `ai_agent_metric_daily` | V11 | Activa | `incrementMetric` |
| `human_handoff_request` | V11 | Activa | condicional |

### 1.5 Módulo aiagents

- Sin JPA: 100% JDBC (`JdbcTemplate`/`NamedParameterJdbcTemplate`) + RowMapper lambda + records en paquete `api`.
- Paquetes: `api` (AiAdminController: GET /api/v1/ai/outbox/stats, POST /api/v1/ai/preview), `application` (servicios concretos @Service), `domain` (enums), `infrastructure` (repositorios @Repository).

---

## 2. Tablas existentes reutilizables (NO duplicar)

| Tabla | Reutilización en la capa semántica |
|---|---|
| `intencion_expresion` | Fuente de migración de datos hacia `ai_intent_expression` (112 frases, business demo `11111111-...`) |
| `ai_entity_alias` | Fuente de migración hacia `ai_canonical_entity` + sinónimos; se extiende con columnas nuevas (no se elimina) |
| `ai_agent_decision_log` | Se EXTENDE (PASO 9) en lugar de crear tabla de acciones nueva: message_id, analysis_id, detected_intent_id, action_type, action_status, idempotency_key, started_at, completed_at, error_code, error_detail, result_reference_type/id, detection_source, matched_expression |
| `ai_conversation_context` | Se mantiene intacta; la capa semántica se relaciona por conversation_id |
| `aesthetic_intent_log` | Se mantiene (log LLM paralelo); `ai_message_analysis` lo complementa como análisis canónico del motor |
| `business_ai_settings` | Configuración por negocio existente (language, escalation) — la capa semántica la respeta |
| `message` | FK de `ai_message_analysis` (message_id) — análisis 1:1 por mensaje |
| `aesthetic_service`, `aesthetic_professional`, `agenda_room`, `business_location` | Destinos `reference_type/reference_id` de `ai_canonical_entity` |

---

## 3. Clases que deberán modificarse (plan)

### Fase 1 (catálogos + expresiones)
- `IntentDetectorService` — inyectar repositorio de expresiones; consultar BD primero, reglas Java como respaldo; registrar source (DATABASE/JAVA_FALLBACK).
- `AiKnowledgeRepository` (interfaz) + `JdbcAiKnowledgeRepository` — nuevo `findActiveIntentExpressions()`.
- Nuevos: `aiagents/application/IntentCatalogService`, `IntentExpressionService` (caché ConcurrentHashMap + TTL), records de DTO.

### Fase 2 (análisis por mensaje)
- `AgentCoordinatorService.route()` — crear `ai_message_analysis` por mensaje entrante y registrar `ai_detected_intent` (ranking, primary).
- Nuevos: `MessageAnalysisService`, `DetectedIntentService`.

### Fase 3 (entidades canónicas)
- `EntityExtractionService` — resolución contra `ai_canonical_entity`/`ai_entity_alias` evolucionado.
- Nuevos: `CanonicalEntityService`, `EntityResolutionService`, `DetectedEntityService`.

### Fase 4 (decisiones + admin)
- `AiAgentJdbcRepository.insertDecisionLog` — extender con columnas nuevas.
- `AiAdminController` + frontend `Administración → Inteligencia conversacional`.

---

## 4. Riesgos identificados

1. **Duplicación funcional existente**: 3 mecanismos de intención (Java, JSON, LLM) + `intencion_expresion` huérfana. La capa nueva debe orquestarlos sin duplicar.
2. `intencion_expresion` sin UNIQUE sobre (business_id, expresion) y sin updated_at → la migración debe deduplicar (existen typos y frases repetidas).
3. `ai_entity_alias.entity_value` mezcla entidades y "intenciones" (`entity_key='intencion'` en V23) → la migración a canónicas debe separar dominios.
4. `ai_agent_decision_log` sin unique de idempotencia → reintentos pueden duplicar decisiones (mitigado por V16 en message + `booking_operation_idempotency` V86; la capa nueva debe preservar idempotency).
5. Cambiar `IntentDetectorService` sin romper `route()` (detección de cancelar/reprogramar sensible) — se mantiene el flujo de confirmación existente.
6. Los 49 fallos de tests `aiagents` preexistentes (conocidos) no deben empeorar.
7. `minimum_confidence` 0-1 con numeric(5,4); `intents.json` usa confianza 0.72-0.90.
8. `ai_intent` con business_id NULL (global) requiere unique parcial en PostgreSQL para cumplir "única por business_id y code".

---

## 5. Migraciones nuevas requeridas (siguiente número: V96)

| Versión | Contenido |
|---|---|
| V96 | `ai_intent` (catálogo) + seed de intenciones (16 requeridas + extras para cobertura 1:1 con AgentIntent: COMMERCIAL_INQUIRY, SERVICE_RECOMMENDATION, PROFESSIONAL_QUERY, QUOTE_REQUEST, PAYMENT_PROBLEM, SUPPORT_GENERAL, TECHNICAL_MESSAGE, KNOWLEDGE_QUERY, FOLLOW_UP, COMPLAINT, WAITLIST_QUERY) |
| V97 | `ai_intent_expression` + migración progresiva desde `intencion_expresion` (ON CONFLICT, dedup) + seed de expresiones chilenas (BOOKING_CREATE/RESCHEDULE/CANCEL) |
| V98 | `ai_message_analysis` + índices/constraints |
| V99 | `ai_detected_intent` + índices/constraints |
| V100 | `ai_canonical_entity` + migración progresiva desde `ai_entity_alias` (entity_key=servicio/sede/fecha/preferencia) |
| V101 | Evolución `ai_entity_alias` (columnas nuevas: canonical_entity_id, normalized_alias, language, country_code, alias_type, confidence_base, active, valid_from, valid_until) |
| V102 | `ai_detected_entity` + índices/constraints |
| V103 | Extensión `ai_agent_decision_log` (14 columnas nuevas del PASO 9) |

Todas idempotentes (`create table if not exists`, `add column if not exists`, `on conflict`), con `comment on table/column` (patrón V93), FKs con naming `fk_/uq_/chk_/idx_`.

## 6. Compatibilidad con datos existentes

- Se conservan: `intencion_expresion`, `ai_entity_alias` (solo se añaden columnas), `ai_agent_decision_log` (solo se añaden columnas), `IntentDetectorService`, `AgentIntent`, `intents.json`.
- `ai_intent` se siembra con las 26 intenciones mapeadas desde `AgentIntent` (códigos en mayúsculas), con `business_id` NULL (global) + version 1.
- Las 112 expresiones de `intencion_expresion` migran a `ai_intent_expression` con `expression_type=COMPLETE_PHRASE` (o SYNONYM para typos), business demo, prioridad preservada.
- El detector usa DATABASE primero; si no hay match, JAVA_FALLBACK; el registro `ai_message_analysis.detector_type` refleja la fuente. Cero cambios en agenda/reservas.

---

## 7. Mapeo AgentIntent → ai_intent (cobertura 1:1)

| AgentIntent | code ai_intent |
|---|---|
| GREETING | GREETING |
| THANKS_OR_FAREWELL | THANKS o GOODBYE (según contexto: "gracias"→THANKS, "chao"→GOODBYE) |
| COMMERCIAL_INQUIRY | COMMERCIAL_INQUIRY |
| SERVICE_INFORMATION | SERVICE_INFORMATION |
| SERVICE_RECOMMENDATION | SERVICE_RECOMMENDATION |
| AVAILABILITY_QUERY | BOOKING_AVAILABILITY |
| PROFESSIONAL_QUERY | PROFESSIONAL_QUERY |
| LOCATION_QUERY | BUSINESS_LOCATION |
| BUSINESS_HOURS_QUERY | BUSINESS_HOURS |
| PRICE_REQUEST | SERVICE_PRICE |
| QUOTE_REQUEST | QUOTE_REQUEST |
| BOOKING_REQUEST / COMMERCIAL_AND_BOOKING | BOOKING_CREATE |
| BOOKING_CHANGE | BOOKING_RESCHEDULE |
| BOOKING_CANCEL | BOOKING_CANCEL |
| BOOKING_STATUS | BOOKING_STATUS |
| PAYMENT_INQUIRY | PAYMENT_INFORMATION |
| PAYMENT_PROBLEM | PAYMENT_PROBLEM |
| SUPPORT_GENERAL | SUPPORT_GENERAL |
| TECHNICAL_MESSAGE | TECHNICAL_MESSAGE |
| KNOWLEDGE_QUERY | KNOWLEDGE_QUERY |
| FOLLOW_UP | FOLLOW_UP |
| COMPLAINT | COMPLAINT |
| HUMAN_REQUEST | HUMAN_REQUEST |
| AMBIGUOUS | UNKNOWN |
| WAITLIST_QUERY | WAITLIST_QUERY |

(Se agregan 11 codes extras a los 16 requeridos para no perder trazabilidad con el enum existente.)
