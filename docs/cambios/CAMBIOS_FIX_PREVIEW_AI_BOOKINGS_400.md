# Correccion de errores preview IA y creacion de citas

## Problemas detectados

1. `POST /api/v1/bookings` devolvia `400` cuando el frontend enviaba el estado `PENDIENTE_CONFIRMACION`.
2. La validacion Java mantenia `@Size(max = 20)` para `status`, aunque la migracion `V29` amplio la columna `booking.status` a `varchar(30)`.
3. `POST /api/v1/conversations/{conversationId}/preview-ai` podia ejecutar una asignacion de sucursal dentro de una transaccion marcada como solo lectura.
4. La pagina publica de confirmacion no trataba `EXPIRADA` como estado cerrado en la funcion auxiliar de cierre.

## Cambios aplicados

- Se aumento la validacion `status` de 20 a 30 caracteres en:
  - `CreateBookingRequest`
  - `UpdateBookingRequest`
  - `CreateBookingFromConversationRequest`
  - `CreateBookingFromLeadRequest`
- Se cambio `ConversationService.previewAiReply` de `@Transactional(readOnly = true)` a `@Transactional`, porque el flujo puede asignar sede detectada a la conversacion.
- Se agrego `EXPIRADA` a los estados cerrados de la pagina publica de confirmacion.

## Validacion esperada

- `PENDIENTE_CONFIRMACION` ya no debe provocar error de validacion por largo.
- La vista previa de IA no debe fallar por intentar actualizar conversacion en transaccion de solo lectura.
- Las citas pendientes deben poder crearse y quedar visibles en agenda.
