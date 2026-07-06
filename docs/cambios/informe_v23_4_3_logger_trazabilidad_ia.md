# Informe tecnico V23.4.3 - Logger transversal de trazabilidad IA

## Problema detectado
La version V23.4.2 ya resuelve el problema principal de contacto sin sucursal y mensaje con sucursal explicita, pero no entrega visibilidad suficiente para diagnosticar cada paso interno del flujo conversacional.

## Objetivo de trazabilidad
Permitir seguir un mensaje desde su ingreso por WhatsApp hasta la respuesta final, incluyendo normalizacion, intencion, entidades, resolucion de sucursal, resolucion de servicio, validacion de agenda, reserva temporal y enlace.

## Capas instrumentadas

- `WhatsAppWebWebhookService`: entrada WhatsApp y envio de respuesta.
- `ConversationService`: previsualizacion IA desde la pantalla de conversacion y despacho saliente.
- `AgentCoordinatorService`: orquestacion, contexto, intencion, agente y respuesta.
- `IntentDetectorService`: normalizacion y candidatos de intencion.
- `EntityExtractionService`: normalizacion y entidades extraidas.
- `BookingAgent`: validacion de datos minimos y respuesta de agenda.
- `TransactionalAgendaBookingService`: servicio, sucursal, agenda, reserva temporal y enlace.
- `application.yml`: nivel INFO para trazas.

## Ejemplo de logs

```text
[AI_TRACE] step=WHATSAPP_MESSAGE_RECEIVED traceId=WA-12345678 conversationId=- messageId=- layer=WhatsAppWebWebhookService rawText=Hola, quiero reservar...
[AI_TRACE] step=INTENT_DETECTED traceId=WA-12345678 conversationId=... layer=AgentCoordinatorService intent=BOOKING_REQUEST confidence=0.86
[AI_TRACE] step=EFFECTIVE_LOCATION_RESOLVED traceId=WA-12345678 conversationId=... layer=BookingAgent locationName=Providencia source=MESSAGE_TEXT
[AI_TRACE] step=CONFIRMATION_LINK_CREATED traceId=WA-12345678 conversationId=... layer=TransactionalAgendaBookingService tokenMasked=abcdef...wxyz
```

## Como filtrar logs

```powershell
docker compose -f docker-compose.local.yml logs -f backend-java
```

```powershell
docker compose -f docker-compose.local.yml logs --tail=1000 backend-java | Select-String -Pattern "AI_TRACE"
```

## Como seguir un traceId

1. Localizar `traceId` en `WHATSAPP_MESSAGE_RECEIVED` o `PREVIEW_AI_REQUEST_RECEIVED`.
2. Filtrar por ese valor.
3. Revisar los pasos sucesivos hasta `AI_FINAL_RESPONSE` y `WHATSAPP_RESPONSE_SEND_RESULT`.

## Datos sensibles enmascarados

- Telefonos con `AiTraceLogger.maskPhone`.
- Tokens con `AiTraceLogger.maskToken`.
- URLs de confirmacion sanitizadas en `sanitizeText`.
- No se registran cabeceras `Authorization` ni credenciales.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AiTraceLogger.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentConversationRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentCoordinatorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/IntentDetectorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/EntityExtractionService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebWebhookService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`
- `backend-java/src/main/resources/application.yml`
- `test_ia_negocio_conversacional_v23_4_3.ps1`

## Riesgos

- Mayor volumen de logs en consola.
- Si se usa en produccion, conviene dejar el nivel INFO solo mientras se diagnostica.
- Algunos flujos de pantalla pueden tener traceId distinto al flujo WhatsApp real porque se ejecutan como previsualizacion.

## Limitaciones

- La version no cambia reglas de negocio.
- No garantiza envio por WhatsApp si el adaptador WhatsApp Web no esta conectado.
- No se valida compilacion completa en este entorno.

## Como ejecutar prueba

```powershell
docker compose -f docker-compose.local.yml up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_3.ps1
```

## Validacion pendiente
No se pudo ejecutar Docker Compose ni Maven en este entorno. Se debe validar localmente con Docker y revisar que el backend quede `Healthy`.
