# Plan de correcciones pendientes — IA Centro Estético

## 1. Falsos positivos del evaluador automático (6 casos)

Estos casos tienen detección de intención correcta pero el evaluador los marca como RIESGOSA. Requieren revisión del evaluador, no del código.

| ID | Acción recomendada | Prioridad |
|---|---|---|
| P081, P085, P327 | Aceptar como APROBADAS — la intención BOOKING_REQUEST es correcta para reservas multi-persona | Baja |
| P176 | Aceptar como APROBADA — AVAILABILITY_QUERY es la intención correcta para preguntas de capacidad | Baja |
| P192 | Aceptar como APROBADA — BOOKING_STATUS es la intención correcta para consultar estado de reserva | Baja |
| P331 | Aceptar como APROBADA — PAYMENT_INQUIRY es la intención correcta para consultas de pago | Baja |

## 2. Mejoras de calidad en respuestas

| Área | Descripción | Prioridad |
|---|---|---|
| BOOKING_STATUS | Mejorar respuesta para que verifique automáticamente si hay reservas pendientes antes de pedir identificación | Media |
| AVAILABILITY_QUERY | Personalizar respuesta para preguntas de capacidad (no solo disponibilidad) | Media |
| Respuesta PAYMENT_INQUIRY | Mejorar respuestas para consultas de política de pago (por persona, reembolsos, etc.) | Media |
| Preguntas cerradas con "no" | "No voy a poder ir", "Ya pagué" — deberían tener intenciones más específicas que AMBIGUOUS | Media |

## 3. Backlog de capacidades no implementadas

Ver `BACKLOG_CAPACIDADES_NO_IMPLEMENTADAS_IA.md`.

## 4. Deuda técnica

| Ítem | Descripción | Prioridad |
|---|---|---|
| Pruebas unitarias | No hay tests unitarios para IntentDetectorService, BookingAgent, PaymentsAgent | Alta |
| Cobertura de edge cases | Mensajes vacíos, solo emojis, caracteres especiales no se prueban | Media |
| Logging | Los logs de INTENT_CANDIDATES no incluyen `isInfoQueryNotAction` | Baja |
