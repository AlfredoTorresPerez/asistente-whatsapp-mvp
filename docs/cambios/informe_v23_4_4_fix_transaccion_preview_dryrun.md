# Informe técnico V23.4.4 - Corrección transacción readOnly en preview-ai

## Problema detectado

La versión V23.4.3 mostró mediante logs `[AI_TRACE]` que el flujo entendía correctamente el mensaje del cliente, resolvía sucursal y servicio, y encontraba disponibilidad real. Sin embargo, al intentar crear una reserva temporal desde `preview-ai`, PostgreSQL rechazaba el `INSERT` con:

```text
cannot execute INSERT in a read-only transaction
```

## Causa raíz

El endpoint `/api/v1/conversations/{conversationId}/preview-ai` está diseñado como vista previa y se ejecuta dentro de una transacción de solo lectura. La lógica del agente de agenda reutilizaba el mismo flujo transaccional real y, al encontrar disponibilidad, intentaba insertar una reserva temporal dentro de esa transacción de solo lectura.

## Corrección aplicada

Se agregó un modo explícito `dryRun` en `AgentConversationRequest`.

- `ConversationService.previewAiReply(...)` crea la solicitud con `dryRun=true`.
- `WhatsAppWebWebhookService` crea la solicitud real con `dryRun=false`.
- `BookingAgent` propaga `request.dryRun()` hacia `TransactionalAgendaBookingService`.
- `TransactionalAgendaBookingService` valida disponibilidad en modo dryRun, pero no crea reserva ni enlace.
- En modo real, registra `TEMPORARY_BOOKING_CREATE_STARTED`, crea la reserva temporal y luego genera el enlace.

## Flujo antes

```text
preview-ai
  -> disponibilidad=true
  -> createTemporaryBooking
  -> INSERT booking
  -> error read-only transaction
```

## Flujo después

```text
preview-ai dryRun=true
  -> disponibilidad=true
  -> TEMPORARY_BOOKING_DRY_RUN
  -> respuesta de vista previa sin INSERT
```

```text
whatsapp real dryRun=false
  -> disponibilidad=true
  -> TEMPORARY_BOOKING_CREATE_STARTED
  -> TEMPORARY_BOOKING_CREATED
  -> CONFIRMATION_LINK_CREATED
  -> respuesta final con enlace
```

## Archivos modificados

- `AgentConversationRequest.java`
- `ConversationService.java`
- `WhatsAppWebWebhookService.java`
- `BookingAgent.java`
- `TransactionalAgendaBookingService.java`
- `AestheticCenterService.java`
- `test_ia_negocio_conversacional_v23_4_4.ps1`

## Logs esperados

En preview:

```text
AVAILABILITY_CHECK_RESULT available=true
TEMPORARY_BOOKING_DRY_RUN
AI_FINAL_RESPONSE containsLink=false
```

En envío real:

```text
AVAILABILITY_CHECK_RESULT available=true
TEMPORARY_BOOKING_CREATE_STARTED
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
AI_FINAL_RESPONSE containsLink=true
```

## Cómo ejecutar

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_4.ps1
```

## Cómo revisar logs

```powershell
docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "AI_TRACE","TEMPORARY_BOOKING","CONFIRMATION_LINK","FLOW_ERROR"
```

## Riesgos y limitaciones

- `preview-ai` ya no genera enlace real; solo muestra disponibilidad o no disponibilidad.
- El enlace real debe generarse en el flujo real de WhatsApp o en un endpoint de envío real que use `dryRun=false`.
- No se ejecutó compilación Docker en este entorno. La validación final debe realizarse localmente.

## Validación pendiente

Validar en ambiente local:

1. Compilación Docker.
2. Backend healthy.
3. Preview no inserta booking.
4. Envío real sí inserta booking y confirmation link.
5. No aparece `cannot execute INSERT in a read-only transaction`.
