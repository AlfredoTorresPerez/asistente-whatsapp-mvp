# API_CONTRACTS

## Objetivo

Definir los contratos REST de Fase 1 para frontend, backend Java y adaptaciones internas. Este documento describe rutas, payloads, criterios de autenticacion y formatos de respuesta.

## Convenciones globales

- Prefijo obligatorio: `/api/v1`.
- Formato por defecto: `application/json`.
- Fechas y horas: ISO 8601 en UTC.
- Todas las respuestas privadas estan scopeadas por `business_id`, aunque el cliente no lo envie como campo explicito.
- Los DTOs de entrada terminan en `Request`; los DTOs de salida terminan en `Response`.
- Los listados usan paginacion server-side.
- Los errores nunca exponen stacktrace ni nombres internos de clases.

## Autenticacion y autorizacion

- Publicos:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/forgot-password`
  - `GET /api/v1/auth/reset-password/validate`
  - `POST /api/v1/auth/reset-password`
  - `GET /api/v1/health`
- Privados:
  - todos los demas endpoints del frontend.
- Integracion interna:
  - `POST /api/v1/integrations/whatsapp-cloud/webhook`
  - protegido por HMAC (`X-Hub-Signature-256`), no por JWT de usuario.

## Formato de error

```json
{
  "timestamp": "2026-05-23T20:15:30Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "La solicitud contiene datos invalidos.",
  "path": "/api/v1/auth/login",
  "fieldErrors": {
    "email": "Ingresa un correo valido."
  }
}
```

## Formato de paginacion

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

## Parametros comunes de listado

| Parametro | Tipo | Regla |
| --- | --- | --- |
| `page` | integer | Base 0. Default `0`. |
| `size` | integer | Default `20`, maximo `100`. |
| `sort` | string | Formato `field,asc` o `field,desc`. |
| `search` | string | Texto libre acotado por pantalla. |
| `active` | boolean | Filtra activos cuando aplique. |
| `from` | datetime | Inicio de rango en UTC. |
| `to` | datetime | Fin de rango en UTC. |

## Salud del backend

| Metodo | Ruta | Auth | Uso | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/health` | publica | Smoke test tecnico. | `{ "status": "UP", "service": "backend-java", "timestamp": "..." }` |

## Auth

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | publica | `{ "email": "owner@demo.cl", "password": "********" }` | `{ "accessToken": "...", "tokenType": "Bearer", "expiresInSeconds": 900, "user": { "id": "...", "firstName": "Ana", "lastName": "Lopez", "email": "owner@demo.cl", "role": "OWNER", "businessId": "..." } }` |
| `GET` | `/api/v1/auth/me` | privada | sin body | `{ "id": "...", "firstName": "Ana", "lastName": "Lopez", "email": "owner@demo.cl", "role": "OWNER", "businessId": "...", "timezone": "America/Santiago" }` |
| `POST` | `/api/v1/auth/forgot-password` | publica | `{ "email": "owner@demo.cl" }` | `{ "status": "ACCEPTED", "maskedEmail": "ow***@demo.cl" }` |
| `GET` | `/api/v1/auth/reset-password/validate?token=...` | publica | query `token` | `{ "valid": true, "expiresAt": "2026-05-23T23:00:00Z" }` |
| `POST` | `/api/v1/auth/reset-password` | publica | `{ "token": "...", "newPassword": "********", "confirmPassword": "********" }` | `{ "status": "PASSWORD_UPDATED" }` |
| `POST` | `/api/v1/auth/logout` | privada | sin body | `{ "status": "LOGGED_OUT" }` |

### Reglas de validacion de Auth

- `email`: requerido, formato email.
- `password`: requerido, minimo `8`, maximo `72`.
- `newPassword`: sujeto a politica activa de seguridad.
- `confirmPassword`: debe coincidir.

## Perfil de usuario

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/users/me` | privada | sin body | `{ "id": "...", "firstName": "Ana", "lastName": "Lopez", "email": "owner@demo.cl", "phone": "+56911112222", "timezone": "America/Santiago", "role": "OWNER" }` |
| `PATCH` | `/api/v1/users/me` | privada | `{ "firstName": "Ana", "lastName": "Lopez", "phone": "+56911112222", "timezone": "America/Santiago" }` | mismo contrato de `GET /users/me` |
| `POST` | `/api/v1/users/me/change-password` | privada | `{ "currentPassword": "********", "newPassword": "********", "confirmPassword": "********" }` | `{ "status": "PASSWORD_CHANGED" }` |

## Dashboard

| Metodo | Ruta | Auth | Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/dashboard/summary` | privada | `from`, `to`, `ownerUserId?` | `{ "kpis": { "openConversations": 24, "newProspects": 11, "openOrders": 8, "pendingAppointments": 5 }, "conversationSeries": [], "orderSeries": [], "todayAppointments": [], "recentActivity": [] }` |

## Notifications

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/notifications` | privada | `page`, `size`, `search?`, `status?`, `type?` | paginado de `NotificationResponse` |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | privada | sin body | `{ "id": "...", "status": "READ", "readAt": "..." }` |
| `PATCH` | `/api/v1/notifications/read-all` | privada | sin body | `{ "updatedCount": 12 }` |

### NotificationResponse

```json
{
  "id": "uuid",
  "type": "NEW_MESSAGE",
  "status": "UNREAD",
  "title": "Nuevo mensaje recibido",
  "body": "Pedro escribio desde WhatsApp.",
  "relatedEntityType": "CONVERSATION",
  "relatedEntityId": "uuid",
  "createdAt": "2026-05-23T20:15:30Z",
  "readAt": null
}
```

## Conversations

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/conversations` | privada | `page`, `size`, `search?`, `status?`, `ownerUserId?`, `unreadOnly?`, `from?`, `to?` | paginado de `ConversationListItemResponse` |
| `POST` | `/api/v1/conversations` | privada | `{ "customerName": "Pedro Soto", "customerPhone": "+56911112222", "ownerUserId": "uuid", "initialMessage": "Hola Pedro" }` | `{ "id": "uuid", "status": "OPEN" }` |
| `GET` | `/api/v1/conversations/{conversationId}` | privada | path param | `ConversationDetailResponse` |
| `GET` | `/api/v1/conversations/{conversationId}/messages` | privada | `page`, `size` | paginado de `ConversationMessageResponse` |
| `POST` | `/api/v1/conversations/{conversationId}/messages` | privada | `{ "body": "Hola", "templateId": null }` | `{ "messageId": "uuid", "deliveryStatus": "QUEUED" }` |
| `POST` | `/api/v1/conversations/{conversationId}/prospects` | privada | `{ "firstName": "Pedro", "lastName": "Soto", "phone": "+56911112222", "email": "pedro@demo.cl", "stage": "NEW", "notes": "Consulta por producto", "assignedUserId": "uuid" }` | `{ "id": "uuid", "conversationId": "uuid" }` |
| `POST` | `/api/v1/conversations/{conversationId}/orders` | privada | `{ "prospectId": "uuid", "items": [ { "productId": "uuid", "quantity": 2, "unitPrice": 19990 } ], "notes": "Entrega en local", "dueDate": "2026-06-05" }` | `{ "id": "uuid", "status": "DRAFT" }` |
| `POST` | `/api/v1/conversations/{conversationId}/appointments` | privada | `{ "subject": "Demostracion", "prospectId": "uuid", "startsAt": "2026-05-27T14:00:00Z", "durationMinutes": 45, "location": "Sucursal Centro", "notes": "Confirmar 1 hora antes" }` | `{ "id": "uuid", "status": "SCHEDULED" }` |

### ConversationListItemResponse

```json
{
  "id": "uuid",
  "customerName": "Pedro Soto",
  "customerPhone": "+56911112222",
  "status": "OPEN",
  "unreadCount": 2,
  "ownerUserId": "uuid",
  "lastMessagePreview": "Necesito informacion",
  "lastMessageAt": "2026-05-23T20:15:30Z"
}
```

### ConversationDetailResponse

```json
{
  "id": "uuid",
  "customerName": "Pedro Soto",
  "customerPhone": "+56911112222",
  "status": "OPEN",
  "ownerUserId": "uuid",
  "prospectId": null,
  "channelType": "WHATSAPP",
  "lastMessageAt": "2026-05-23T20:15:30Z",
  "notes": null
}
```

## Templates

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/templates` | privada | `page`, `size`, `search?`, `category?`, `active?` | paginado de `TemplateResponse` |
| `POST` | `/api/v1/templates` | privada | `{ "name": "Seguimiento 24h", "category": "FOLLOW_UP", "body": "Hola {{customer_name}}", "active": true }` | `TemplateResponse` |
| `PATCH` | `/api/v1/templates/{templateId}` | privada | `{ "name": "Seguimiento 24h", "category": "FOLLOW_UP", "body": "Hola {{customer_name}}", "active": false }` | `TemplateResponse` |
| `DELETE` | `/api/v1/templates/{templateId}` | privada | sin body | `{ "status": "DEACTIVATED" }` |

## Prospects

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/prospects` | privada | `page`, `size`, `search?`, `stage?`, `ownerUserId?` | paginado de `ProspectResponse` |
| `POST` | `/api/v1/prospects` | privada | `{ "firstName": "Pedro", "lastName": "Soto", "phone": "+56911112222", "email": "pedro@demo.cl", "stage": "NEW", "notes": "Interesado en plan basico", "assignedUserId": "uuid" }` | `ProspectResponse` |
| `GET` | `/api/v1/prospects/{prospectId}` | privada | path param | `ProspectDetailResponse` |
| `PATCH` | `/api/v1/prospects/{prospectId}` | privada | mismo body que `POST`, mas `active?` | `ProspectResponse` |

## Appointments

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/appointments` | privada | `page`, `size`, `search?`, `status?`, `from?`, `to?`, `ownerUserId?` | paginado de `AppointmentResponse` |
| `POST` | `/api/v1/appointments` | privada | `{ "subject": "Visita tecnica", "prospectId": "uuid", "conversationId": null, "startsAt": "2026-05-27T14:00:00Z", "durationMinutes": 45, "location": "Sucursal Centro", "notes": "Llevar catalogo" }` | `AppointmentResponse` |
| `GET` | `/api/v1/appointments/{appointmentId}` | privada | path param | `AppointmentDetailResponse` |
| `PATCH` | `/api/v1/appointments/{appointmentId}` | privada | `{ "status": "COMPLETED" }` o actualizacion parcial permitida | `AppointmentResponse` |
| `POST` | `/api/v1/appointments/{appointmentId}/reschedule` | privada | `{ "startsAt": "2026-05-28T16:00:00Z", "durationMinutes": 45, "reason": "Cliente solicito cambio" }` | `AppointmentResponse` |

## Orders y payments

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/orders` | privada | `page`, `size`, `search?`, `status?`, `paymentStatus?`, `from?`, `to?` | paginado de `OrderListItemResponse` |
| `POST` | `/api/v1/orders` | privada | `{ "prospectId": "uuid", "conversationId": null, "items": [ { "productId": "uuid", "quantity": 1, "unitPrice": 19990 } ], "notes": "Retiro en tienda", "dueDate": "2026-06-05" }` | `OrderDetailResponse` |
| `GET` | `/api/v1/orders/{orderId}` | privada | path param | `OrderDetailResponse` |
| `PATCH` | `/api/v1/orders/{orderId}` | privada | `{ "status": "CONFIRMED" }` | `OrderDetailResponse` |
| `POST` | `/api/v1/orders/{orderId}/payments` | privada | `{ "amount": 15000, "method": "TRANSFER", "paidAt": "2026-05-23T20:15:30Z", "reference": "TRX-781", "notes": "Abono inicial" }` | `{ "paymentId": "uuid", "balanceDue": 4990, "paymentStatus": "PARTIALLY_PAID" }` |

## Products

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/products` | privada | `page`, `size`, `search?`, `category?`, `active?` | paginado de `ProductResponse` |
| `POST` | `/api/v1/products` | privada | `{ "name": "Plan Basico", "sku": "PB-001", "category": "PLAN", "description": "Incluye instalacion", "price": 19990, "active": true }` | `ProductResponse` |
| `GET` | `/api/v1/products/{productId}` | privada | path param | `ProductResponse` |
| `PATCH` | `/api/v1/products/{productId}` | privada | mismo body que `POST` | `ProductResponse` |

## Automation Rules

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/automation-rules` | privada | `page`, `size`, `search?`, `triggerType?`, `active?` | paginado de `AutomationRuleResponse` |
| `POST` | `/api/v1/automation-rules` | privada | `{ "name": "Seguimiento despues de palabra precio", "triggerType": "KEYWORD_MATCH", "keyword": "precio", "delayMinutes": 5, "actionType": "SEND_TEMPLATE", "templateId": "uuid", "assignedUserId": null, "active": true }` | `AutomationRuleResponse` |
| `GET` | `/api/v1/automation-rules/{ruleId}` | privada | path param | `AutomationRuleResponse` |
| `PATCH` | `/api/v1/automation-rules/{ruleId}` | privada | mismo body que `POST` | `AutomationRuleResponse` |
| `PATCH` | `/api/v1/automation-rules/{ruleId}/status` | privada | `{ "active": false }` | `{ "id": "uuid", "active": false }` |
| `POST` | `/api/v1/automation-rules/{ruleId}/test` | privada | `{ "sampleMessage": "Hola, quiero saber el precio", "sampleConversationId": null }` | `{ "matched": true, "scheduledAction": { "actionType": "SEND_TEMPLATE", "templateId": "uuid", "delayMinutes": 5 }, "debug": [ "keyword matched" ] }` |

## Reports

| Metodo | Ruta | Auth | Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/reports/overview` | privada | `from`, `to`, `ownerUserId?`, `channel=WHATSAPP` | `{ "kpis": {}, "conversationBreakdown": [], "orderBreakdown": [], "appointmentBreakdown": [] }` |

## Administration

### Admin summary

| Metodo | Ruta | Auth | Response |
| --- | --- | --- | --- |
| `GET` | `/api/v1/admin/summary` | privada, roles `OWNER` o `ADMIN` | `{ "company": { "id": "uuid", "companyName": "Demo Spa" }, "users": { "total": 8, "active": 7 }, "whatsapp": { "status": "CONNECTED" }, "security": { "sessionTimeoutMinutes": 30 } }` |

### Company

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/company` | privada, roles `OWNER` o `ADMIN` | sin body | `{ "id": "uuid", "companyName": "Demo Spa", "businessName": "Demo", "timezone": "America/Santiago", "currency": "CLP", "contactEmail": "hola@demo.cl", "supportPhone": "+56911112222", "address": "Santiago" }` |
| `PATCH` | `/api/v1/company` | privada, roles `OWNER` o `ADMIN` | mismo body que `GET` sin `id` | mismo response que `GET` |

### Users and roles

| Metodo | Ruta | Auth | Request o Query | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/admin/users` | privada, roles `OWNER` o `ADMIN` | `page`, `size`, `search?`, `role?`, `active?` | paginado de `AdminUserResponse` |
| `POST` | `/api/v1/admin/users` | privada, roles `OWNER` o `ADMIN` | `{ "firstName": "Pia", "lastName": "Diaz", "email": "pia@demo.cl", "phone": "+56933334444", "role": "AGENT", "active": true }` | `AdminUserResponse` |
| `GET` | `/api/v1/admin/users/{userId}` | privada, roles `OWNER` o `ADMIN` | path param | `AdminUserResponse` |
| `PATCH` | `/api/v1/admin/users/{userId}` | privada, roles `OWNER` o `ADMIN` | mismo body que `POST` | `AdminUserResponse` |
| `GET` | `/api/v1/admin/roles` | privada, roles `OWNER` o `ADMIN` | sin body | `{ "items": [ "OWNER", "ADMIN", "AGENT", "SALES" ] }` |

### Security policy

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/admin/security` | privada, rol `OWNER` | sin body | `{ "sessionTimeoutMinutes": 30, "passwordMinLength": 8, "requireUppercase": true, "requireNumber": true, "requireSymbol": false, "maxFailedLoginAttempts": 5 }` |
| `PATCH` | `/api/v1/admin/security` | privada, rol `OWNER` | mismo body que `GET` | mismo response que `GET` |

## Control del canal WhatsApp expuesto al frontend

Estos endpoints son del backend Java hacia el frontend. El canal es nativo del backend (proveedor `META_CLOUD_API` o `SIMULATED`, configurado con `APP_WHATSAPP_CHANNEL_PROVIDER`); la UI nunca consume un servicio externo. No existe QR ni sesion de dispositivo.

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/whatsapp-channel/status` | privada, roles `OWNER` o `ADMIN` | sin body | `{ "provider": "SIMULATED", "connectionStatus": "CONNECTED", "phoneNumber": "+56911112222", "phoneNumberId": null, "adapterMode": "SIMULATED", "lastEventAt": "2026-05-23T20:15:30Z", "active": true, "recentEventCount": 3, "recentErrorCount": 0, "recentEvents": [ ... ], "message": "..." }` |
| `POST` | `/api/v1/whatsapp-channel/connect` | privada, roles `OWNER` o `ADMIN` | sin body | mismo shape que `status` |
| `POST` | `/api/v1/whatsapp-channel/disconnect` | privada, roles `OWNER` o `ADMIN` | sin body | mismo shape que `status` |
| `POST` | `/api/v1/whatsapp-channel/test-message` | privada, roles `OWNER` o `ADMIN` | `{ "recipientPhone": "+56911112222", "body": "Mensaje de prueba" }` | `{ "messageId": "uuid", "status": "SENT" }` |

Los cuatro endpoints tambien estan disponibles bajo el alias `/api/channels/whatsapp-channel/...`.

### Configuracion del canal

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/configuration/whatsapp` | privada, roles `OWNER`, `ADMIN` o `SUPERVISOR` | sin body | configuracion actual del canal |
| `PATCH` | `/api/v1/configuration/whatsapp/preferences` | privada, roles `OWNER` o `ADMIN` | preferencias del canal | configuracion actualizada |
| `POST` | `/api/v1/configuration/whatsapp/connect` | privada, roles `OWNER` o `ADMIN` | sin body | `{ "status": "ACCEPTED" }` |
| `POST` | `/api/v1/configuration/whatsapp/disconnect` | privada, roles `OWNER` o `ADMIN` | sin body | `{ "status": "ACCEPTED" }` |

No existe `/refresh-qr`.

## Mensajes entrantes al canal

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/integrations/whatsapp-cloud/webhook` | Verify Token (query) | `hub.mode`, `hub.verify_token`, `hub.challenge` | `200` con `hub.challenge` como texto plano |
| `POST` | `/api/v1/integrations/whatsapp-cloud/webhook` | HMAC-SHA256 (X-Hub-Signature-256) | Payload JSON de Meta Cloud API | `{ "status": "ACCEPTED" }` |

### Headers requeridos del webhook WhatsApp Cloud API

- `X-Hub-Signature-256` (HMAC-SHA256 del body con App Secret)

## Simulador de mensajes entrantes

En local (proveedor `SIMULATED`) se simulan mensajes entrantes sin Meta ni sesion de dispositivo:

| Metodo | Ruta | Auth | Request | Response |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/test/whatsapp-inbound` | simulacion local | `{ "from": "+56911112222", "body": "Hola, quiero una reserva" }` | `{ "status": "ACCEPTED" }` |

## Reglas de negocio reflejadas en API

- El frontend no envia `businessId` en requests privadas; el backend lo deriva del usuario autenticado.
- Las operaciones administrativas siguen filtrando por `business_id`.
- Las acciones destructivas de Fase 1 son soft delete o desactivacion logica.
- Las pruebas de reglas no envian mensajes reales al canal.
- Las respuestas de errores de validacion deben ser compatibles con React Hook Form + Zod.
