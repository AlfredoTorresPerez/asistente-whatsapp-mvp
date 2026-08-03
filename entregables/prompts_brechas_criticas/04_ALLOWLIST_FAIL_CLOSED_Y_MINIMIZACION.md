# Prompt 04 — Allowlist fail-closed y minimización de datos de webhook

Actúa como especialista en seguridad de integraciones y privacidad. Convierte la lista permitida de teléfonos de prueba en una barrera efectiva para mensajes entrantes y salientes de la modalidad local Meta controlada.

## Contexto verificado

- `WhatsAppCloudWebhookParser.isTestPhoneAllowed` permite cualquier teléfono cuando la lista es nula o vacía.
- El evento con el cuerpo completo del webhook se inserta en `channel_event_log` antes de validar el remitente.
- La compuerta de `local-meta-controlled` exige una lista no vacía, pero el perfil legado y una configuración incorrecta pueden evitar esa protección.
- La integración cuenta con idempotencia, métricas, outbox y logs enmascarados parciales.

## Objetivo

En local Meta controlado, rechazar por defecto cualquier origen o destino no autorizado, no persistir contenido de mensajes rechazados y conservar idempotencia, trazabilidad sanitizada y compatibilidad con producción.

## Trabajo requerido

1. Inspecciona parser, controlador, propiedades, adaptador de envío, servicios inbound/outbox, repositorios, migraciones, métricas y pruebas existentes.
2. Extrae una política reusable de teléfonos de prueba autorizados con normalización estricta y comparación exacta. No uses coincidencias parciales.
3. Define el comportamiento por entorno:
   - `local-meta-controlled`: allowlist obligatoria y fail-closed;
   - `local-safe`: no hay tráfico Meta;
   - QA/producción: conservar la política existente salvo decisión explícita y documentada.
4. Para mensajes entrantes, valida el remitente antes de:
   - crear conversación o cliente;
   - insertar mensaje;
   - encolar respuesta;
   - persistir el payload completo del webhook.
5. Si se requiere idempotencia para eventos rechazados, almacena solo una huella no reversible, tipo de evento, timestamp, motivo y metadatos mínimos sanitizados. No guardes teléfono completo, nombre, texto ni payload crudo.
6. Aplica el mismo control a envíos salientes en local Meta controlado. Ningún flujo —outbox, confirmación, recordatorio, envío manual o reintento— puede enviar a un destino fuera de la lista.
7. Mantén el webhook respondiendo de forma compatible con Meta para evitar reintentos innecesarios, pero registra una métrica de rechazo sin datos sensibles.
8. Revisa que logs, excepciones, trazas y métricas solo usen identificadores enmascarados o hashes apropiados. Centraliza el enmascaramiento en `LogSanitizer`.
9. Evita condiciones de carrera: la validación debe ocurrir inmediatamente antes del efecto externo y no solo al recibir el mensaje.
10. Documenta claramente que una allowlist vacía significa “nadie autorizado”, nunca “todos autorizados”.

## Pruebas obligatorias

- Lista nula y vacía: arranque rechazado o todos los mensajes bloqueados, según la capa probada.
- Teléfono autorizado con distintos formatos: aceptado tras normalización.
- Teléfono no autorizado: webhook aceptado técnicamente, pero sin cliente, conversación, mensaje, payload crudo, outbox ni envío.
- Repetición del mismo evento rechazado: comportamiento idempotente.
- Intento de envío directo y por outbox a destino no autorizado: bloqueado.
- Confirmación, recordatorio y respuesta automática a destino autorizado: continúan funcionando.
- Producción y proveedor simulado sin regresiones.
- Aserciones que confirmen que logs y registros no contienen teléfonos completos ni cuerpos de mensajes rechazados.

## Criterios de aceptación

- La allowlist es fail-closed en local Meta controlado para entrada y salida.
- Ningún dato personal de un remitente no autorizado se persiste fuera del mínimo técnico no reversible.
- Los rechazos son observables mediante métricas y logs sanitizados.
- Idempotencia y flujos autorizados siguen funcionando.
- No cambia el comportamiento de producción sin una decisión explícita.

## Entrega esperada

Implementa la política, migraciones si fueran necesarias, pruebas y documentación. Entrega evidencia sanitizada y no realices llamadas a Meta durante la verificación.
