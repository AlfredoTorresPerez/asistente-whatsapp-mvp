# CAMBIOS - FASE 4: Resolucion por alias con scoring (ai_entity_alias -> ai_canonical_entity)

Fecha: 2026-07-31
Contexto: `docs/cambios/CAMBIOS_FASE3_CAPA_SEMANTICA_IA.md` (y anteriores de la capa semantica IA)

## Objetivo

Resolver entidades canonicas cuando el mensaje menciona un alias (no el
canonical_name exacto): "en provi" -> canonical "providencia". Con scoring
que combina confidence_base del alias, alias_type, longitud y calidad del
match, y trazabilidad del metodo (ALIAS) y del alias matcheado en
`ai_detected_entity`.

## Cambios

### Base de datos

Sin migraciones nuevas: la FASE 3 ya dejo `ai_entity_alias` con
`canonical_entity_id`, `alias_type`, `confidence_base` y ventanas de
vigencia (V101). La FASE 4 es Java puro sobre ese esquema.

### Java

| Archivo | Cambio |
|---|---|
| `CanonicalEntityJdbcRepository` | `findActiveAliases(UUID)` (nuevo): join `ai_entity_alias` con `ai_canonical_entity` activa (global o local), ventanas valid_from/valid_until respetadas y orden priority desc / longitud desc. Record `CanonicalAliasRecord`. Nota: las condiciones de ventana se agrupan con parentesis (la precedencia SQL de `or` sobre `and` las rompe). |
| `CanonicalEntityService` | Cache de aliases por negocio (mismo TTL `app.ai.agents.intent-catalog-cache-ttl-seconds`, fallback cache/vacio). `resolveAliases(businessId, normalizedText)`: aliases presentes como substring del mensaje -> lista `AliasMatch` ordenada por score desc (umbral 0.55). `resolveValueByAlias(businessId, value)`: valor extraido igual al alias (1.0), contenido (0.95) o contenido por el alias (0.9) -> mejor match. Score = confidence_base * factor alias_type (PREFERRED 1.0, SYNONYM 0.95, REGIONALISM 0.9, ORTHOGRAPHIC_ERROR 0.88, CONTEXTUAL 0.8) * factor longitud (1 token 0.73, 3+ tokens 1.0) * calidad del match * bonus 1.05 si es secuencia de tokens completos. Record `AliasMatch`. |
| `EntityExtractionService` | Nueva etapa `applyCanonicalAliases` entre `applyCanonicalEntities` y `applyDatabaseAliases` (el canonical exacto gana con putIfAbsent; los alias canonicos se aplican antes que el barrido generico de BD). Aplica cada AliasMatch por entity_key derivado y guarda claves auxiliares de trazabilidad: `<key>_canonical_id`, `<key>_resolution` (DATABASE/ALIAS), `<key>_matched_alias`, `<key>_confidence`. `applyCanonicalByType` tambien marca `<key>_canonical_id`/`_resolution` para consistencia. |
| `DetectedEntityService` | Orden de resolucion en `recordEntity`: (1) claves auxiliares de la extraccion (canonical_entity_id + ALIAS/DATABASE + matched_alias + confidence), (2) coincidencia exacta del valor contra canonical_name (DATABASE 0.85), (3) `resolveValueByAlias` (ALIAS con canonical_entity_id, matched_alias y confidence del scoring), (4) PATTERN sin canonical. |

## Verificacion

1. `mvn spotless:apply` + `mvn compile` OK.
2. Suite `aiagents`: 313 tests, 37 failures + 11 errors = 48 fallos (misma
   base). No empeoro.
3. Prueba funcional en produccion local (simulador WhatsApp, session
   `demo-sales`):
   - "quiero una hora en provi" -> `ai_detected_entity`:
     LOCATION sede "Providencia" con `resolution_method=ALIAS`,
     `matched_alias="provi"`, canonical_entity_id -> "providencia",
     confidence 0.5907 (0.85 * 0.95 SYNONYM * 0.73 longitud 1 token * 0.95
     texto mas largo * 1.05 token completo).
   - Regresion FASE 3: "quiero agendar una hora para depilacion laser esta
     semana" -> mismas 3 entidades (RELATIVE_DATE DATABASE con canonical,
     SERVICE y CONTACT PATTERN).

## Notas

- El scoring penaliza aliases cortos (1 token) de forma deliberada para
  evitar falsos positivos con palabras genericas; el umbral de aplicacion
  es 0.55.
- Las claves auxiliares `*_canonical_id`, `*_resolution`, `*_matched_alias`
  y `*_confidence` conviven en el mapa de entidades: no son rastreadas por
  `DetectedEntityService` como entidades, solo como metadata de resolucion,
  y quedan visibles en el payload de `ai_message_analysis` (entity_*).
- `resolveValueByAlias` tambien permite que la FASE 4 beneficie al registro
  aunque la extraccion no haya aplicado la etapa canonicas (vias de
  extraccion legacy).
