# CAMBIOS V23.4.8 - Envio real desde vista previa de reserva

## Problema corregido

En V23.4.7 la vista previa de IA validaba correctamente la disponibilidad exacta y generaba:

- `TEMPORARY_BOOKING_DRY_RUN`
- `WHATSAPP_MESSAGE_FORMATTED type=BOOKING_PREVIEW`

Pero al presionar **Enviar**, el sistema enviaba el texto de la vista previa por WhatsApp sin reejecutar el flujo transaccional real. Por eso no aparecian:

- `AI_REAL_SEND_STARTED`
- `TRANSACTIONAL_BOOKING_STARTED dryRun=false`
- `TEMPORARY_BOOKING_CREATE_STARTED`
- `TEMPORARY_BOOKING_CREATED`
- `CONFIRMATION_LINK_CREATED`

## Causa raiz

La deteccion de vista previa seguia buscando el texto antiguo de V23.4.4/V23.4.5:

- `Esta es una vista previa`
- `al enviar la respuesta real`

Pero desde V23.4.6 el formato nuevo es:

- `👀 *Vista previa de reserva*`
- `Esta vista previa *no creo una reserva temporal ni un enlace real*.`
- `Al enviar la respuesta por WhatsApp se creara la reserva temporal y el enlace de confirmacion.`

Por esa razon el backend no reconocia que el mensaje era una vista previa transaccional.

## Correccion aplicada

Se actualizo la deteccion de `BOOKING_PREVIEW` en `ConversationService` para reconocer:

1. Formato legacy anterior.
2. Formato nuevo V23.4.6:
   - `vista previa de reserva`
   - `no creo una reserva temporal ni un enlace real`
   - `al enviar la respuesta por whatsapp se creara la reserva temporal`
3. Fallback por contenido:
   - `no creo una reserva temporal ni un enlace real`
   - `se creara la reserva temporal y el enlace de confirmacion`

Ademas, la respuesta de `preview-ai` agrega el sufijo `_BOOKING_PREVIEW` en `source` cuando el cuerpo corresponde a una vista previa de reserva. Esto deja metadata disponible para el frontend si se decide usarla.

## Comportamiento esperado

Al presionar **Enviar** sobre una respuesta de vista previa:

1. No se envia el texto de preview directamente.
2. Se detecta `BOOKING_PREVIEW`.
3. Se ejecuta `AI_REAL_SEND_STARTED`.
4. Se reejecuta el flujo con `dryRun=false`.
5. Se crea reserva temporal.
6. Se genera enlace de confirmacion.
7. Se formatea mensaje con `✅ *Reserva temporal creada*`.
8. Se envia WhatsApp con enlace real.

## Logs esperados

```text
AI_REAL_SEND_STARTED
TRANSACTIONAL_BOOKING_STARTED dryRun=false
EXACT_SLOT_VALIDATION_RESULT available=true
TEMPORARY_BOOKING_CREATE_STARTED
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
WHATSAPP_MESSAGE_FORMATTED type=TEMPORARY_BOOKING
AI_REAL_SEND_RESULT containsLink=true
WHATSAPP_RESPONSE_SEND_RESULT sent=true
```

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java`
- `test_ia_negocio_conversacional_v23_4_8.ps1`

## Validacion pendiente

No se declara 100% validado hasta ejecutar Docker Compose en ambiente local y confirmar los logs anteriores despues de presionar **Enviar**.
