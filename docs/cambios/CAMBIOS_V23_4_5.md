# CAMBIOS V23.4.5 — envío real desde preview IA

## Objetivo

Corregir el flujo donde `preview-ai` valida disponibilidad en modo `dryRun`, pero al presionar enviar se mandaba el texto de vista previa sin crear reserva temporal ni enlace real.

## Problema corregido

En V23.4.4 el flujo de vista previa quedó correcto:

- `dryRun=true`
- no crea `booking`
- no genera enlace real
- no produce `cannot execute INSERT in a read-only transaction`

Pero el envío manual posterior reutilizaba ese mismo texto de vista previa y no ejecutaba el flujo real.

## Cambio aplicado

En `ConversationService.sendMessage(...)`, cuando el cuerpo del mensaje corresponde a una respuesta de vista previa de IA, el backend ejecuta nuevamente el orquestador en modo real:

- `dryRun=false`
- transacción escribible
- crea reserva temporal si hay disponibilidad
- genera enlace de confirmación
- reemplaza el texto de vista previa por la respuesta final real antes de enviar por WhatsApp

## Logs agregados

Se agregaron/reforzaron los siguientes pasos:

- `AI_REAL_SEND_STARTED`
- `AI_REAL_SEND_RESULT`
- `TEMPORARY_BOOKING_CREATE_STARTED`
- `TEMPORARY_BOOKING_CREATED`
- `CONFIRMATION_LINK_CREATED`
- `WHATSAPP_RESPONSE_SEND_STARTED`
- `WHATSAPP_RESPONSE_SEND_RESULT`
- `FLOW_ERROR`

## Archivo principal modificado

```text
backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java
```

## Validación esperada

1. Presionar **Responder con IA** debe seguir mostrando vista previa sin crear reserva.
2. Presionar **Enviar** con esa respuesta de vista previa debe ejecutar modo real.
3. Los logs deben mostrar `AI_REAL_SEND_STARTED`.
4. Si hay disponibilidad, debe aparecer `TEMPORARY_BOOKING_CREATED` y `CONFIRMATION_LINK_CREATED`.
5. El mensaje enviado por WhatsApp debe contener `/reservas/confirmar/`.
