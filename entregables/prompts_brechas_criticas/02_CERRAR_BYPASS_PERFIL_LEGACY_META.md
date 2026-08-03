# Prompt 02 — Cerrar el bypass del perfil legado de Meta

Actúa como ingeniero principal de Spring Boot y seguridad operativa. Elimina la posibilidad de iniciar WhatsApp Cloud API real en local sin pasar por las protecciones de `local-meta-controlled`.

## Contexto verificado

- Existen `application-local-safe.yml`, `application-local-meta-controlled.yml` y el perfil legado `application-local-whatsapp-cloud.yml`.
- `LocalEnvironmentGate` solo protege `local-safe` y `local-meta-controlled`.
- El perfil legado puede habilitar Meta real sin acknowledgement ni lista permitida.
- Una configuración local observada utilizó el perfil legado con firma deshabilitada, lista permitida ausente, safe mode desactivado y respuestas automáticas habilitadas.

## Objetivo

Conservar las dos modalidades locales intencionales —simulada y Meta real controlada—, pero hacer imposible cualquier combinación local que active Meta, respuesta real u otro tráfico externo evitando la compuerta de seguridad.

## Trabajo requerido

1. Inspecciona perfiles Spring, `LocalEnvironmentGate`, `LocalEnvironmentPolicy`, propiedades de canales, configuración de IA, correo, calendario, pagos, scripts de arranque y documentación.
2. Define una política inequívoca para perfiles locales:
   - `local,local-safe`: proveedor simulado, sin tráfico externo y respuesta real deshabilitada;
   - `local,local-meta-controlled`: Meta real con acknowledgement, firma obligatoria, lista permitida no vacía y credenciales completas;
   - ninguna otra combinación local puede activar `META_CLOUD_API`.
3. Retira el perfil `local-whatsapp-cloud` o conviértelo en un alias seguro que ejecute exactamente las mismas validaciones del perfil controlado. No debe quedar un camino de compatibilidad menos protegido.
4. Haz que la compuerta rechace:
   - perfil `local` sin modalidad explícita;
   - `local-safe` y `local-meta-controlled` simultáneos;
   - Meta habilitado fuera de `local-meta-controlled`;
   - firma deshabilitada, allowlist vacía, acknowledgement falso o dry-run incompatible;
   - respuesta automática real en modo seguro;
   - combinaciones locales con salidas externas no declaradas.
5. Decide y documenta el alcance de salidas externas permitidas en Meta controlado. Si OpenAI o correo real son necesarios para pruebas, deben tener flags y acknowledgements explícitos separados; no deben habilitarse implícitamente por activar Meta.
6. Mantén QA y producción sin cambios de comportamiento, salvo validaciones que sean claramente seguras y compatibles.
7. Actualiza plantillas y documentación para que el modo seguro sea el valor predeterminado. No escribas valores reales ni números completos.
8. Proporciona mensajes de error que nombren propiedades infractoras, nunca sus valores.

## Pruebas obligatorias

- Pruebas unitarias de matriz de perfiles y propiedades, incluyendo todos los casos rechazados.
- Prueba de contexto Spring que demuestre que la aplicación aborta antes de quedar disponible si se intenta usar el perfil legado inseguro.
- Prueba de que `local-safe` arranca con `SIMULATED` y bloquea Meta, OpenAI, correo externo, calendario externo y pagos reales.
- Prueba de que `local-meta-controlled` solo arranca cuando todos los controles están presentes.
- Regresión de QA y producción a nivel de carga de configuración.
- Suite focalizada de webhook, outbox y proveedor simulado.

## Criterios de aceptación

- No existe un perfil local capaz de activar Meta sin la política controlada.
- La configuración insegura detectada en la auditoría produce un fallo de arranque claro.
- El modo simulado sigue siendo el predeterminado y no emite tráfico externo.
- QA y producción conservan su comportamiento esperado.
- No se registran valores sensibles al validar o fallar.

## Entrega esperada

Implementa el cierre del bypass, documenta la matriz final de perfiles y adjunta evidencia de pruebas. No reinicies el túnel ni contactes Meta durante esta tarea.
