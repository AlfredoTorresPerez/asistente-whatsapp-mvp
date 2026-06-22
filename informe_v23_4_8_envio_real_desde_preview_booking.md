# Informe tecnico V23.4.8 - Envio real desde vista previa BOOKING_PREVIEW

## Problema

La vista previa de reserva validaba correctamente disponibilidad exacta, pero al enviar la respuesta se mandaba por WhatsApp el texto de preview. No se creaba `booking`, no se generaba enlace y no aparecian logs de ejecucion real.

## Evidencia previa

El log mostraba:

```text
TEMPORARY_BOOKING_DRY_RUN
WHATSAPP_MESSAGE_FORMATTED type=BOOKING_PREVIEW
WHATSAPP_RESPONSE_SEND_STARTED
WHATSAPP_RESPONSE_SEND_RESULT sent=true
```

pero no mostraba:

```text
AI_REAL_SEND_STARTED
TRANSACTIONAL_BOOKING_STARTED dryRun=false
TEMPORARY_BOOKING_CREATE_STARTED
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
```

## Causa raiz

El metodo que detecta si un cuerpo corresponde a preview de IA (`isAiDryRunPreviewBody`) no reconocia el nuevo formato introducido para WhatsApp:

```text
👀 *Vista previa de reserva*
Esta vista previa *no creo una reserva temporal ni un enlace real*.
Al enviar la respuesta por WhatsApp se creara la reserva temporal y el enlace de confirmacion.
```

Por eso `resolveRealAiMessageBodyIfRequired` devolvia el body sin transformarlo, y `dispatchOutbound` lo enviaba tal cual.

## Solucion

Se amplio la deteccion para cubrir:

- formato legacy anterior;
- formato V23.4.6 `👀 *Vista previa de reserva*`;
- fallback basado en frases transaccionales principales.

Tambien se agrego metadata en `ConversationAiReplyResponse.source` agregando `_BOOKING_PREVIEW` cuando la respuesta sugerida corresponde a una vista previa de reserva.

## Riesgos

- Si el usuario escribe manualmente un mensaje extremadamente parecido al texto de preview, podria disparar ejecucion real. La probabilidad es baja porque la deteccion exige varias frases especificas.
- La solucion por metadata queda disponible en `source`, pero si el frontend aun no envia `source` al endpoint de envio, la deteccion textual sigue siendo necesaria.

## Como validar

1. Levantar proyecto:

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

2. Enviar mensaje del cliente:

```text
Hola, quiero reservar limpieza facial profunda jueves a las 10:00 en Providencia
```

3. Presionar `Responder con IA`.

4. Confirmar que la vista previa contiene:

```text
👀 *Vista previa de reserva*
```

5. Presionar `Enviar`.

6. Revisar logs:

```powershell
docker compose -f docker-compose.local.yml logs --tail=3000 backend-java | Select-String -Pattern "AI_REAL_SEND","dryRun=false","TEMPORARY_BOOKING_CREATE_STARTED","TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","WHATSAPP_MESSAGE_FORMATTED","WHATSAPP_RESPONSE_SEND_RESULT","FLOW_ERROR"
```

## Criterio de aceptacion

Despues de presionar **Enviar**, el mensaje enviado por WhatsApp debe contener:

```text
✅ *Reserva temporal creada*
```

Y debe contener un enlace real:

```text
/reservas/confirmar/
```

No debe enviarse el texto `👀 *Vista previa de reserva*` como mensaje final.
