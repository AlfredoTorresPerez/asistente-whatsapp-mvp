# ANÁLISIS DE REPOSITORIO PARA PRUEBAS

## 1. Módulos encontrados

| Módulo | Frontend | Backend API | DB Tables |
|--------|----------|-------------|-----------|
| Auth/Login | `LoginPage.tsx` | `POST /api/v1/auth/login`, `GET /api/v1/auth/me` | `user_account`, `role`, `user_role` |
| Agenda Completa | `CompleteAgendaPage.tsx` | `POST /api/v1/agenda/availability`, `GET /api/v1/agenda/calendar`, `POST /api/v1/agenda/temporary-bookings`, `PATCH .../reschedule`, `PATCH .../cancel` | `booking`, `agenda_room`, `agenda_block`, `agenda_holiday`, `agenda_business_hours`, `agenda_professional_hours` |
| Reservas (admin) | `NewAppointmentPage.tsx`, etc. | `POST /api/v1/bookings`, `GET /api/v1/bookings`, etc. | `booking`, `booking_status_history` |
| Confirmación pública | `BookingConfirmationPage.tsx` | `GET /api/v1/public/booking-confirmations/{token}`, `POST .../confirm` | `booking_confirmation_link` |
| Cancelación pública | `BookingCancellationPage.tsx` | `GET /api/v1/public/booking-cancellations/{token}`, `POST .../confirm` | `booking_cancellation_link` |
| Reprogramación pública | `BookingReschedulePage.tsx` | `GET /api/v1/public/booking-reschedules/{token}`, `POST .../confirm` | `booking_reschedule_link` |
| Pago público | `BookingPaymentPage.tsx` | `GET /api/v1/public/booking-payments/{paymentId}`, `POST .../simulate` | `booking_payment` |
| Sucursales | `AdminLocationsPage.tsx` | `GET /api/v1/business-locations`, `POST/PUT/DELETE` | `business_location` |
| Profesionales | (agenda filter) | `GET /api/v1/agenda/filter-options`, `GET /api/v1/multisite/professionals` | `aesthetic_professional`, `aesthetic_professional_location` |
| Servicios | `CatalogPage.tsx` | `GET /api/v1/esthetic/services`, `POST/PUT` | `aesthetic_service`, `aesthetic_service_category` |
| Conversaciones | `ConversationsPage.tsx` | `GET/POST /api/v1/conversations`, `POST .../messages` | `conversation`, `message` |
| WhatsApp | `WhatsAppWebConnectionPage.tsx` | `POST /api/v1/integrations/whatsapp-web/webhook` | `conversation`, `message`, `channel_event_log` |
| Notificaciones | `NotificationsPage.tsx` | `GET /api/v1/notifications` | `notification` |
| Dashboard | `DashboardPage.tsx` | `GET /api/v1/dashboard/summary` | (aggregate queries) |
| Configuración | `ConfigurationPage.tsx` | `GET/PATCH /api/v1/configuration/whatsapp` | `whatsapp_configuration_preferences` |
| Administración | `AdministrationPage.tsx` | `GET /api/v1/admin/summary` | `business`, `security_policy` |

## 2. Endpoints encontrados (37 controllers, ~120+ endpoints)

### Agenda y Reservas (críticos para este plan de pruebas)

| Endpoint | Método | Autenticación | Propósito |
|----------|--------|---------------|-----------|
| `/api/v1/agenda/availability` | POST | Sí | Consultar disponibilidad por sucursal, servicio, profesional, fecha |
| `/api/v1/agenda/calendar` | GET | Sí | Vista calendario semanal con reservas |
| `/api/v1/agenda/filter-options` | GET | Sí | Obtener servicios, profesionales, cabinas filtrables |
| `/api/v1/agenda/temporary-bookings` | POST | Sí | Crear reserva temporal desde agenda |
| `/api/v1/agenda/blocks` | POST | Sí | Crear bloqueo manual |
| `/api/v1/agenda/bookings/{id}/reschedule` | PATCH | Sí | Reprogramar desde agenda |
| `/api/v1/agenda/bookings/{id}/cancel` | PATCH | Sí | Cancelar desde agenda |
| `/api/v1/bookings` | GET/POST | Sí | CRUD reservas admin |
| `/api/v1/bookings/{id}` | GET/PUT | Sí | Detalle/actualizar reserva |
| `/api/v1/bookings/{id}/reschedule` | PATCH | Sí | Reprogramar admin |
| `/api/v1/bookings/{id}/cancel` | PATCH | Sí | Cancelar admin |
| `/api/v1/bookings/{id}/confirmation-link` | POST | Sí | Generar link de confirmación |
| `/api/v1/bookings/{id}/reschedule-link` | POST | Sí | Generar link de reprogramación |
| `/api/v1/bookings/{id}/cancellation-link` | POST | Sí | Generar link de cancelación |
| `/api/v1/public/booking-confirmations/{token}` | GET | No | Vista previa confirmación |
| `/api/v1/public/booking-confirmations/{token}/confirm` | POST | No | Confirmar reserva |
| `/api/v1/public/booking-confirmations/{token}/availability` | GET | No | Disponibilidad para reprogramar |
| `/api/v1/public/booking-confirmations/{token}/reschedule` | POST | No | Reprogramar desde link |
| `/api/v1/public/booking-confirmations/{token}/cancel` | POST | No | Cancelar desde link |
| `/api/v1/public/booking-cancellations/{token}` | GET | No | Vista previa cancelación |
| `/api/v1/public/booking-cancellations/{token}/confirm` | POST | No | Confirmar cancelación |
| `/api/v1/public/booking-reschedules/{token}` | GET | No | Vista previa reprogramación |
| `/api/v1/public/booking-reschedules/{token}/confirm` | POST | No | Confirmar reprogramación |
| `/api/v1/public/booking-reschedules/{token}/reject` | POST | No | Rechazar reprogramación |
| `/api/v1/public/booking-payments/{paymentId}` | GET | No | Ver pago |
| `/api/v1/public/booking-payments/{paymentId}/detail` | GET | No | Detalle de pago |
| `/api/v1/public/booking-payments/{paymentId}/simulate` | POST | No | Simular pago |
| `/api/v1/public/landing/availability` | POST | No | Disponibilidad landing pública |
| `/api/v1/public/landing/bookings` | POST | No | Crear reserva landing pública |
| `/api/v1/auth/login` | POST | No | Login |
| `/api/v1/auth/me` | GET | Sí | Obtener usuario actual |
| `/api/v1/health` | GET | No | Health check |

## 3. Pantallas encontradas

| Ruta | Componente | Propósito |
|------|-----------|-----------|
| `/login` | `LoginPage` | Inicio de sesión |
| `/agenda` | `CompleteAgendaPage` | Agenda visual semanal |
| `/appointments` | `AppointmentsPage` | Lista de reservas admin |
| `/appointments/new` | `NewAppointmentPage` | Crear reserva admin |
| `/appointments/{id}/reschedule` | `RescheduleAppointmentPage` | Reprogramar admin |
| `/appointments/{id}` | `AppointmentDetailPage` | Detalle reserva |
| `/reservas/confirmar/:token` | `BookingConfirmationPage` | Confirmación pública |
| `/reservas/cancelar/:token` | `BookingCancellationPage` | Cancelación pública |
| `/reservas/reprogramar/:token` | `BookingReschedulePage` | Reprogramación pública |
| `/reservas/pagar/:paymentId` | `BookingPaymentPage` | Pago público |
| `/admin/locations` | `AdminLocationsPage` | Gestionar sucursales |
| `/conversations` | `ConversationsPage` | Conversaciones WhatsApp |
| `/catalog` | `CatalogPage` | Catálogo servicios/productos |
| `/prospects` | `ProspectsPage` | Prospectos/leads |
| `/orders` | `OrdersPage` | Órdenes |
| `/dashboard` | `DashboardPage` | Dashboard |
| `/business-ai` | `BusinessAiPage` | Configuración AI |
| `/admin/security` | `AdminSecurityPage` | Seguridad |

## 4. Tablas encontradas (PostgreSQL)

### Tablas principales de booking/agenda
- `booking` — Reservas (con profesional_id, location_id, service_id, room_id, status, version)
- `booking_confirmation_link` — Links de confirmación
- `booking_cancellation_link` — Links de cancelación  
- `booking_reschedule_link` — Links de reprogramación
- `booking_status_history` — Historial de estados
- `booking_payment` — Pagos de reservas
- `booking_reminder` — Recordatorios
- `booking_email_log` — Log de emails
- `booking_calendar_sync` — Sincronización calendario
- `agenda_room` — Cabinas/recursos
- `agenda_room_service` — Asociación cabina-servicio
- `agenda_block` — Bloqueos manuales
- `agenda_holiday` — Feriados
- `agenda_business_hours` — Horarios de sucursal
- `agenda_professional_hours` — Horarios de profesional
- `agenda_professional_service` — Asociación profesional-servicio
- `professional_absence` — Ausencias del profesional (V40)

### Tablas maestras
- `business` — Negocio
- `business_location` — Sucursales (con timezone)
- `aesthetic_professional` — Profesionales (con max_daily_bookings)
- `aesthetic_service` — Servicios (con duration, preparation, cleanup, requires_room, requires_deposit)
- `aesthetic_service_category` — Categorías
- `aesthetic_professional_location` — Asignación profesional-sucursal
- `aesthetic_service_location` — Asignación servicio-sucursal
- `customer` — Clientes (con normalized_phone, active)
- `user_account` — Usuarios

### Tablas de conversaciones
- `conversation`, `message`, `channel_account`, `channel_event_log`

## 5. Riesgos detectados

| Riesgo | Descripción | Impacto |
|--------|-------------|---------|
| Sin endpoint health dedicado | Solo `GET /api/v1/health` existe | Bajo |
| Concurrencia no testeada | No hay pruebas de doble reserva simultánea | Alto |
| Datos QA no aislados | No existe prefijo QA_AUTO_ en seed data | Alto |
| Sin conexión DB en tests | No hay configuración de DB para test con PG | Medio |
| WhatsApp simulado no expuesto | No hay endpoint de simulación de inbound messages | Alto |
| Validaciones backend recién implementadas | V40 y AvailabilityService no tienen tests de integración | Alto |
| Pantallas públicas sin token válido difícil de generar | Requieren flujo completo de booking + link creation | Medio |
| Sin datos demo estables para pruebas | Demo data en V37 usa fechas fijas Jun/Jul 2026 | Alto |

## 6. Brechas

| Brecha | Detalle | Acción requerida |
|--------|---------|------------------|
| Falta endpoint para simular mensaje WhatsApp entrante | No hay un `POST /api/v1/test/whatsapp-inbound` | Marcar pruebas WhatsApp como BLOCKED hasta crear helper API |
| Falta endpoint de health DB | No hay un endpoint que verifique conexión PostgreSQL | Pruebas DB requieren conexión directa (riesgosa) |
| No hay seed QA_AUTO_ | Los datos demo usan nombres reales | Crear helper que genere datos vía API con prefijo QA_AUTO_ |
| Validación de disponibilidad no expone todo el motor en API | No hay endpoint que valide todas las reglas en una sola llamada | Usar combinación de endpoints existentes |
| No se pueden probar 2-8 sucursales sin configurarlas | Demo data tiene 2 sucursales (Providencia, Las Condes) | Usar datos existentes |

## 7. Qué se puede probar automáticamente

| Categoría | Tests posibles | % Cobertura |
|-----------|---------------|-------------|
| Smoke tests | Frontend carga, login, health check | 100% |
| Auth | Login/logout/forgot password | 100% |
| Agenda visual | Vista semanal, 7 días, línea hora actual | 100% |
| Disponibilidad | Slots libres/ocupados, filtros por servicio/profesional | 90% |
| Reserva admin (API) | Crear, validar errores, estados | 80% |
| Reserva admin (UI) | Formulario, validaciones, confirmación | 70% |
| Reprogramación admin (API) | Validar transiciones, slot availability | 80% |
| Cancelación admin (API) | Validar estados, liberar slot | 80% |
| Confirmación pública | Preview + confirm + doble confirmación + expiración | 90% |
| Cancelación pública | Preview + confirm + estados | 80% |
| Reprogramación pública | Preview + confirm + reject | 80% |
| Pago simulado | Ver detalle + simular pago | 90% |
| Concurrencia (API) | Dos requests simultáneos al mismo slot | 50% |
| Base de datos | Solo verificar desde API, no conexión directa | 30% |
| WhatsApp simulado | BLOCKED (sin endpoint de simulación) | 0% |

## 8. Qué queda BLOCKED

| Prueba | Motivo |
|--------|--------|
| WhatsApp inbound simulado (TST-001 a TST-056) | No existe endpoint de simulación de mensaje entrante. El webhook `POST /api/v1/integrations/whatsapp-web/webhook` existe pero requiere payload real de WhatsApp WebJS. No hay un `POST /api/v1/test/whatsapp-inbound` para simular. |
| Flujo completo WhatsApp -> reserva | No se puede simular una conversación completa desde cero. Falta endpoint para crear conversación con mensaje de cliente y activar AI. |
| Base de datos directa (SELECT/INSERT) | No hay configuración de base de datos de prueba en el stack de tests. El proyecto no usa testcontainers. Conectarse a la DB de desarrollo es inseguro y podría corromper datos. |
| 3-8 sucursales en escenarios reales | Demo data tiene solo 2 sucursales. Para probar 3-8 se requiere configuración manual o un script de seed. |
| Pruebas de carga/estrés | Playwright no es la herramienta adecuada para concurrencia masiva. Para concurrencia se usará API. |
| Pagos reales (webhook) | No hay webhook de pago real configurado. Solo simulación. |
| Sincronización Google Calendar real | No hay cuenta OAuth configurada en tests. |
| Email real | Solo simulado (`APP_EMAIL_SIMULATION_ENABLED=true`) |
