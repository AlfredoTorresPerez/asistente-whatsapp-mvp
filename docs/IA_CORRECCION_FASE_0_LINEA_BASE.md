# Fase 0 — Línea base y protección de alcance

## 1. Información de la rama

- **Rama**: `fix/correccion-asistente-ia-fase0`
- **Basada en**: `master` (commit `66ea872`)
- **Fecha**: 2026-07-29

## 2. Resultados de pruebas existentes

- **Total tests**: 613
- **Fallos**: 0
- **Errores**: 0
- **Saltados**: 0
- **Resultado**: BUILD SUCCESS

## 3. Diagnóstico del ejecutor de pruebas actual

### Archivo: `scripts/ejecutar_prueba_v2.js`

**Problemas identificados:**

1. **Clave de sesión fija**: todas las preguntas usan `sessionKey: 'demo-sales'`. No hay identificador único por mensaje.

2. **Correlación incorrecta**: La respuesta se obtiene con `ORDER BY created_at DESC LIMIT 1` filtrada solo por `customer_phone`. Esto puede asociar una respuesta antigua a una pregunta nueva.

3. **Sin identificador de correlación**: No se envía un `messageId` o `correlationId` único por pregunta.

4. **Evaluador semántico defectuoso**: Asigna puntaje por palabras genéricas ("hola", "servicio", "precio", "agenda"). Por esto, 460 preguntas distintas obtuvieron 96/100 aunque la IA solo generó **2 respuestas únicas**:
   - "Hola, gracias por escribirnos. ¿Te ayudo con servicios, precios o agenda?"
   - "Puedo ayudarte con información del catálogo, pero necesito el servicio específic..."

5. **Sin detección de respuestas duplicadas o antiguas**: No valida que la respuesta recibida corresponda al mensaje enviado.

6. **Sin capacidad de escenarios continuos**: Todas las preguntas se tratan como casos independientes sin compartir contexto.

### Resultados de la última ejecución

- 460/460 procesadas
- 458 en la ejecución principal + 2 de piloto
- **100% clasificadas como ÓPTIMA** (96/100)
- **0 errores críticos**
- Tiempo promedio: 3.236 ms

**Conclusión**: los resultados son engañosos. La IA solo respondió con 2 mensajes genéricos, sin distinguir entre solicitudes de catálogo, reclamos, reservas, cancelaciones, etc.

## 4. Diagnóstico de detección de intenciones

### Archivo: `IntentDetectorService.java`

**Problemas identificados:**

1. **Las palabras "servicio" y "producto" están en SALES_WORDS**: Esto hace que cualquier consulta que mencione "servicio" (incluso reclamos o quejas sobre un servicio) se clasifique como `COMMERCIAL_INQUIRY`.

2. **SENSITIVE_WORDS muy restrictivo**: Solo palabras como "quemadura", "dolor fuerte", "reacción". No detecta variaciones como "me quemé", "me duele mucho", etc.

3. **COMPLAINT_WORDS no incluye quejas sobre servicios específicos**: Frases como "no quedé conforme con el servicio" requieren coincidencia exacta de la lista de COMPLAINT_WORDS.

4. **No hay diferenciación entre consulta de catálogo genérica vs específica**: `SALES_WORDS` incluye "servicio" lo que provoca que todo se rutee a `SalesAgent`.

5. **El saludo genérico se usa como respuesta para todo**: `ReceptionAgent` parece estar devolviendo siempre el mismo saludo/catálogo.

6. **Las reglas de `ConversationSpecCatalog` no se están aplicando correctamente**: El `shouldUseCatalogIntent` solo aplica para AMBIGUOUS, BOOKING_CHANGE, HUMAN_REQUEST, COMPLAINT, pero no para otras intenciones.

## 5. Diagnóstico de agentes

### Archivos revisados:
- `ReceptionAgent.java`
- `SalesAgent.java`
- `BookingAgent.java`
- `AgentCoordinatorService.java`
- `SupportAgent.java`
- `HumanHandoffAgent.java`

**Problemas identificados:**

1. **ReceptionAgent responde con saludo genérico para casi todo**: Al recibir un mensaje con intención AMBIGUOUS o GREETING, responde con el mismo mensaje de "¿Te ayudo con servicios, precios o agenda?".

2. **SalesAgent no puede responder sin un servicio específico**: Si no se extrae `servicio_o_producto`, devuelve "AI_SALES_MISSING_SERVICE_RESPONSE" que pide el servicio específico.

3. **No hay diferenciación real entre intenciones comerciales**: SERVICE_INFORMATION, SERVICE_RECOMMENDATION y COMMERCIAL_INQUIRY siguen caminos similares que siempre requieren un servicio específico.

4. **BookingAgent no se activa para preguntas de reserva**: Muchas preguntas que deberían activar BOOKING_REQUEST se quedan en COMMERCIAL_INQUIRY o AMBIGUOUS porque faltan palabras clave de reserva.

5. **HumanHandoffAgent y SupportAgent no se activan**: Los reclamos y solicitudes humanas no se detectan correctamente.

## 6. Componentes financieros prohibidos (no modificar)

### Java classes — no tocar:

```
**Bookings Payment:**
- BookingPaymentProvider.java (domain)
- BookingPaymentService.java (application)
- BookingPaymentProviderRegistry.java (application)
- BookingPaymentProperties.java (application)
- BookingReceiptService.java (application)
- BookingPaymentJdbcRepository.java (infrastructure)
- MercadoPagoPaymentProvider.java (infrastructure)
- SimulatedPaymentProvider.java (infrastructure)
- BookingPaymentWebhookController.java (api)
- BookingPaymentWebhookRequest.java (api)
- BookingPaymentWebhookResponse.java (api)
- BookingPaymentResponse.java (api)
- PublicBookingPaymentController.java (api)
- PublicBookingPaymentDetailResponse.java (api)
- CreateBookingPaymentLinkRequest.java (api)
- RegisterBookingManualPaymentRequest.java (api)
- RefundBookingPaymentRequest.java (api)

**Orders Payment:**
- RegisterPaymentRequest.java (api)
- OrderPaymentResponse.java (api)
- OrderService.java — solo método registerPayment
- OrderJdbcRepository.java — solo queries/mutations de pago
- OrderDetailResponse.java — campo payments
- OrderSummaryResponse.java — campo paymentStatus
- OrderController.java — endpoints de pago

**AI Agents (solo referencia, no modificar lógica de pagos):**
- PaymentsAgent.java — no modificar
- AgentType.java — no eliminar PAYMENTS
- AgentIntent.java — no eliminar PAYMENT_INQUIRY, PAYMENT_PROBLEM
- IntentDetectorService.java — no modificar PAYMENT_WORDS, PAYMENT_PROBLEM_WORDS

**Booking State Machine:**
- BookingStateMachine.java — no modificar PENDING_PAYMENT
- BookingConfirmationService.java — no modificar lógica de pago
- BookingService.java — no modificar PENDING_PAYMENT
```

### Migration files — no modificar:

```
V35__booking_payments.sql
V36__booking_payment_operations.sql
V42__booking_receipt_table.sql
V45__booking_payment_provider_columns.sql
V47__transbank_webpay_mall.sql
```

### Archivos de configuración — no modificar secciones de pago:

```
application.yml — sección booking-payment
.env* — variables APP_BOOKING_PAYMENT_*, APP_MERCADOPAGO_*, APP_TRANSBANK_*
```

## 7. Excel corregido creado

- **Archivo**: `preguntas_respuesta_IA_version_2_corregido.xlsx`
- **Hoja**: `Preguntas_Respuestas`
- **Columnas agregadas**:
  - `Excluido del prompt pagos/cobros` (SÍ/NO) — pendiente de clasificar
  - `Intencion esperada` — pendiente de definir
  - `Categoria` — pendiente de clasificar
  - `Escenario` — pendiente de clasificar

## 8. Próximos pasos (Fase 1)

1. Reemplazar `scripts/ejecutar_prueba_v2.js` con un ejecutor que tenga:
   - Identificador único por mensaje (correlationId)
   - Sesión independiente por escenario
   - Correlación por messageId y timestamp
   - Detección de respuestas duplicadas/antiguas
   - Capacidad de escenarios continuos
2. Agregar columna de exclusión en el Excel (identificar casos de pago)
3. Implementar nuevo evaluador semántico (Fase 2)
