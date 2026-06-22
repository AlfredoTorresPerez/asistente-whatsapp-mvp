# Orquestador multiagente para WhatsApp

Este refactor agrega un módulo incremental `aiagents` sin romper la estructura existente del monolito ni el contrato local con `whatsapp-web-service`.

## Objetivo

Convertir cada mensaje entrante de WhatsApp en una decisión operativa trazable:

1. detectar intención;
2. extraer entidades básicas;
3. seleccionar un único subagente;
4. guardar contexto conversacional;
5. registrar métricas;
6. crear derivación humana cuando corresponda;
7. opcionalmente enviar una respuesta automática por el canal WhatsApp configurado.

## Subagentes incluidos

- `RECEPTION`: saludos, mensajes ambiguos y captura inicial.
- `SALES`: consultas comerciales, precios y cotizaciones.
- `BOOKING`: reservas, cambios y cancelaciones.
- `SUPPORT`: dudas operativas y problemas simples.
- `PAYMENTS`: pagos, facturas, comprobantes y cobros.
- `FOLLOW_UP`: recuperación de leads, recordatorios y oportunidades pendientes.
- `KNOWLEDGE`: documentos, políticas, FAQ y catálogo autorizado.
- `HUMAN_HANDOFF`: reclamos, urgencias, solicitudes humanas y casos sensibles.

## Integración con WhatsApp Web local

El punto de integración se mantiene en:

```text
backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebWebhookService.java
```

Flujo actual:

```text
whatsapp-web-service
  -> webhook MESSAGE_RECEIVED
  -> persistencia de customer / conversation / inbound message
  -> análisis estético existente
  -> AgentCoordinatorService
  -> ai_conversation_context / ai_agent_decision_log / ai_agent_metric_daily
  -> human_handoff_request si aplica
  -> respuesta automática opcional
```

El adaptador Node `whatsapp-web-service` no fue reemplazado ni renombrado.

## Variables de entorno

```bash
APP_AI_AGENTS_ENABLED=true
APP_AI_AGENTS_AUTO_REPLY_ENABLED=false
APP_AI_AGENTS_AUDIT_ENABLED=true
APP_AI_AGENTS_DEFAULT_CONFIDENCE=0.78
```

Por seguridad, `APP_AI_AGENTS_AUTO_REPLY_ENABLED` queda en `false`. Con ese valor el sistema clasifica y registra la decisión, pero no envía mensajes automáticos al cliente.

Para probar respuesta automática en local:

```bash
APP_AI_AGENTS_AUTO_REPLY_ENABLED=true
```

## Tablas agregadas

Migración:

```text
backend-java/src/main/resources/db/migration/V11__ai_agents_orchestration.sql
```

Tablas:

- `ai_conversation_context`
- `ai_agent_decision_log`
- `human_handoff_request`
- `ai_agent_metric_daily`

## Prueba local con whatsapp-web-service

Levantar ambiente:

```bash
docker compose -f docker-compose.local.yml up --build
```

Simular mensaje entrante:

```bash
curl -i -X POST "http://localhost:3001/api/v1/messages/simulate-inbound" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-whatsapp-web-key" \
  -d '{"from":"56911112222","body":"Hola, quiero saber el precio y agendar para mañana"}'
```

Validar en base de datos:

```sql
select primary_intent, agent_type, response_to_customer, created_at
from ai_agent_decision_log
order by created_at desc
limit 5;

select active_agent, primary_intent, extracted_data, missing_data, requires_human
from ai_conversation_context
order by updated_at desc
limit 5;
```

Probar derivación humana:

```bash
curl -i -X POST "http://localhost:3001/api/v1/messages/simulate-inbound" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-whatsapp-web-key" \
  -d '{"from":"56911112222","body":"Estoy molesto, quiero hablar con un ejecutivo ahora"}'
```

Validar:

```sql
select urgency, reason, status, summary, created_at
from human_handoff_request
order by created_at desc
limit 5;
```

## Diseño técnico

Paquete principal:

```text
com.asistentewhatsapp.aiagents
```

Estructura:

```text
aiagents/
  domain/
    AgentIntent
    AgentType
  application/
    AgentCoordinatorService
    AgentRegistry
    IntentDetectorService
    EntityExtractionService
    ReceptionAgent
    SalesAgent
    BookingAgent
    SupportAgent
    PaymentsAgent
    FollowUpAgent
    KnowledgeAgent
    HumanHandoffAgent
  infrastructure/
    AiAgentJdbcRepository
```

## Decisiones de refactor

- No se eliminó el módulo `aesthetic`; se mantiene y convive con el nuevo coordinador.
- No se modificó el contrato de headers HMAC del webhook local.
- No se renombró `whatsapp-web-service`.
- No se agregó LangChain4j ni pgvector aún para evitar cambios pesados en el MVP local.
- La detección inicial es por reglas determinísticas en español, preparada para reemplazo posterior por OpenAI/RAG.
- La respuesta automática queda detrás de feature flag.
