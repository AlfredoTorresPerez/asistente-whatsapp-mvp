# Cambios: aprobar y enviar desde IA del negocio

## Objetivo

Convertir la accion de la vista previa de IA del negocio en una accion operativa real: seleccionar una conversacion activa, aprobar la respuesta generada o editada, enviarla al cliente y registrar el mensaje en el historial.

## Cambios funcionales

1. Se agrego busqueda de conversaciones activas en la tarjeta de vista previa.
2. Se agrego selector de destinatario activo usando conversaciones abiertas del modulo de conversaciones.
3. El boton cambio de `Aprobar y copiar` a `Aprobar y enviar`.
4. Al aprobar, la pantalla envia el texto aprobado mediante el endpoint existente de conversaciones.
5. La respuesta enviada queda registrada como mensaje saliente en la conversacion seleccionada.
6. Se mantiene la validacion para impedir envios sin respuesta generada o sin conversacion seleccionada.

## Flujo esperado

1. El usuario escribe un escenario en el simulador.
2. Presiona `Probar IA`.
3. La aplicacion genera una respuesta sugerida.
4. El usuario selecciona una conversacion activa como destinatario.
5. Opcionalmente edita la respuesta.
6. Presiona `Aprobar y enviar`.
7. El sistema envia el mensaje por el canal configurado y lo registra en el historial.

## Archivos modificados

- `frontend-react/src/modules/business-ai/pages/BusinessAiPage.tsx`

## API utilizada

Se reutiliza el endpoint ya existente:

```http
POST /api/v1/conversations/{conversationId}/messages
```

Con cuerpo:

```json
{
  "body": "respuesta aprobada"
}
```

## Restricciones

- Solo se permite enviar si hay una conversacion activa seleccionada.
- La pantalla no crea conversaciones nuevas; usa conversaciones existentes.
- Si el adaptador de WhatsApp Web no esta listo, el backend puede registrar error de envio segun la respuesta del canal.
- La disponibilidad de agenda sigue sin confirmarse desde esta accion; solo envia la respuesta aprobada.
