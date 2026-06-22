# v23.4.23 - Respuestas reales alineadas con matriz y sucursal visible

## Diagnóstico

La matriz de pruebas ejecuta el orquestador IA directamente, usando datos controlados y `dryRun=false` para casos transaccionales. En el flujo real desde WhatsApp existían dos diferencias operativas:

1. En `docker-compose.local.yml`, `APP_AI_AGENTS_AUTO_REPLY_ENABLED` estaba en `false`, por lo que el cliente no recibía automáticamente la misma respuesta que valida la matriz. El panel generaba una vista previa (`dryRun=true`) y el operador debía enviarla manualmente.
2. La sede escrita por el cliente, por ejemplo `Providencia`, se detectaba como entidad de agenda, pero no siempre se persistía en la conversación. Por eso el panel podía seguir mostrando `Sin sucursal` aunque la IA hubiera entendido la sede.

## Cambios aplicados

- Se activó `APP_AI_AGENTS_AUTO_REPLY_ENABLED=true` en el entorno local para que el flujo cliente real use el mismo orquestador que la matriz.
- Al recibir un mensaje entrante desde WhatsApp Web, el backend intenta asociar automáticamente la conversación a una sede activa si el texto menciona el nombre, código o comuna de la sede.
- La vista previa IA también intenta persistir la sede detectada a partir de la entidad `sede` o del último mensaje del cliente.
- Se agregaron trazas `CONVERSATION_LOCATION_ASSIGNED` para auditar cuándo una conversación deja de estar `Sin sucursal`.

## Resultado esperado

Si el cliente envía:

```text
Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 16:00 en Providencia.
```

El flujo real debería:

1. Detectar intención de agenda/reserva.
2. Extraer servicio, fecha, hora y sede.
3. Asociar la conversación a `Providencia`.
4. Responder automáticamente desde WhatsApp con reserva temporal y enlace real de confirmación.

## Logs esperados

```text
CONVERSATION_LOCATION_ASSIGNED source=INBOUND_MESSAGE locationName=Providencia
AI_ROUTE_STARTED
TRANSACTIONAL_BOOKING_STARTED dryRun=false
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
WHATSAPP_RESPONSE_SEND_STARTED responseType=AI_AUTO_REPLY
```
