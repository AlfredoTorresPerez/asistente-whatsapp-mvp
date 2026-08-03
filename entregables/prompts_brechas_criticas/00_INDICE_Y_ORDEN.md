# Prompts para corregir las brechas críticas del ambiente local

Estos prompts están diseñados para ejecutarse de forma secuencial sobre el repositorio del asistente. Cada archivo es autónomo, pero debe comenzar inspeccionando el estado actual porque una corrección anterior puede haber modificado los mismos archivos.

## Orden recomendado

1. `01_SECRETOS_Y_EMPAQUETADO_SEGURO.md`
   Contiene la exposición actual, evita nuevas filtraciones y establece un proceso de distribución seguro.
2. `02_CERRAR_BYPASS_PERFIL_LEGACY_META.md`
   Elimina la posibilidad de iniciar Meta real sin las protecciones del perfil controlado.
3. `03_CABLEAR_META_CONTROLADO_EN_COMPOSE.md`
   Hace reproducible el arranque seguro mediante Docker Compose y los scripts oficiales.
4. `04_ALLOWLIST_FAIL_CLOSED_Y_MINIMIZACION.md`
   Convierte la lista permitida en un control efectivo para tráfico entrante y saliente, y evita persistir datos no autorizados.

## Reglas comunes

- No mostrar, copiar, registrar ni documentar valores de secretos.
- No incluir números telefónicos completos; usar marcadores o valores claramente ficticios y enmascarados.
- No contactar Meta, enviar mensajes, rotar credenciales externas ni reiniciar el túnel sin autorización explícita del usuario.
- No penalizar la integración real con WhatsApp Cloud API: es una modalidad local intencional. La meta es hacerla controlada y fail-closed.
- Mantener funcional el proveedor `SIMULATED` y preservar la separación entre local simulado, local Meta controlado, QA y producción.
- Inspeccionar el código existente y las instrucciones `AGENTS.md` antes de modificar.
- Preservar cambios ajenos presentes en el árbol de trabajo.
- Ejecutar pruebas proporcionales al riesgo y entregar evidencia sanitizada.

## Criterio de cierre del conjunto

El ambiente queda corregido cuando el modo simulado no puede emitir tráfico externo, Meta real solo puede arrancar con confirmación explícita, firma obligatoria y lista permitida no vacía, ningún mensaje real puede dirigirse fuera de esa lista en local, y el único ZIP compartible es generado y verificado por los scripts oficiales sin secretos, metadatos Git ni datos personales completos.
