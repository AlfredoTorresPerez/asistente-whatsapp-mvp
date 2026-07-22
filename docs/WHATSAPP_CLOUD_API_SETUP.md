# Configuración de WhatsApp Cloud API (Meta)

Este documento describe la configuración de **WhatsApp Business Platform Cloud API de Meta** para el MVP multicliente.

## Arquitectura: un número central por empresa

```
Empresa A → WABA propia → 1 número central → N sucursales
Empresa B → WABA propia → 1 número central → N sucursales
```

- Cada empresa tiene un `channel_account` con `provider_name = META_CLOUD_API`, `routing_mode = CENTRALIZED`, `location_id = null`.
- `phone_number_id` enruta los webhooks a la empresa correcta.
- La sucursal se asigna a la conversación (`conversation.location_id`), no al canal.
- Las credenciales (token, WABA ID, Phone Number ID) son por empresa y se almacenan cifradas en BD.
- Los `.env` contienen solo la configuración de la Meta App del SaaS, no credenciales de cada tenant.

## Diferencia entre QR de WhatsApp Web y QR comercial

| Característica | QR de WhatsApp Web | QR comercial |
|---|---|---|
| Propósito | Vincular un dispositivo a WhatsApp Web | Que el cliente abra WhatsApp y envíe un mensaje |
| Para qué sirve | Recibir/Enviar mensajes vía Web | Iniciar conversación con el número central |
| Aplica a | WHATSAPP_WEB | META_CLOUD_API |
| Formato | Imagen generada por WhatsApp | URL `https://wa.me/{numero}?text={mensaje}` |
| Es sesión | Sí, autentica la conexión | No, solo abre WhatsApp |

**META_CLOUD_API nunca muestra QR de sesión.** Solo genera QR comerciales por sucursal.

## Configuración por ambiente

| Ambiente | Perfil | Proveedor | Uso |
|---|---|---|---|
| local | `local` (default) | WEB o META_CLOUD_API | Desarrollo, pruebas simuladas |
| qa | `qa` | META_CLOUD_API | Validación con Meta real |
| production | `production` | META_CLOUD_API | Producción real |

Seleccionar con `SPRING_PROFILES_ACTIVE=local|qa|production`.

## Qué va en el ambiente vs qué va en el tenant

### En el ambiente (`.env`, `application-*.yml`)
- Meta App ID, App Secret (de la Meta App del SaaS)
- Webhook Verify Token, Webhook Public URL
- `credential-encryption-secret`
- Configuración de red, BD, JWT, etc.

### En el tenant (base de datos `channel_account`)
- `phone_number_id` (único global)
- `provider_account_id` (WABA ID)
- `encrypted_access_token` (cifrado con AES-256-GCM)
- `display_phone_number`, `normalized_phone_number`, `verified_name`
- Estados: `registration_status`, `operational_status`, `webhook_status`, `credential_status`

**No usar globalmente:** `APP_WHATSAPP_CLOUD_API_PHONE_NUMBER_ID`, `APP_WHATSAPP_CLOUD_API_BUSINESS_ACCOUNT_ID`, `APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN`.

## Variables obligatorias por ambiente

### Backend (Spring Boot)

| Variable | local | qa | production |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | local | qa | production |
| `APP_WHATSAPP_CHANNEL_PROVIDER` | META_CLOUD_API | META_CLOUD_API | META_CLOUD_API |
| `APP_WHATSAPP_CLOUD_API_ENABLED` | true | true | true |
| `APP_WHATSAPP_CLOUD_API_APP_ID` | opcional | **requerido** | **requerido** |
| `APP_WHATSAPP_CLOUD_API_APP_SECRET` | opcional | **requerido** | **requerido** |
| `APP_WHATSAPP_CLOUD_API_WEBHOOK_VERIFY_TOKEN` | opcional | **requerido** | **requerido** |
| `APP_WHATSAPP_CLOUD_API_CREDENTIAL_ENCRYPTION_SECRET` | opcional | **requerido** | **requerido** |

### Frontend (se inyectan en build)

| Variable | Propósito |
|---|---|
| `VITE_META_APP_ID` | Meta App ID para Embedded Signup |
| `VITE_META_EMBEDDED_SIGNUP_CONFIG_ID` | Config ID de Embedded Signup |
| `VITE_META_GRAPH_API_VERSION` | Versión de Graph API (v23.0) |

> Nota: Las variables `VITE_` se incorporan al bundle durante `pnpm build`. No funcionan si se establecen solo como variables de entorno del contenedor nginx.

## Flujo Embedded Signup

### Frontend
1. Carga el SDK de Facebook.
2. Inicializa `FB.init()` con `appId` y versión de Graph API.
3. Escucha eventos `message` de `https://www.facebook.com` y `https://business.facebook.com` para capturar WABA ID y Phone Number ID.
4. Llama a `FB.login()` con `config_id` y `response_type: 'code'`.
5. Valida el `origin` de los mensajes.
6. Limpia listeners y timeouts al completar o fallar.

### Backend (`POST /api/v1/integrations/whatsapp-cloud/onboarding/complete`)
```json
{
  "code": "código-temporal",
  "redirectUri": "https://tudominio.com",
  "wabaId": "123456789",
  "phoneNumberId": "987654321"
}
```
1. Valida `redirectUri` contra lista permitida por ambiente.
2. Intercambia código por token vía `POST /oauth/access_token`.
3. Consulta WABA (`GET /{wabaId}`) para verificar que existe y es accesible.
4. Consulta Phone Number (`GET /{phoneNumberId}`) y verifica que pertenece al WABA.
5. Valida que `phoneNumberId` no esté asociado a otro tenant.
6. Suscribe la app al WABA (`POST /{wabaId}/subscribed_apps`).
7. Registra el número (`POST /{phoneNumberId}/register`).
8. Configura PIN de dos pasos (`POST /{phoneNumberId}/two_step_pin`).
9. Cifra el token y persiste en `channel_account`.
10. Guarda `display_phone_number`, `normalized_phone_number`, `verified_name`.

### Revalidación (`POST /api/v1/integrations/whatsapp-cloud/revalidate`)
- Verifica token, WABA, teléfono, actualiza metadatos.
- Marca `last_health_check_at`.
- No marca `webhook_status=SUBSCRIBED` solo porque se pudo consultar el teléfono.

### Desconexión
- `disconnect`: solo local (elimina credenciales, desactiva canal).
- `disconnect-from-meta`: también desuscribe la app del WABA en Meta.

## Webhooks

### Verificación (GET)
`/api/v1/integrations/whatsapp-cloud/webhook?hub.mode=subscribe&hub.verify_token=...&hub.challenge=...`
- Comparación en tiempo constante del verify_token.

### Recepción (POST)
1. Valida firma `X-Hub-Signature-256` con comparación en tiempo constante.
2. Límite de 100KB por payload.
3. Idempotencia:
   - Mensajes: `message.id` como clave única.
   - Estados: `status.id + "-" + status.status + "-" + timestamp` hasheado.
   - Un `message.id` repetido se ignora; no bloquea otros mensajes del mismo payload.
4. `phone_number_id` desconocido: usa `continue`, no aborta todo el payload.
5. Si no se puede persistir durablemente, devuelve código de error para que Meta reintente.

### Configuración en Meta Developer Dashboard
- URL: `https://tudominio.com/api/v1/integrations/whatsapp-cloud/webhook`
- Token: el mismo de `APP_WHATSAPP_CLOUD_API_WEBHOOK_VERIFY_TOKEN`
- Eventos: `messages`, `message_deliveries`, `message_reads`

## Permisos y App Review de Meta

Se requieren los siguientes permisos de Meta:
- `whatsapp_business_messaging`
- `whatsapp_business_management`
- `business_management`

La App de Meta debe pasar **App Review** para que el Embedded Signup funcione con cuentas de terceros.
En modo desarrollo solo funciona con administradores/testers de la app.

## Plantillas y ventana de 24 horas

- Dentro de la ventana de 24h desde el último mensaje del cliente: se permite texto libre.
- Fuera de la ventana: se debe usar una plantilla aprobada por Meta.
- Configurar nombres de plantillas por ambiente o tenant en `channel_account` o properties.

## QR comercial por sucursal

Generación determinística:

```
https://wa.me/{numeroCentral}?text={mensajeCodificado}
```

Ejemplo:
```
https://wa.me/56927305158?text=SEDE%3APRINCIPAL%20Quiero%20realizar%20una%20reserva
```

El mensaje incluye `SEDE:{locationCode}` para que la IA detecte la sucursal automáticamente.

Endpoint: `GET /api/v1/business-locations/{locationId}/commercial-qr`

## Rotación de secretos

1. Generar nuevo token en Meta Developer Dashboard.
2. Actualizar en BD (`channel_account.encrypted_access_token`) mediante el endpoint de revalidación o directamente.
3. Rotar `credential-encryption-secret` requiere descifrar y volver a cifrar todos los tokens almacenados.

## Runbook de errores

| Código | Error | Acción |
|---|---|---|
| `META_OAUTH_FAILED` | Error al intercambiar código | Verificar App ID, App Secret, redirect URI |
| `META_WABA_FETCH_FAILED` | No se puede consultar WABA | Verificar que el token tenga permisos sobre el WABA |
| `META_PHONE_FETCH_FAILED` | No se puede consultar número | Verificar Phone Number ID |
| `PHONE_NUMBER_ID_ALREADY_IN_USE` | Número ya asociado a otro tenant | Desconectar del otro tenant primero |
| `META_SUBSCRIBE_FAILED` | No se pudo suscribir la app | Verificar permisos de la app sobre el WABA |
| `META_PHONE_REGISTER_FAILED` | No se pudo registrar el número | Verificar PIN de dos pasos |
| `TOKEN_DECRYPT_FAILED` | No se pudo descifrar el token | Verificar `credential-encryption-secret` |
| `META_UNREACHABLE` | Meta no responde | Verificar conectividad, token vigente |

## Promoción local → QA → producción

1. **Nunca** copiar `.env` entre ambientes.
2. Cada ambiente tiene su propia Meta App (o al menos sus propios secrets).
3. En local se puede usar `dry-run=true` o WhatsApp Web.
4. QA y producción usan `META_CLOUD_API` con sus propias credenciales de tenant.
5. Las migraciones Flyway son las mismas para todos los ambientes.

## Pruebas

Todas las pruebas usan datos simulados, sin tráfico real a Meta:

```bash
# Backend
cd backend-java
./mvnw test -Dtest="*WhatsAppCloud*"
./mvnw test -Dtest="*ChannelDispatch*"
./mvnw test -Dtest="*MetaOnboarding*"

# Frontend
cd frontend-react
pnpm test
pnpm lint
pnpm build
```

## Endpoints de onboarding

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/integrations/whatsapp-cloud/onboarding/complete` | Completa el onboarding con el código de Meta |
| GET | `/api/v1/integrations/whatsapp-cloud/status` | Estado actual del canal Cloud API |
| POST | `/api/v1/integrations/whatsapp-cloud/revalidate` | Revalida la conexión contra Meta |
| POST | `/api/v1/integrations/whatsapp-cloud/disconnect` | Desconexión local |
| POST | `/api/v1/integrations/whatsapp-cloud/disconnect-from-meta` | Desconexión local + Meta |

## Seguridad multitenant

- El `businessId` nunca se obtiene del request del cliente.
- Se extrae del token JWT del usuario autenticado en todos los endpoints.
- Un usuario de Empresa A nunca puede enviar mensajes con credenciales de Empresa B.
- Rate limiting en endpoints de onboarding y envío.
- No se exponen tokens, PIN, App Secret ni cuerpos completos en logs.
