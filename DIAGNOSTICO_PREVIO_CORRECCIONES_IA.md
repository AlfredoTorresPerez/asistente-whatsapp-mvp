# Diagnóstico previo de correcciones de IA

## Resumen de hallazgos

Se analizaron 13 respuestas riesgosas y 197 respuestas parcialmente correctas de la auditoría de 460 consultas. Se identificaron 5 causas raíz principales.

---

## Causa raíz 1: DERIVACION_HUMANA - Patrones incompletos y falsos positivos

### Problema
El detector de solicitud humana (`containsHumanRequest`) usa `\b` word boundaries correctamente para palabras individuales, pero:
- No incluye patrones como "contactarme", "contactenme", "llame", "llamen"
- Frases con "hablar con recepción" pasan desapercibidas
- Evaluación tiene falsos positivos: P085, P176, P192

### Casos afectados (riesgosas reales)
| ID | Mensaje | Detectado | Esperado |
|---|---|---|---|
| P010 | Quiero que me llame alguien del centro | AMBIGUOUS | HUMAN_REQUEST |
| P011 | ¿Pueden contactarme por teléfono? | AMBIGUOUS | HUMAN_REQUEST |
| P347 | ¿Puedo hablar con recepción por una restricción especial? | AMBIGUOUS | HUMAN_REQUEST |
| P374 | ¿Puedo hablar con recepción para desbloquear mi cuenta? | AMBIGUOUS | HUMAN_REQUEST |
| P408 | Tuve un problema con mi atención. | SUPPORT_GENERAL | COMPLAINT |
| P409 | No quedé conforme con el servicio. | COMMERCIAL_INQUIRY | COMPLAINT |
| P414 | Me hicieron un cobro duplicado. | PAYMENT_INQUIRY | PAYMENT_PROBLEM |
| P419 | Creo que hubo un error en mi reserva. | BOOKING_STATUS | COMPLAINT |
| P420 | Mi reserva desapareció de la agenda. | BOOKING_STATUS | COMPLAINT |
| P421 | Llegué a la sucursal y no tenían registrada mi cita. | LOCATION_QUERY | COMPLAINT |

### Falsos positivos (no requieren derivación)
| ID | Mensaje | Causa del falso positivo |
|---|---|---|
| P085 | Quiero reservar para dos personas. | Evaluación chequea "persona" como substring, pero código correctamente usa `\bpersona\b` (no match con "personas") |
| P176 | ¿Cuántas personas pueden atender al mismo tiempo? | Misma causa que P085 |
| P192 | ¿Mi reserva está pendiente de recepción? | "recepcion" en "pendiente de recepcion" no es solicitud humana |

### Archivos
- `IntentDetectorService.java` líneas 24-25 (HUMAN_WORDS), 404-418 (containsHumanRequest)
- `HumanHandoffAgent.java` líneas 17-24

### Cambio propuesto
1. Agregar a HUMAN_WORDS: "contactarme", "contactenme", "llame", "llamen", "hablar con recepción", "hablar con un"
2. Agregar patrón COMPLAINT_PROBLEM_WORDS: "problema con mi atencion", "no quede conforme", "error en mi reserva", "desaparecio de la agenda", "no tenian registrada"
3. Mover COMPLAINT antes que SUPPORT_GENERAL en la cadena de detección

---

## Causa raíz 2: PREGUNTAS_INFORMATIVAS_EJECUTAN_ACCIONES

### Problema
Preguntas informativas que contienen palabras como "cancelar", "reprogramar", "reservar" gatillan acciones transaccionales.

### Casos afectados (parciales, 40+)
Mensajes como:
- "¿Me avisarán si el centro cancela la cita?" → detecta BOOKING_CANCEL
- "¿Qué pasa si cancelo?" → detecta BOOKING_CANCEL
- "¿Hay penalización por cancelar tarde?" → detecta BOOKING_CANCEL
- "¿Puedo cambiar el horario?" → detecta BOOKING_CHANGE
- "¿Hasta cuándo puedo reprogramar?" → detecta BOOKING_CHANGE
- "¿Cuánto debo pagar para reservar?" → detecta BOOKING_REQUEST en vez de PRICE_REQUEST

### Archivos
- `IntentDetectorService.java` líneas 158-164, 273-278

### Cambio propuesto
Agregar detección de pregunta interrogativa antes de action intents:
```java
private boolean isQuestion(String text) {
    return Pattern.compile("^(qué|cuál|cuánto|cómo|puedo|existe|hay|me avisarán|qué pasa si|hasta cuándo|cuántas veces|cuál es)").matcher(text).find()
        || text.contains("?");
}
```

Si es pregunta + contiene acción → BOOKING_STATUS o COMMERCIAL_INQUIRY (no ejecutar acción).

---

## Causa raíz 3: LISTA_SERVICIOS_CODIFICADA

### Problema
`WhatsAppMessageFormatter.askService()` línea 70-75 tiene 6 servicios hardcodeados:
"Limpieza facial profunda, Depilación bozo, Depilación rostro, Depilación axilas, Depilación piernas, Depilación bikini"

### Casos afectados
Todas las respuestas que usan `askService()` muestran siempre la misma lista fija.

### Archivos
- `WhatsAppMessageFormatter.java` líneas 70-75
- `BookingAgent.java` línea 228

### Cambio propuesto
1. Convertir `askService()` para aceptar parámetro `List<String> services` desde DB
2. Crear método en `AiBusinessKnowledgeService` que retorne servicios activos
3. Modificar `BookingAgent.bookingMissingDataResponse()` para pasar servicios dinámicos

---

## Causa raíz 4: DETECCION_DE_INTENCION - Prioridad incorrecta

### Problema
La cadena de `if/else` en `IntentDetectorService.detect()` no prioriza correctamente:
1. Preguntas sobre servicios antes que COMMERCIAL_INQUIRY
2. Preguntas sobre disponibilidad antes que BOOKING_REQUEST
3. Contexto previo antes que detección fresca

### Casos afectados
Numerosos parciales donde la intención esperada no coincide (ver plan_correcciones_IA.md C02).

### Archivos
- `IntentDetectorService.java` líneas 106-314

### Cambio propuesto
Reestructurar el orden de detección:
1. Blank/technical
2. Sensitive/safety
3. Human request
4. Negated agenda actions
5. Question check (interrogative)
6. Knowledge/follow-up
7. Explicit booking/cancel/change (only if not a question)
8. Availability
9. Services/recommendations/prices
10. Location/business hours
11. Support/ambiguous

---

## Causa raíz 5: CONSERVACION_DE_CONTEXTO

### Problema
Cuando el estado conversacional está en `WAITING_SERVICE` y el cliente responde "Limpieza facial", el coordinador a veces reinicia la detección en vez de usar el contexto.

### Archivos
- `AgentCoordinatorService.java` líneas 337-415 (resolveContextAwareIntent)

### Cambio propuesto
Dar mayor prioridad al contexto previo sobre la detección fresca cuando el estado actual tiene `ultimo_dato_solicitado` y el nuevo mensaje provee ese dato.

---

## Riesgo de los cambios

| Cambio | Riesgo | Mitigación |
|---|---|---|
| Agregar HUMAN_WORDS | Falsos positivos de derivación | Usar solo patrones con word boundaries |
| Detección de preguntas | Perder booking requests reales | Pregunta + datos completos → booking; pregunta sin datos → info |
| Quitar hardcoded services | BookingAgent necesita AiBusinessKnowledgeService | Ya está disponible via constructor |
| Reordenar detección | Regression en intents actuales | Pruebas unitarias existentes deben pasar |

## Pruebas que demostrarán corrección

1. `containsHumanRequest("personas")` → false (plural no gatilla)
2. `containsHumanRequest("pendiente de recepcion")` → false (no es solicitud humana)
3. `containsHumanRequest("quiero que me llame alguien")` → true
4. `containsHumanRequest("pueden contactarme")` → true
5. `isQuestion("¿Qué pasa si cancelo?")` → true (no ejecuta BOOKING_CANCEL)
6. `isQuestion("¿Hay penalización por cancelar?")` → true (no ejecuta BOOKING_CANCEL)
7. `detect("Quiero cancelar mi hora.")` → BOOKING_CANCEL (acción explícita sí ejecuta)
8. `detect("No quiero cancelar, solo información.")` → PRICE_REQUEST/COMMERCIAL_INQUIRY (negación inhibe)
9. `askService(["Limpieza facial", "Hidratación facial", "Masaje relajante"])` → solo esos 3
10. `askLocation(["Providencia", "Las Condes"])` → solo esas 2
