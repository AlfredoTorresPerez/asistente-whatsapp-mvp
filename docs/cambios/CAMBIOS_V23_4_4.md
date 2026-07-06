# Cambios V23.4.4 - Separación preview/dryRun y creación real de reserva

## Problema corregido

En V23.4.3 el flujo de `preview-ai` llegaba correctamente hasta:

- `AVAILABILITY_CHECK_RESULT available=true`

pero luego intentaba crear una reserva temporal dentro de una transacción de solo lectura, provocando el error PostgreSQL:

```text
cannot execute INSERT in a read-only transaction
```

## Solución aplicada

Se separaron explícitamente dos modos de ejecución:

1. **Modo preview/dryRun**
   - Se usa en `/api/v1/conversations/{conversationId}/preview-ai`.
   - Valida intención, entidades, sucursal, servicio y disponibilidad.
   - No crea `booking`.
   - No genera `confirmation_link` real.
   - Devuelve una respuesta de vista previa indicando que hay disponibilidad y que la reserva/enlace se crearán en el envío real.

2. **Modo real**
   - Se usa en el flujo real de WhatsApp entrante y en el flujo transaccional no marcado como dryRun.
   - Usa transacción escribible.
   - Crea reserva temporal.
   - Genera enlace de confirmación.
   - Mantiene logs de creación y errores.

## Logs agregados/reforzados

- `TEMPORARY_BOOKING_DRY_RUN`
- `TEMPORARY_BOOKING_CREATE_STARTED`
- `TEMPORARY_BOOKING_CREATED`
- `CONFIRMATION_LINK_CREATED`
- `FLOW_ERROR`

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentConversationRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebWebhookService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`
- `test_ia_negocio_conversacional_v23_4_4.ps1`

## Validación requerida

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

Luego revisar logs:

```powershell
docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "AI_TRACE","TEMPORARY_BOOKING","CONFIRMATION_LINK","FLOW_ERROR"
```

Y ejecutar prueba:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_4.ps1
```
