# Backlog de capacidades no implementadas — IA Centro Estético

Capacidades identificadas durante la auditoría de 460 preguntas que NO están implementadas actualmente.

## 1. Capacidad de atención multi-persona

- **Descripción**: El asistente no diferencia entre reservas individuales, para dos personas, o grupales
- **Preguntas afectadas**: P081, P085, P327, P328, P329, P330
- **Ejemplo**: "Quiero reservar para dos personas" inicia flujo individual
- **Solución propuesta**: Nuevo campo "cantidad_personas" en el flujo de reserva; validación de capacidad en agenda

## 2. Consulta de políticas (cancelación, reprogramación, pago)

- **Descripción**: No hay respuestas específicas para políticas del negocio
- **Preguntas afectadas**: P224, P225, P226, P227, P262, P289, P290, P295, P296, P297, P405
- **Ejemplo**: "¿Hasta cuándo puedo cancelar sin cobro?" → PAYMENT_INQUIRY genérica
- **Solución propuesta**: Módulo de policy-knowledge con reglas configurables por empresa

## 3. Gestión de inasistencias y bloqueos

- **Descripción**: El sistema no reconoce ni gestiona inasistencias, bloqueos ni regularizaciones
- **Preguntas afectadas**: P366, P368, P369, P370, P371, P372, P373, P375
- **Ejemplo**: "Me informaron que estoy bloqueada, ¿qué significa?" → AMBIGUOUS genérica
- **Solución propuesta**: Nuevo intent BLOCKED_ACCOUNT; flujo de desbloqueo

## 4. Recordatorios y notificaciones

- **Descripción**: No hay gestión de recordatorios, preferencias de notificación, ni avisos
- **Preguntas afectadas**: P233, P234, P235, P236, P237, P238, P239, P240, P241, P242, P243, P244, P245, P246, P304, P315, P316
- **Ejemplo**: "¿Pueden enviarme un recordatorio el día anterior?" → AMBIGUOUS
- **Solución propuesta**: Nuevo intent REMINDER_REQUEST; integración con servicio de notificaciones

## 5. Lista de espera multi-opción

- **Descripción**: La lista de espera solo maneja un servicio/sucursal; falta soporte para múltiples opciones
- **Preguntas afectadas**: P302, P303, P304, P305, P306, P307, P308, P309, P310, P311, P312, P313, P314, P315, P316
- **Ejemplo**: "¿Puedo quedar en espera en varias sucursales?" → LOCATION_QUERY
- **Solución propuesta**: Ampliar entidad "lista_espera" con múltiples sucursales, profesionales y horarios

## 6. Contraindicaciones y preparación pre-servicio

- **Descripción**: No hay respuestas para contraindicaciones médicas, preparación previa, ni condiciones especiales
- **Preguntas afectadas**: P348, P349, P350, P351, P352, P353, P354, P355, P356, P357, P359, P360, P361, P362
- **Ejemplo**: "¿Debo hacer algo antes del tratamiento?" → AMBIGUOUS
- **Solución propuesta**: Nuevo intent TREATMENT_PREPARATION; knowledge-base de servicios con pre-requisitos

## 7. Consentimiento y menores de edad

- **Descripción**: No hay gestión de consentimiento informado ni flujo para menores de edad
- **Preguntas afectadas**: P336, P337, P338, P339, P340, P341, P342, P343, P344, P345, P346
- **Ejemplo**: "¿Necesito firmar un consentimiento?" → AMBIGUOUS
- **Solución propuesta**: Nuevo intent CONSENT_REQUEST; flujo con validación de edad y tutor

## 8. Horarios por sucursal y excepciones

- **Descripción**: Los horarios de atención son genéricos; no hay manejo de excepciones (feriados, horario extendido)
- **Preguntas afectadas**: P381, P382, P384, P386, P388, P389, P392
- **Ejemplo**: "¿Atienden en días feriados?" → AMBIGUOUS
- **Solución propuesta**: Repositorio de horarios con excepciones; nuevo intent HOLIDAY_HOURS_QUERY

## 9. Reasignación de profesional

- **Descripción**: No hay flujo para cuando el profesional cambia, se ausenta o hay licencia
- **Preguntas afectadas**: P393, P395, P396, P397, P398, P399, P400, P401, P402, P403, P406
- **Ejemplo**: "¿Qué pasa si la profesional se ausenta?" → PROFESSIONAL_QUERY (parcial)
- **Solución propuesta**: Eventos de cambio de profesional con notificación al cliente

## Tabla resumen

| Capacidad | Prioridad | Impacto | Esfuerzo estimado |
|---|---|---|---|
| Consulta de políticas | Alta | ~15 preguntas | 3 días |
| Recordatorios y notificaciones | Alta | ~18 preguntas | 5 días |
| Gestión de inasistencias | Alta | ~10 preguntas | 3 días |
| Multi-persona | Media | ~6 preguntas | 2 días |
| Lista de espera multi-opción | Media | ~15 preguntas | 4 días |
| Contraindicaciones | Media | ~15 preguntas | 3 días |
| Consentimiento y menores | Baja | ~11 preguntas | 3 días |
| Horarios por sucursal | Baja | ~7 preguntas | 2 días |
| Reasignación de profesional | Baja | ~11 preguntas | 4 días |
