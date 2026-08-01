# Orquestador multiagente para WhatsApp

Este refactor agrega un módulo incremental `aiagents` sin romper la estructura existente del monolito ni el contrato local del canal WhatsApp (proveedor `SIMULATED` o `META_CLOUD_API`).

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

## Integración con el canal WhatsApp

El mensaje entrante llega por el canal nativo del backend:

- proveedor `SIMULATED`: `POST /api/v1/test/whatsapp-inbound` (local);
- proveedor `META_CLOUD_API`: webhook `POST /api/v1/integrations/whatsapp-cloud/webhook` firmado con `X-Hub-Signature-256`.

Flujo actual:

```text
canal (simulado o Cloud API)
  -> mensaje entrante
  -> persistencia de customer / conversation / inbound message
  -> análisis estético existente
  -> AgentCoordinatorService
  -> ai_conversation_context / ai_agent_decision_log / ai_agent_metric_daily
  -> human_handoff_request si aplica
  -> respuesta automática opcional
```

El canal es nativo del backend; no existe servicio Node externo ni sesión de dispositivo.

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

## Prueba local con el canal simulado

Levantar ambiente:

```bash
docker compose -f docker-compose.local.yml up --build
```

Simular mensaje entrante:

```bash
curl -i -X POST "http://localhost:8080/api/v1/test/whatsapp-inbound" \
  -H "Content-Type: application/json" \
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
curl -i -X POST "http://localhost:8080/api/v1/test/whatsapp-inbound" \
  -H "Content-Type: application/json" \
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
- No se modificó la verificación de firma `X-Hub-Signature-256` del webhook Cloud API.
- El canal no requiere servicio externo; la simulación local se hace con `POST /api/v1/test/whatsapp-inbound`.
- No se agregó LangChain4j ni pgvector aún para evitar cambios pesados en el MVP local.
- La detección inicial es por reglas determinísticas en español, preparada para reemplazo posterior por OpenAI/RAG.
- La respuesta automática queda detrás de feature flag.
