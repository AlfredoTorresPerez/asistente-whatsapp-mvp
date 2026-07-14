# Debugging Auto-Reply Local: Flujo, Logs y Puntos de Fallo

## Resumen del Flujo End-to-End

```
┌─────────────────┐    ┌──────────────────┐    ┌───────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│  WhatsApp Web   │───▶│  whatsapp-web    │───▶│  Backend Java     │───▶│  channel_event_log   │───▶│  ai_reply_outbox    │───▶│  AiReplyOutboxProcessor│
│  (qr scan +     │    │  -service        │    │  Webhook          │    │  (INSERT)            │    │  (ENQUEUED)         │    │  (POLL + PROCESS)     │
│   incoming msg) │    │  (Node.js)       │    │  /api/v1/integ/   │    │                      │    │                     │    │                       │
└─────────────────┘    └──────────────────┘    │  whatsapp-web/    │    └───────────────────┘    └─────────────────────┘    └──────────┬──────────┘
                                               │  webhook          │                                                                    │
                                               └──────────────────┘                                                                    ▼
                                                                                                  ┌──────────────────────┐    ┌──────────────────────┐
                                                                                                  │ AgentCoordinatorService│───▶│ ChannelDispatchService│
                                                                                                  │ (Intent + Agent)       │    │ (WhatsAppWebAdapter)  │
                                                                                                  └──────────────────────┘    └──────────┬───────────┘
                                                                                                                                          │
                                                                                                                                          ▼
                                                                                                                               ┌──────────────────────┐
                                                                                                                               │ whatsapp-web-service │
                                                                                                                               │ /api/v1/messages/send│
                                                                                                                               └──────────────────────┘
```

## Configuración Local Requerida (.env.local)

```bash
# AI Agents - IMPORTANTE para local
APP_AI_AGENTS_ENABLED=true
APP_AI_AGENTS_AUTO_REPLY_ENABLED=true      # Encola respuestas en outbox
APP_AI_AGENTS_AUDIT_ENABLED=true           # Registra contexto, decisiones, métricas
APP_AI_AGENTS_SAFE_MODE_ENABLED=false      # FALSE = envío REAL a WhatsApp Web local
APP_AI_AGENTS_OUTBOX_WORKER_INTERVAL_MS=2000   # Polling cada 2s (vs 5s default)
APP_AI_AGENTS_OUTBOX_BATCH_SIZE=5
APP_OPENAI_ENABLED=false                   # Keyword-based OK para local
APP_WHATSAPP_WEB_DEMO_FALLBACK_ENABLED=false   # Ver fallos reales
```

## Endpoints Admin para Observabilidad

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/v1/ai/outbox/stats` | GET | `{pending, processing, failed, oldestAgeSeconds}` |
| `/api/v1/ai/preview` | POST | Preview IA sin persistir: `{message, conversationId?, ...}` → `AgentRoutingResult` |
| `/api/v1/whatsapp-web/status` | GET | Estado canal + QR + últimos eventos |
| `/api/v1/whatsapp-web/qr` | GET | QR con `expiresAt` y `lastQrAt` |

## Logs Estructurados Clave (correlationId end-to-end)

### 1. Webhook Reception
```
[AI_TRACE] step=MESSAGE_RECEIVED traceId=AI-xxxxxxxx conversationId=... layer=WhatsAppWebWebhookService messageSummary="Hola, quiero agendar..."
[AI_TRACE] step=CHANNEL_EVENT_LOG_INSERTED traceId=... conversationId=... layer=WhatsAppWebWebhookService deliveryId=...
```

### 2. Outbox Enqueue
```
[AI_TRACE] step=AI_OUTBOX_ENQUEUED traceId=... conversationId=... layer=WhatsAppWebWebhookService outboxId=... nextAttemptAt=...
```

### 3. Outbox Worker Poll
```
[AI_TRACE] step=AI_OUTBOX_WORKER_CLAIMED traceId=JOB-AI-OUTBOX-xxxxxxxx conversationId=null layer=AiReplyOutboxProcessor claimedJobs=1 batchSize=5
[AI_TRACE] step=AI_OUTBOX_WORKER_COMPLETED traceId=JOB-AI-OUTBOX-xxxxxxxx conversationId=null layer=AiReplyOutboxProcessor claimed=1 processed=1 failed=0 durationMs=245
```

### 4. Job Processing (por job)
```
[AI_TRACE] step=AI_ROUTE_STARTED traceId=AI-xxxxxxxx conversationId=... layer=AgentCoordinatorService channelAccountId=... customerId=... phoneMasked=569****54580 messageSummary="Hola, quiero agendar..."
[AI_TRACE] step=INTENT_DETECTED traceId=... conversationId=... layer=IntentDetectorService intent=BOOKING_REQUEST confidence=0.86 urgency=bajo
[AI_TRACE] step=ENTITIES_MERGED traceId=... conversationId=... layer=AgentCoordinatorService entities={servicio_o_producto: "limpieza facial", fecha_relativa: "mañana", hora: "10:00", sede: "providencia"}
[AI_TRACE] step=AGENT_SELECTED traceId=... conversationId=... layer=AgentCoordinatorService agent=BOOKING intent=BOOKING_REQUEST
[AI_TRACE] step=AI_FINAL_RESPONSE traceId=... conversationId=... layer=AgentCoordinatorService agent=BOOKING intent=BOOKING_REQUEST confidence=0.86 missing=[fecha_deseada, horario_preferido] responseSummary="Para agendar necesito confirmar..."
[AI_TRACE] step=WHATSAPP_RESPONSE_SEND_STARTED traceId=... conversationId=... layer=AiReplyOutboxProcessor phoneMasked=569****54580 messageLength=156 responseType=AI_AUTO_REPLY_OUTBOX
[AI_TRACE] step=WHATSAPP_RESPONSE_SEND_RESULT traceId=... conversationId=... messageId=... layer=AiReplyOutboxProcessor sent=true adapterStatus=SENT externalMessageIdMasked=wamid.xxxxx
```

### 5. Errores Comunes
```
[AI_TRACE] step=AI_OUTBOX_JOB_SKIPPED traceId=... conversationId=... layer=AiReplyOutboxProcessor reason=AI_AUTO_REPLY_DISABLED
[AI_TRACE] step=AI_OUTBOX_JOB_FAILED traceId=... conversationId=... layer=AiReplyOutboxProcessor attempt=1 maxAttempts=5 nextAttemptAt=... errorType=UnsupportedMessagingChannelException
[AI_TRACE] step=WHATSAPP_RESPONSE_SEND_RESULT traceId=... conversationId=... layer=AiReplyOutboxProcessor sent=false adapterStatus=FAILED error=AI_REPLY_OUTBOX_DISPATCH_FAILED
```

## Puntos de Fallo Comunes y Soluciones

### 1. Outbox worker no procesa (claimDueJobs retorna vacío)
**Síntomas**: `AI_OUTBOX_WORKER_IDLE` repetido, `pending` crece en `/api/v1/ai/outbox/stats`

**Causas**:
- `APP_AI_AGENTS_AUTO_REPLY_ENABLED=false` → Worker salta jobs con `AI_AUTO_REPLY_DISABLED`
- `APP_AI_AGENTS_ENABLED=false` → Igual que anterior
- Jobs tienen `next_attempt_at` en el futuro → Revisar `processingTimeoutMs` (default 120s)
- Tabla `ai_reply_outbox` vacía → Webhook no está encolando (ver logs `MESSAGE_RECEIVED`)

**Diagnóstico**:
```bash
# Ver stats
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/ai/outbox/stats

# Ver jobs en BD
docker exec -it asistente-postgres psql -U assistant -d asistente_whatsapp -c "SELECT id, status, attempts, next_attempt_at, trace_id FROM ai_reply_outbox ORDER BY created_at DESC LIMIT 10;"
```

### 2. Procesa pero no envía (safe mode + demo fallback)
**Síntomas**: `WHATSAPP_RESPONSE_SEND_RESULT sent=true adapterStatus=SIMULATED safeMode=true`

**Causas**:
- `APP_AI_AGENTS_SAFE_MODE_ENABLED=true` → Cambiar a `false` para envío real
- `APP_WHATSAPP_WEB_DEMO_FALLBACK_ENABLED=true` → Cambiar a `false` para ver errores reales del adapter

### 3. IntentDetectorService keyword-only (sin OpenAI)
**Síntomas**: Intents `AMBIGUOUS` o `GREETING` cuando debería ser `BOOKING_REQUEST`

**Solución**: Ajustar keywords en `IntentDetectorService.java` o habilitar `APP_OPENAI_ENABLED=true` con API key válida.

### 4. TransactionalAgendaBookingService falla silenciosamente
**Síntomas**: Respuesta IA genérica "No pude completar la reserva..."

**Verificaciones (ejecutar seed script)**:
```sql
-- Ejecutar scripts/seed-local-whatsapp-data.sql
-- Valida:
-- 1. business_location.whatsapp_number configurado en sedes activas
-- 2. aesthetic_service.requires_room, preparation_minutes, cleanup_minutes
-- 3. agenda_business_hours, agenda_professional_hours, agenda_room, agenda_room_service
-- 4. Canal WhatsApp existe en channel_account
```

### 5. Sin visibilidad: ¿se encoló? ¿procesó? ¿falló?
**Solución**: Usar endpoints admin
```bash
# Stats outbox
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/ai/outbox/stats

# Preview IA (sin side effects)
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"Hola quiero agendar limpieza facial mañana 10:00 Providencia","recipientPhone":"56950954580"}' \
  http://localhost:8080/api/v1/ai/preview

# Estado canal + QR
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/whatsapp-web/status
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/whatsapp-web/qr
```

## Scripts de Apoyo

| Script | Propósito |
|--------|-----------|
| `scripts/seed-local-whatsapp-data.sql` | Valida y siembra datos demo: sedes, servicios, horarios, cabinas, canal WhatsApp |
| `scripts/test-manual-auto-reply.ps1` | Test automatizado: webhook → poll outbox → verifica outbound_message |

## Checklist Rápido Pre-Test

- [ ] `.env.local` copiado con config AI explícita (ver arriba)
- [ ] `docker compose -f docker-compose.local.yml --profile dev-visual up -d`
- [ ] Backend compila: `cd backend-java && ./mvnw compile`
- [ ] QR escaneado y `whatsapp-web-service` health `runtimeReady=true`
- [ ] Canal WhatsApp `CONNECTED` en `/api/v1/whatsapp-web/status`
- [ ] Seed ejecutado: `psql -h localhost -p 5433 -U assistant -d asistente_whatsapp -f scripts/seed-local-whatsapp-data.sql`
- [ ] Test manual: `powershell -ExecutionPolicy Bypass -File scripts/test-manual-auto-reply.ps1`

## Comandos Útiles de Debug

```bash
# Ver logs outbox worker en tiempo real
docker logs -f asistente-backend-java | grep -E "AI_OUTBOX|AI_ROUTE|INTENT_DETECTED|WHATSAPP_RESPONSE"

# Ver channel_event_log
docker exec -it asistente-postgres psql -U assistant -d asistente_whatsapp -c "SELECT * FROM channel_event_log ORDER BY received_at DESC LIMIT 10;"

# Ver ai_reply_outbox
docker exec -it asistente-postgres psql -U assistant -d asistente_whatsapp -c "SELECT id, status, attempts, max_attempts, trace_id, next_attempt_at, created_at FROM ai_reply_outbox ORDER BY created_at DESC LIMIT 10;"

# Ver outbound_message
docker exec -it asistente-postgres psql -U assistant -d asistente_whatsapp -c "SELECT id, conversation_id, body, external_message_id, status, accepted_at FROM outbound_message ORDER BY created_at DESC LIMIT 10;"

# Reiniciar outbox worker (cambia interval)
docker compose -f docker-compose.local.yml restart backend-java
```