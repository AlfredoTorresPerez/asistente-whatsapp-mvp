# Informe técnico V23.4.5 — envío real desde preview IA

## Problema detectado

El flujo `preview-ai` fue corregido en V23.4.4 para no insertar reservas en transacciones de solo lectura. Sin embargo, cuando el usuario presionaba enviar, el sistema enviaba literalmente el texto de vista previa:

```text
Hay disponibilidad ... Esta es una vista previa: no se creó una reserva temporal ni un enlace real...
```

Eso no ejecutaba la creación real de reserva temporal ni la generación del enlace de confirmación.

## Causa raíz

El botón de envío terminaba llamando al endpoint estándar de mensajes con el `body` sugerido por `preview-ai`. El backend no distinguía que ese texto correspondía a una respuesta `dryRun`, por lo que lo enviaba como texto manual normal.

## Solución aplicada

Se agregó una detección de cuerpo de mensaje de vista previa IA en `ConversationService`.

Cuando el texto contiene la marca de vista previa, el backend:

1. carga el último mensaje entrante de la conversación;
2. ejecuta `AgentCoordinatorService.route(...)` con `dryRun=false`;
3. permite que `TransactionalAgendaBookingService` cree la reserva temporal;
4. permite que se genere el enlace de confirmación;
5. reemplaza el texto de vista previa por la respuesta final real;
6. envía el mensaje final por WhatsApp.

## Archivo modificado

```text
backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java
```

## Flujo antes

```text
preview-ai
  -> dryRun=true
  -> disponibilidad OK
  -> devuelve texto de vista previa
Enviar
  -> envía texto de vista previa
  -> no crea booking
  -> no genera enlace
```

## Flujo después

```text
preview-ai
  -> dryRun=true
  -> disponibilidad OK
  -> devuelve texto de vista previa
Enviar
  -> detecta texto de vista previa
  -> ejecuta IA real dryRun=false
  -> crea booking temporal
  -> genera confirmation link
  -> envía mensaje real con enlace
```

## Logs esperados

```text
AI_REAL_SEND_STARTED
TRANSACTIONAL_BOOKING_STARTED dryRun=false
AVAILABILITY_CHECK_RESULT available=true
TEMPORARY_BOOKING_CREATE_STARTED
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
AI_REAL_SEND_RESULT containsLink=true
WHATSAPP_RESPONSE_SEND_STARTED
WHATSAPP_RESPONSE_SEND_RESULT sent=true
```

## Riesgos

- Si el usuario edita demasiado el texto de vista previa, puede que el backend no lo reconozca como respuesta `dryRun`.
- Si el usuario envía dos veces sin idempotencia, podrían generarse reservas duplicadas. Se recomienda usar `idempotencyKey` desde el frontend.
- Si WhatsApp Web no está conectado, puede crearse la reserva y fallar el envío. El log lo dejará visible.

## Validación pendiente

No se ejecutó Docker Compose en este entorno. La validación final debe realizarse localmente con:

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

Luego probar desde la pantalla de conversación:

1. recibir mensaje de cliente con servicio + fecha + hora + sucursal;
2. presionar **Responder con IA**;
3. verificar que la vista previa no contiene enlace;
4. presionar **Enviar**;
5. verificar logs y mensaje final con enlace.
