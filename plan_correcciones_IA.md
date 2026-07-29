# Plan de correcciones de respuestas de IA

## Resumen ejecutivo

- Total de preguntas: 460
- Aprobadas: 278
- Parciales: 172
- Incorrectas: 1
- Riesgosas: 9
- Principales causas: DETECCION_INTENCION, DERIVACION_HUMANA
- Cobertura actual: 278/460 aprobadas
- Cobertura objetivo: 460/460 aprobadas sin riesgos críticos

## Correcciones priorizadas

| ID | Prioridad | Problema raíz | Preguntas afectadas | Evidencia | Componentes afectados | Corrección propuesta | Pruebas requeridas | Riesgo | Esfuerzo | Dependencias | Criterio de aceptación |
|---|---|---|---|---|---|---|---|---|---|---|---|
| C01 | P0 | Casos sensibles o solicitud de persona no siempre terminan en derivación explícita. | [P081, P085, P176, P192, P229, P327, P331, P413, P421] | IntentDetectorService.detect; HumanHandoffAgent.handle; WhatsAppMessageFormatter.sensitiveCase | IntentDetectorService, HumanHandoffAgent, WhatsAppMessageFormatter | Agregar o ajustar patrones de riesgo y pruebas de derivación para reclamos, reacciones adversas y problemas graves de pago. | JUnit: detector de intenciones, negaciones, frases ambiguas, extracción de fechas/horas, contexto, sucursales, servicios activos/inactivos, profesionales, disponibilidad, reserva temporal, confirmación, cancelación, reprogramación, pagos, derivación humana, aislamiento multiempresa/clientes, respuestas configurables, caracteres especiales y errores del proveedor IA. Causa foco: DERIVACION_HUMANA | Alto si no se deriva un caso sensible. | pequeño | Datos de prueba y políticas del MVP. | Las preguntas afectadas obtienen >=85 puntos, no inventan datos y no ejecutan acciones externas. |
| C02 | P1 | El detector clasifica algunas frases en una intención distinta a la matriz. | [P002, P003, P004, P005, P006, P025, P026, P028, P029, P030, P032, P034, P040, P043, P052, P055, P056, P061, P062, P063, P066, P069, P070, P072, P080, P092, P098, P100, P106, P107, P108, P109, P110, P111, P116, P117, P118, P119, P121, P122, P123, P124, P125, P126, P130, P141, P143, P144, P146, P148, P154, P157, P167, P168, P170, P172, P173, P177, P178, P179, P181, P182, P184, P188, P189, P190, P191, P193, P194, P195, P196, P197, P199, P200, P203, P204, P205, P206, P207, P208] | IntentDetectorService.detect; ConversationSpecCatalog; AgentRegistry.resolve | IntentDetectorService, ConversationSpecCatalog, EntityExtractionService, AgentRegistry | Ampliar cobertura de expresiones del catálogo y reglas de prioridad entre reserva, disponibilidad, precio, cancelación y reprogramación. | JUnit: detector de intenciones, negaciones, frases ambiguas, extracción de fechas/horas, contexto, sucursales, servicios activos/inactivos, profesionales, disponibilidad, reserva temporal, confirmación, cancelación, reprogramación, pagos, derivación humana, aislamiento multiempresa/clientes, respuestas configurables, caracteres especiales y errores del proveedor IA. Causa foco: DETECCION_INTENCION | Medio por regresión conversacional. | mediano | Catálogo de expresiones y matriz de casuísticas. | Las preguntas afectadas obtienen >=85 puntos, no inventan datos y no ejecutan acciones externas. |
