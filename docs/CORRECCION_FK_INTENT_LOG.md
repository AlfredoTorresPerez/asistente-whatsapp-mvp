# Corrección FK intent log

Se corrigió el registro de análisis de intención del módulo Centro Estético.

## Problema

Al recibir mensajes reales por WhatsApp Web, el análisis de intención se ejecutaba con una transacción independiente. El cliente y la conversación recién creados todavía no estaban confirmados en base de datos, por lo que PostgreSQL rechazaba el insert en `aesthetic_intent_log` por clave foránea.

## Cambios

- `AestheticCenterService.analyzeInboundMessage` ahora participa en la misma transacción del flujo llamador.
- Antes de guardar el log de IA, se valida si `customer_id` y `conversation_id` existen para el negocio. Si no existen, se guardan como `null` para no romper el flujo.
- Se corrigió una condición duplicada en `WhatsAppWebChannelJdbcRepository.findLatestConversation`.

## Resultado esperado

- El backend no debe registrar errores de clave foránea al analizar mensajes de WhatsApp Web.
- La pantalla Conexión WhatsApp Web debe poder mostrar el último análisis de IA cuando exista.
- El webhook no se rechaza si falla una clasificación secundaria.
