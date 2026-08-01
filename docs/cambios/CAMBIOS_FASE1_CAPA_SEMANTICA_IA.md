# CAMBIOS - FASE 1: Capa semantica de IA (catalogo de intenciones y expresiones)

Fecha: 2026-07-31
Contexto: `docs/cambios/ANALISIS_PREVIO_CAPA_SEMANTICA_IA.md`

## Objetivo

Crear el catalogo normalizado de intenciones (`ai_intent`) y expresiones
(`ai_intent_expression`) y hacer que la deteccion de intenciones los consulte
primero, manteniendo las reglas Java como respaldo (fallback) sin romper el
flujo de agenda existente.

## Cambios

### Base de datos (Flyway)

| Migracion | Contenido |
|---|---|
| `V96__ai_intent_catalog.sql` | Tabla `ai_intent` (business_id NULL = global), unique parcial por `code`, checks (code mayusculas, confidence 0-1, priority 1-999), 27 intenciones globales sembradas (16 requeridas + 11 extras para cobertura 1:1 con `AgentIntent`). |
| `V97__ai_intent_expression_catalog.sql` | Tabla `ai_intent_expression` (7 tipos de expresion, unique parcial por intent_id/business_id/expression_normalized, indices de lookup por idioma/pais/activo). Migra las 112 expresiones de `intencion_expresion` (legado intacto) con mapeo `reservar->BOOKING_CREATE`, `reprogramar_reserva->BOOKING_RESCHEDULE`, `cancelar_reserva->BOOKING_CANCEL`, `consultar_reservas->BOOKING_STATUS`. Siembra 28 expresiones chilenas para BOOKING_CREATE/RESCHEDULE/CANCEL (incluye `ajendar una hora` como ORTHOGRAPHIC_ERROR, `tendran hora`/`hay cupo` como REGIONALISM). |

Ambas validan sintaxis en transaccion rollback y se aplicaron en BD local
(ahora en version 97: 27 intents, 140 expresiones).

### Java

| Archivo | Cambio |
|---|---|
| `AiKnowledgeRepository` | Nuevo record `IntentExpression(code, expressionNormalized, expressionType, priority, confidenceBase, requiresHuman, minimumConfidence)` y metodo `findActiveIntentExpressions(UUID businessId)` (global + por negocio, ventanas valid_from/valid_until, join con `ai_intent` activo). |
| `JdbcAiKnowledgeRepository` | Implementacion SQL del metodo. |
| `IntentExpressionService` (nuevo) | Cache local `ConcurrentHashMap` por businessId con TTL (propiedad `app.ai.agents.intent-catalog-cache-ttl-seconds`, default 300). Degrada a cache previa o vacio si la BD falla; expone `invalidate`/`invalidateAll` para la administracion futura. |
| `AiAgentProperties` | Nueva propiedad `intentCatalogCacheTtlSeconds` (default 300). |
| `IntentDetectorService` | Constructor Spring con `IntentExpressionService` (los constructores sin BD siguen para tests). En `detect()` consulta el catalogo BD tras la negacion explicita y antes de las reglas Java. Matcheo: `ORTHOGRAPHIC_ERROR` contra texto normalizado sin corregir typos; el resto contra texto con typos corregidos. Se descarta si `confidenceBase < minimumConfidence` del intent. Traza `INTENT_DB_CATALOG` con `source=DATABASE`. Mapeo de codes de BD a `AgentIntent` (BOOKING_RESCHEDULE->BOOKING_CHANGE, SERVICE_PRICE->PRICE_REQUEST, etc.). |
| Tests | 7 stubs `implements AiKnowledgeRepository` actualizados con `findActiveIntentExpressions` -> `List.of()`. |

## Verificacion

1. `mvn compile` y `mvn test-compile` OK. `mvn spotless:apply` aplicado.
2. Suite `aiagents`: 313 tests, 48 fallos (linea base previa: 49). No se
   empeoro; los tests usan el constructor sin BD (codigo identico al previo).
3. Prueba funcional en produccion local (simulador
   `POST /api/v1/test/whatsapp-inbound`):
   - "quiero agendar una hora" -> `INTENT_DB_CATALOG BOOKING_REQUEST COMPLETE_PHRASE 0.9 source=DATABASE`
   - "quiero ajendar una hora" -> BOOKING_REQUEST 0.9 (typo corregido por `normalize()`)
   - "no podre asistir" -> `BOOKING_CANCEL 0.85 source=DATABASE` (caso nuevo no cubierto por reglas Java)
   - "me gustaria una cotizacion de depilacion laser" -> QUOTE_REQUEST 0.88 (fallback Java intacto)
4. Agenda transaccional no modificada.

## Notas

- `intencion_expresion` se conserva como legado sin reemplazarse.
- El admin demo local (`admin@demo.cl`) quedo LOCKED durante las pruebas de
  login y se desbloqueo via SQL (`status='ACTIVE'`, `failed_login_attempts=0`).
