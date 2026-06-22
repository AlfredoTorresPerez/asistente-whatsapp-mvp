# Cambios v23.4.22 - Envio real de reserva cuando el panel conserva saludo generico

Esta version corrige el caso observado en logs donde la vista previa IA detectaba correctamente la reserva, pero al presionar enviar el panel enviaba un saludo generico de 62 caracteres en lugar de ejecutar el flujo real de reserva.

## Correcciones

- Si el backend recibe un saludo generico como `Hola Contacto, gracias por escribirnos. Te ayudo de inmediato.` y el ultimo mensaje entrante del cliente es de agenda/reserva, el backend regenera la respuesta real de IA con `dryRun=false`.
- Se agrega traza `AI_REAL_SEND_STARTED reason=GENERIC_AI_GREETING_AFTER_BOOKING_INBOUND_DETECTED` para auditar este fallback.
- El frontend conserva de forma mas robusta la sugerencia de reserva `BOOKING_PREVIEW` al enviar, incluso si el composer quedo con un saludo generico.
- El reporte de matriz queda actualizado a `reporte_matriz_excel_ia_v23_4_22.md`.

## Trazas esperadas al enviar desde el panel

```text
AI_REAL_SEND_STARTED reason=GENERIC_AI_GREETING_AFTER_BOOKING_INBOUND_DETECTED
TRANSACTIONAL_BOOKING_STARTED dryRun=false
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
AI_REAL_SEND_RESULT generated=true containsLink=true
WHATSAPP_RESPONSE_SEND_STARTED
```
