# Prompt ejecutado: cola persistente para respuestas IA

## Objetivo

Incorporar al asistente WhatsApp una cola robusta de procesamiento asincrono para desacoplar tres responsabilidades:

1. Recepcion del mensaje entrante desde WhatsApp.
2. Analisis y orquestacion IA del mensaje recibido.
3. Envio de la respuesta automatica al cliente.

## Prompt tecnico

Actua como arquitecto backend senior Java Spring Boot y refactoriza el proyecto adjunto para incorporar una cola persistente basada en el patron transactional outbox.

Requisitos:

- El webhook de WhatsApp no debe ejecutar directamente el analisis IA ni el envio del mensaje de respuesta.
- Al recibir un MESSAGE_RECEIVED, el sistema debe guardar el cliente, la conversacion y el mensaje INBOUND igual que ahora.
- En la misma transaccion debe insertar un trabajo pendiente en una tabla ai_reply_outbox.
- Un worker programado debe leer trabajos PENDING vencidos, bloquearlos de forma segura, ejecutar el analisis IA, generar la respuesta multiagente y enviarla por el canal WhatsApp.
- El worker debe soportar reintentos con backoff exponencial, limite maximo de intentos, estados PROCESSING, PROCESSED, FAILED y SKIPPED.
- Debe evitar procesamiento duplicado usando una restriccion unica por business_id e inbound_message_id.
- MESSAGE_SENT_EXTERNAL debe seguir registrandose como mensaje manual saliente y no debe activar IA.
- La respuesta automatica solo debe enviarse si APP_AI_AGENTS_AUTO_REPLY_ENABLED esta activo.
- Mantener trazabilidad con AiTraceLogger y message_delivery_log.
- Mantener compatibilidad con Flyway, PostgreSQL, Spring Boot y la arquitectura modular existente.

Criterio de aceptacion:

- El webhook responde rapido despues de persistir el evento y encolar la solicitud IA.
- La IA se ejecuta fuera del flujo sincrono del webhook.
- Si falla el envio o el proveedor IA tarda/falla, el trabajo queda reintentable.
- Si se agotan los intentos, el trabajo queda en FAILED sin perder auditoria ni mensaje original.
