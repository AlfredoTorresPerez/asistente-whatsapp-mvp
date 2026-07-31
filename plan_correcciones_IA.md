# Plan de correcciones de respuestas de IA

## Resumen ejecutivo

- Total de preguntas: 460
- Aprobadas: 0
- Parciales: 0
- Incorrectas: 0
- Riesgosas: 0
- Principales causas: ERROR_PERSISTENCIA
- Cobertura actual: 0/460 aprobadas
- Cobertura objetivo: 460/460 aprobadas sin riesgos críticos

## Correcciones priorizadas

| ID | Prioridad | Problema raíz | Preguntas afectadas | Evidencia | Componentes afectados | Corrección propuesta | Pruebas requeridas | Riesgo | Esfuerzo | Dependencias | Criterio de aceptación |
|---|---|---|---|---|---|---|---|---|---|---|---|
| C01 | P3 | ERROR_PERSISTENCIA | [P001, P002, P003, P004, P005, P006, P007, P008, P009, P010, P011, P012, P013, P014, P015, P016, P017, P018, P019, P020, P021, P022, P023, P024, P025, P026, P027, P028, P029, P030, P031, P032, P033, P034, P035, P036, P037, P038, P039, P040, P041, P042, P043, P044, P045, P046, P047, P048, P049, P050, P051, P052, P053, P054, P055, P056, P057, P058, P059, P060, P061, P062, P063, P064, P065, P066, P067, P068, P069, P070, P071, P072, P073, P074, P075, P076, P077, P078, P079, P080] | registro_ejecucion_IA.json; clase AgentCoordinatorService o dependencia invocada | AgentCoordinatorService, agentes especializados, reglas de conocimiento | Definir regla específica del caso y conectar al agente existente sin crear integraciones externas nuevas. | JUnit: detector de intenciones, negaciones, frases ambiguas, extracción de fechas/horas, contexto, sucursales, servicios activos/inactivos, profesionales, disponibilidad, reserva temporal, confirmación, cancelación, reprogramación, pagos, derivación humana, aislamiento multiempresa/clientes, respuestas configurables, caracteres especiales y errores del proveedor IA. Causa foco: ERROR_PERSISTENCIA | Medio por regresión conversacional. | mediano | Datos de prueba y políticas del MVP. | Las preguntas afectadas obtienen >=85 puntos, no inventan datos y no ejecutan acciones externas. |
