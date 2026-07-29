# Prompt correctivo para OpenCode - Versión 2

Basado en la ejecución PRUEBA_IA_20260729T144535

## Contexto técnico

Se ejecutaron 460 preguntas contra el asistente WhatsApp para centros estéticos.
Proveedor activo: WhatsApp Cloud API (META_CLOUD_API).
Backend: Spring Boot en Java 21, PostgreSQL 16.

## Resultados

- Óptimas: 460
- Aceptables: 0
- Deficientes: 0
- Errores críticos: 0

## Casos fallidos

## Archivos involucrados

- `IntentDetectorService.java`: clasificación de intención del mensaje
- `AgentCoordinatorService.java`: ruteo entre agentes
- `BookingAgent.java`: agente de reservas
- `SalesAgent.java`: agente de ventas e información
- `ReceptionAgent.java`: agente de recepción

## Cambios permitidos

1. Ajustes en `IntentDetectorService.java` para mejorar clasificación
2. Ajustes en `AgentCoordinatorService.java` para mejorar ruteo
3. Ajustes en reglas de respuesta en `AiBusinessKnowledgeService.java`

## Cambios prohibidos

1. No modificar la integración de WhatsApp
2. No modificar credenciales o secretos
3. No eliminar datos de la base de datos

## Orden de implementación

1. Corregir errores P0 (críticos)
2. Mejorar casos deficientes (P1)
3. Optimizar casos aceptables (P2)

## Pruebas

- Ejecutar `mvn test` para pruebas unitarias
- Ejecutar runner con `node scripts/ejecutar_prueba_v2.js failed` para regresión

## Criterios de aceptación

- Todos los errores críticos resueltos
- Tasa de éxito (óptimas+aceptables) >= 80%
- Sin regresiones en casos anteriormente óptimos
