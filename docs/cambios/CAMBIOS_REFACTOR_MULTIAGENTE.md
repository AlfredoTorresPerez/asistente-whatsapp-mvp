# Cambios aplicados: refactor multiagente compatible con whatsapp-web-service

## Resumen

Se agregó un módulo incremental de orquestación multiagente sin romper la estructura actual ni el contrato local con `whatsapp-web-service`.

## Archivos principales agregados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/**`
- `backend-java/src/main/resources/db/migration/V11__ai_agents_orchestration.sql`
- `docs/AI_AGENTS_ORCHESTRATION.md`

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebWebhookService.java`
- `backend-java/src/main/resources/application.yml`
- `docker-compose.yml`
- `docker-compose.local.yml`
- `docker-compose.prod.yml`
- `backend-java/.env.example`
- `.env.local.example`
- `.env.production.example`
- `README.md`

## Compatibilidad local

- Se mantiene `whatsapp-web-service`.
- Se mantienen los endpoints existentes del adaptador Node.
- Se mantiene la firma HMAC del webhook.
- La respuesta automática queda desactivada por defecto:
  `APP_AI_AGENTS_AUTO_REPLY_ENABLED=false`.

## Validación realizada

- Se hizo una compilación sintáctica aislada del nuevo paquete `aiagents` con stubs mínimos.
- Se hizo una compilación sintáctica aislada de `WhatsAppWebWebhookService` con stubs mínimos.
- No pude ejecutar Maven real porque el wrapper intentó descargar Maven desde `repo.maven.apache.org` y el entorno no tiene acceso de red para esa descarga.

## Próxima validación recomendada en tu máquina

```bash
docker compose -f docker-compose.local.yml up --build
```

Luego:

```bash
curl -i -X POST "http://localhost:3001/api/v1/messages/simulate-inbound" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-whatsapp-web-key" \
  -d '{"from":"56911112222","body":"Hola, quiero saber el precio y agendar para mañana"}'
```

Y revisar:

```sql
select primary_intent, agent_type, response_to_customer, created_at
from ai_agent_decision_log
order by created_at desc
limit 5;
```
