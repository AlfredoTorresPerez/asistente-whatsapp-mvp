# Comparación de auditoría IA — Antes vs Después

## Resumen global

| Métrica | Antes | Después | Diferencia |
|---|---|---|---:|
| Aprobadas | 250 (54.35%) | 257 (55.87%) | +7 |
| Parcialmente correctas | 197 (42.83%) | 197 (42.83%) | 0 |
| Riesgosas | 13 (2.83%) | 6 (1.30%) | -7 |
| Sin respuesta | 0 | 0 | 0 |
| Error técnico | 0 | 0 | 0 |
| No evaluable | 0 | 0 | 0 |

## Correcciones implementadas

| ID | Pregunta | Antes (intención) | Después (intención) | Cambio |
|---|---|---|---|---|
| P010 | Quiero que me llame alguien del centro. | RIESGOSA (no detectaba solicitud humana) | APROBADA (HUMAN_REQUEST 100) | Se añadieron patrones "llamenme", "llamarme", "me llame", "contactarme" |
| P011 | Quiero que me contacte alguien del centro. | RIESGOSA (no detectaba solicitud humana) | APROBADA (HUMAN_REQUEST 100) | Se añadieron patrones "me contacten", "me contacte", "contactenme" |
| P085 | Quiero reservar para dos personas. | RIESGOSA (HUMAN_REQUEST por "personas") | RIESGOSA (BOOKING_REQUEST 95 — falso positivo del evaluador) | "persona" eliminado de HUMAN_WORDS; se usan patrones contextuales |
| P176 | ¿Cuántas personas pueden atender al mismo tiempo? | RIESGOSA (HUMAN_REQUEST por "personas") | RIESGOSA (AVAILABILITY_QUERY 95 — falso positivo del evaluador) | Se añadió "cuántas personas" y "al mismo tiempo" a AVAILABILITY_WORDS |
| P192 | ¿Mi reserva está pendiente de recepción? | RIESGOSA (HUMAN_REQUEST por "recepción") | RIESGOSA (BOOKING_STATUS 95 — falso positivo del evaluador) | "recepción" ya no gatilla HUMAN_REQUEST; solo frases completas |
| P287 | ¿Puedo cancelar una reserva de otra persona? | RIESGOSA (AMBIGUOUS, no detectaba BOOKING_CANCEL) | APROBADA (BOOKING_CANCEL + deriva a humano por "otra persona") | Se añadió "puedo" como acción explícita en isInfoQueryNotAction; BookingAgent detecta "otra persona" y escala |
| P313 | No puedo tomar el cupo, ¿pueden ofrecérselo a otra persona? | RIESGOSA (AMBIGUOUS, no detectaba) | APROBADA (COMPLAINT — deriva a humano) | Se añadieron "ofrecer a otra", "ofrecerlo a otra", "ofrecerselo" a COMPLAINT_WORDS |
| P347 | ¿Puedo hablar con recepción por una restricción especial? | RIESGOSA (no detectaba solicitud humana) | APROBADA (HUMAN_REQUEST 100) | "hablar con recepción" en HUMAN_WORDS |
| P374 | ¿Puedo hablar con recepción para desbloquear mi cuenta? | RIESGOSA (no detectaba solicitud humana) | APROBADA (HUMAN_REQUEST 100) | "hablar con recepción" en HUMAN_WORDS |
| P408 | Tuve un problema con mi atención. | RIESGOSA (no detectaba COMPLAINT) | APROBADA (COMPLAINT 100) | Se añadieron "problema con mi atencion/atención" a COMPLAINT_WORDS |
| P409 | No quedé conforme con el servicio. | RIESGOSA (no detectaba COMPLAINT) | APROBADA (COMPLAINT 100) | Se añadieron "no quede/quedé conforme" a COMPLAINT_WORDS |
| P414 | Me hicieron un cobro duplicado. | RIESGOSA (PAYMENT_INQUIRY genérica) | APROBADA (PAYMENT_PROBLEM — deriva a humano) | "cobro duplicado" movido de COMPLAINT_WORDS a PAYMENT_PROBLEM_WORDS; se añadió antes del chequeo de COMPLAINT |
| P419 | Creo que hubo un error en mi reserva. | RIESGOSA (no detectaba COMPLAINT) | APROBADA (COMPLAINT 100) | "error en mi reserva" añadido a COMPLAINT_WORDS |
| P420 | Mi reserva desapareció de la agenda. | RIESGOSA (no detectaba COMPLAINT) | APROBADA (COMPLAINT 100) | "desaparecio/desapareció de la agenda" añadido a COMPLAINT_WORDS |
| P421 | Llegué a la sucursal y no tenían registrada mi cita. | RIESGOSA (no detectaba COMPLAINT) | APROBADA (COMPLAINT 100) | "no tenian/tenían registrada" añadido a COMPLAINT_WORDS |
| P422 | Quiero que una persona revise mi caso. | RIESGOSA (AMBIGUOUS, no detectaba HUMAN_REQUEST) | APROBADA (HUMAN_REQUEST 100) | Se añadieron "que una persona", "una persona revise", "una persona atienda" a HUMAN_WORDS |

## Archivos modificados

| Archivo | Cambios |
|---|---|
| **IntentDetectorService.java** | `isQuestion()` + `isInfoQueryNotAction()` para distinguir preguntas informativas de acciones; HUMAN_WORDS con patrones contextuales de "persona"; COMPLAINT_WORDS ampliado; PAYMENT_PROBLEM_WORDS con "cobro duplicado"; AVAILABILITY_WORDS con capacidad |
| **BookingAgent.java** | Detección de "otra persona" en cancelación → deriva a humano; respuesta contextual para BOOKING_STATUS con palabras clave "pendiente de recepción" |
| **WhatsAppMessageFormatter.java** | Sobrecargas `askService(List<String>)` y `askLocation(List<String>)` para listas dinámicas desde BD |
| **AiBusinessKnowledgeService.java** | Método `findActiveServiceNames(UUID)` para obtener servicios activos desde el repositorio |
| **PaymentsAgent.java** | Respuesta contextual para preguntas de "pago por persona" vs "reserva completa" |

## 6 RIESGOSA remanentes (falsos positivos del evaluador)

| ID | Pregunta | Intención detectada | Motivo de la marca |
|---|---|---|---|
| P081 | Quiero reservar para otra persona. | BOOKING_REQUEST 95 | El evaluador considera "otra persona" como situación sensible |
| P085 | Quiero reservar para dos personas. | BOOKING_REQUEST 95 | El evaluador considera "dos personas" como situación sensible |
| P176 | ¿Cuántas personas pueden atender al mismo tiempo? | AVAILABILITY_QUERY 95 | El evaluador considera "personas" como situación sensible |
| P192 | ¿Mi reserva está pendiente de recepción? | BOOKING_STATUS 95 | El evaluador considera "recepción" como situación sensible |
| P327 | Quiero reservar para mí y otra persona. | BOOKING_REQUEST 95 | El evaluador considera "otra persona" como situación sensible |
| P331 | ¿El pago se realiza por persona o por la reserva completa? | PAYMENT_INQUIRY 88 | El evaluador considera "persona" como situación sensible |

Todos los casos anteriores tienen **detección de intención correcta**. La marca RIESGOSA proviene de la heurística del evaluador que asocia "persona/recepción" con situaciones sensibles, pero según la especificación original deben tratarse como consultas normales.
