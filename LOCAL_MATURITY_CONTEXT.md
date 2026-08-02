# Contexto obligatorio para evaluar la madurez del ambiente local

> Instrucciones iniciales para OpenCode y cualquier agente que evalúe la madurez del ambiente local.

Este proyecto corresponde a un MVP (producto mínimo viable) de un asistente de negocios mediante WhatsApp para un centro de estética.

## Modalidades del ambiente local

El ambiente local posee dos modalidades intencionales:

1. **Modalidad local simulada** — destinada a desarrollo, pruebas automáticas y funcionamiento sin servicios externos.
2. **Modalidad local de integración real controlada con WhatsApp Cloud API** — destinada a pruebas conversacionales completas.

La modalidad con Meta utiliza:

- Un número prepago dedicado exclusivamente como número empresarial de prueba.
- Un número personal expresamente autorizado que representa al cliente de prueba.
- Una página de aterrizaje que permite iniciar la conversación.
- WhatsApp Cloud API para envío y recepción real.
- Notificaciones web de Meta.
- Flujos de agenda, confirmación, reprogramación, cancelación y consultas comerciales.

## Decisión intencional (no penalizar)

La activación de WhatsApp Cloud API en local es una decisión **intencional y necesaria** para evaluar la madurez conversacional y la integración completa.

**No considerar la utilización de Meta en local como una brecha por sí sola.**

## Controles a evaluar

Evaluar la madurez según los controles implementados:

- Activación explícita del perfil de Meta.
- Número empresarial dedicado a pruebas.
- Lista permitida de clientes de prueba.
- Validación de firma de las notificaciones web.
- Prevención de mensajes duplicados.
- Límites de frecuencia.
- Interruptor de emergencia para detener respuestas reales.
- Registros sanitizados.
- Secretos fuera del repositorio.
- Ausencia de credenciales en archivos comprimidos.
- Datos exclusivamente de prueba.
- Proveedor simulado disponible y funcional.
- Separación clara entre local simulado, local con Meta y producción.

## Dimensiones de la evaluación

La evaluación debe distinguir entre:

1. Aislamiento del ambiente local.
2. Seguridad de la integración.
3. Madurez de la integración real.
4. Madurez conversacional.
5. Reproducibilidad.
6. Protección de secretos.

## Fortaleza de la integración real controlada

La integración real controlada debe considerarse una fortaleza cuando:

- Solamente intervienen números autorizados.
- No se utilizan clientes reales.
- Meta requiere activación explícita.
- Los secretos se encuentran protegidos.
- Existe trazabilidad.
- Las respuestas pueden detenerse inmediatamente.
- El proveedor simulado continúa disponible.
- No existe riesgo de afectar producción.

## Reglas de evaluación

- No incluir números telefónicos completos, tokens, contraseñas ni secretos en informes, registros o documentación.
- Evaluar exclusivamente la madurez del ambiente local.
- No penalizar el uso de Meta en local por sí solo. Evaluar si existen los controles adecuados: perfil explícito, lista permitida de números, validación de firma, prevención de duplicados, límites de frecuencia, interruptor de emergencia, registros sanitizados, secretos fuera del repositorio, proveedor simulado y separación respecto de producción.
- Separar la calificación de aislamiento local, seguridad, integración real y madurez conversacional.
