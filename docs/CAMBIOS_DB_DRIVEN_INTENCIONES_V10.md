# Cambios V10 - IA conversacional guiada por base de datos

## Objetivo

Eliminar respuestas de negocio en codigo duro para las intenciones comerciales, agenda y pagos. La respuesta ahora se arma desde:

- `aesthetic_service`
- `aesthetic_service_category`
- `aesthetic_business_rule.rule_payload`
- `ai_entity_alias`

## Migracion agregada

- `backend-java/src/main/resources/db/migration/V14__ai_db_driven_intent_rules.sql`

Esta migracion crea:

- `ai_entity_alias`: tabla de aliases configurables para extraer entidades desde mensajes.
- Reglas `AI_*` en `aesthetic_business_rule` con plantillas editables desde base de datos.

## Archivos agregados

- `AiKnowledgeRepository.java`
- `AiBusinessKnowledgeService.java`
- `JdbcAiKnowledgeRepository.java`

## Archivos modificados

- `SalesAgent.java`
- `BookingAgent.java`
- `PaymentsAgent.java`
- `EntityExtractionService.java`
- `AiAgentIntentCoverageSimulationTest.java`

## Comportamiento esperado

- Consulta de servicios: lista opciones desde reglas/catalogo de base de datos.
- Consulta de precios: usa precio y duracion de `aesthetic_service`.
- Cotizacion: usa plantilla configurada en `aesthetic_business_rule`.
- Agenda: usa plantillas configuradas en base de datos.
- Cambio de cita: ya no pide servicio, pide identificacion de cita.
- Pago: si el cliente entrega solicitud y monto, pide solo metodo de pago.

## Ejecucion de simulacion

```powershell
cd C:\mvp\asistente-whatsapp-mvp-local-virtual-v10-db-driven-intenciones\asistente-whatsapp-mvp\backend-java
.\mvnw.cmd -Dtest=AiAgentIntentCoverageSimulationTest test
```

Reporte:

```text
backend-java\target\ai-intent-simulation\intent-coverage-report.md
```

## Nota tecnica

La simulacion unitaria usa un repositorio de conocimiento simulado para no depender de PostgreSQL durante el test unitario. En ejecucion real, Spring inyecta `JdbcAiKnowledgeRepository`, que consulta PostgreSQL.
