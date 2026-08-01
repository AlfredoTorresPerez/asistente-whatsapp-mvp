# Asistente WhatsApp MVP

MVP de asistente empresarial para WhatsApp orientado a Centro Estetico Bella. Incluye backend Java/Spring Boot, frontend React/Vite, PostgreSQL, modulo de IA conversacional, catalogo, agenda, conversaciones, administracion y adaptador experimental de WhatsApp Web.

## Evaluacion libre del cliente

Para entregar el MVP a un cliente o evaluador sin guion rigido, usar primero:

- [GUIA_EVALUACION_LIBRE_CLIENTE.md](GUIA_EVALUACION_LIBRE_CLIENTE.md)
- [docs/MVP_CONTROLLED_DEMO_READINESS.md](docs/MVP_CONTROLLED_DEMO_READINESS.md)

La guia explica que se puede probar libremente, que sigue experimental, que no debe considerarse productivo todavia y como reportar errores.

## Levantar localmente

```powershell
docker compose -f docker-compose.local.yml up -d --build
```

URLs principales:

- Frontend: http://localhost:5173
- Backend health: http://localhost:8080/api/v1/health
- WhatsApp Web visual: http://localhost:6080/vnc.html?autoconnect=true&resize=scale

Credenciales demo locales:

- Usuario: `admin@demo.cl`
- Contrasena: `Cambiar123!`

La sesion demo local dura 8 horas (`APP_ACCESS_TOKEN_EXPIRES_IN_SECONDS=28800`).

Estas credenciales son solo para entornos locales o demos controladas. No deben configurarse como valores productivos ni mostrarse en la pantalla de inicio de sesion salvo activacion explicita del modo demo local.

## Validacion tecnica

Backend:

```powershell
cd backend-java
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend-react
corepack pnpm install --frozen-lockfile
corepack pnpm build
corepack pnpm test -- --run
corepack pnpm lint
```

Docker:

```powershell
docker compose -f docker-compose.local.yml ps
docker compose -f docker-compose.local.yml logs --tail=200 backend-java frontend-react
```

## Flujo de pagos

El sistema soporta pagos de reservas en modo **simulado** (QA/demo) y con proveedor real **MercadoPago** (producción). El modo se selecciona por variable de entorno.

### Variables de entorno

```env
# SIMULATED (default para local/demo) o MERCADOPAGO
APP_PAYMENT_PROVIDER=SIMULATED

# Solo necesario en MERCADOPAGO
APP_MERCADOPAGO_ACCESS_TOKEN=...
APP_MERCADOPAGO_POS_ID=...
APP_MERCADOPAGO_WEBHOOK_SECRET=...
```

### Endpoints públicos (sin autenticación)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/public/booking-payments/{paymentId}/detail` | Datos del pago + reserva asociada |
| `POST` | `/api/v1/public/booking-payments/{paymentId}/simulate` | Simular aprobación/rechazo (solo modo SIMULATED) |

`POST /simulate` acepta `{"action": "APPROVED"}` o `{"action": "REJECTED"}`.

### Página pública de pago

```
/reservas/pagar/:paymentId
```

### Gestión interna (requiere auth)

La página `AppointmentDetailPage` (admin) permite generar link de pago, registrar pago manual offline y reembolsar.

### Pruebas E2E de pago

```powershell
cd frontend-react
npx playwright test --project=all-chromium --grep "Pago" --reporter=list
```

Requiere que el backend esté corriendo y que no haya otro proceso (como contenedores Docker) ocupando el puerto 5173.

### Arquitectura

- `BookingPaymentService` — orquestación (crear, actualizar estado, expirar, reembolsar)
- `SimulatedPaymentProvider` — simulación local sin llamadas externas
- `MercadoPagoPaymentProvider` — integración real con API de MercadoPago
- `BookingPaymentController` — endpoints privados (admin)
- `PublicBookingPaymentController` — endpoints públicos (checkout)
- `BookingPaymentJdbcRepository` — persistencia + JOIN con `bookings`
- Migraciones Flyway: `V20__create_booking_payments.sql`, `V35__booking_payment_indexes.sql`, `V36__booking_payment_cascade_fix.sql`

## Restricciones

- WhatsApp Web es experimental y no debe presentarse como canal productivo final.
- Para produccion se recomienda WhatsApp Cloud API.
- La auto-respuesta IA queda desactivada por defecto.
- La agenda no debe confirmar disponibilidad sin validacion real.
- No usar `docker compose down -v` salvo que se quiera borrar datos locales.


## Alcance corregido del MVP

Esta aplicacion debe describirse como **Asistente de Negocios por WhatsApp para centro estetico multisucursal**.

No corresponde llamarla multicanal u omnicanal mientras el unico canal operativo real sea WhatsApp. El objetivo funcional del MVP es convertir conversaciones de WhatsApp en reservas confirmadas por enlace, con operacion por multiples sucursales.

## v23.4.15 - pruebas de matriz Excel IA

Esta entrega incorpora pruebas automatizadas para validar preguntas del cliente y respuestas del orquestador IA desde la matriz Excel entregada.

Ejecutar en Windows:

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1
```

Modo estricto:

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

Documentacion: `docs/qa/PRUEBAS_MATRIZ_EXCEL_IA_V23_4_15.md`.

## Cola persistente para respuestas IA

Esta version incorpora una cola persistente basada en el patron outbox para desacoplar la recepcion del mensaje WhatsApp, el procesamiento IA y el envio de la respuesta automatica.

Flujo resumido:

1. El webhook recibe `MESSAGE_RECEIVED`.
2. Se guarda el cliente, la conversacion y el mensaje `INBOUND`.
3. Se inserta un trabajo `PENDING` en `ai_reply_outbox`.
4. `AiReplyOutboxProcessor` reclama trabajos vencidos y ejecuta la orquestacion IA.
5. Si existe respuesta automatica, se guarda el mensaje `OUTBOUND`, se despacha por WhatsApp y se registra `message_delivery_log`.
6. Si falla, el trabajo queda reintentable hasta agotar `max_attempts`.

Variables principales:

```env
APP_AI_AGENTS_OUTBOX_WORKER_INTERVAL_MS=5000
APP_AI_AGENTS_OUTBOX_BATCH_SIZE=10
APP_AI_AGENTS_OUTBOX_PROCESSING_TIMEOUT_MS=120000
APP_AI_AGENTS_OUTBOX_RETRY_BASE_DELAY_MS=30000
APP_AI_AGENTS_OUTBOX_RETRY_MAX_DELAY_MS=900000
```

Documento de cambios: `CAMBIOS_COLA_OUTBOX_IA.md`.
Prompt tecnico ejecutado: `PROMPT_COLA_OUTBOX_IA.md`.
