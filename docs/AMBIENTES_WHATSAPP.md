# Ambientes WhatsApp

## Objetivo

La aplicacion queda desacoplada mediante el puerto Java `CanalWhatsApp`.

- Ambiente local: `WhatsAppWebAdapter` usa `whatsapp-web.js`, Puppeteer, Chromium, Xvfb y noVNC.
- Ambiente productivo: `WhatsAppCloudApiAdapter` queda preparado para WhatsApp Cloud API. Puede operar en modo `dry-run` para validar despliegue sin credenciales reales.

## Ambiente local

Comando recomendado:

```bash
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

Tambien se puede usar:

```bash
docker compose up --build
```

Servicios locales:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080/actuator/health
- Servicio WhatsApp Web: http://localhost:3001/health
- Navegador visual noVNC: http://localhost:6080/vnc.html?autoconnect=true&resize=scale

Variables clave:

```text
APP_WHATSAPP_CHANNEL_PROVIDER=WEB
APP_WHATSAPP_WEB_ENABLED=true
APP_WHATSAPP_CLOUD_API_ENABLED=false
WHATSAPP_WEB_VISUAL_MODE=true
WHATSAPP_WEB_HEADLESS=false
WHATSAPP_WEB_CLEAN_PROFILE_LOCKS_ON_START=true
WHATSAPP_WEB_KILL_ORPHAN_CHROMIUM_ON_START=true
```

### Correccion de perfil Chromium bloqueado

El servicio local elimina archivos de bloqueo conocidos de Chromium antes de iniciar `whatsapp-web.js` y puede cerrar procesos huérfanos de Chromium dentro del contenedor. Esto mitiga errores como perfil ocupado por otro proceso.

Si el volumen quedara corrupto, ejecutar:

```bash
docker compose down
docker volume rm asistente-whatsapp-mvp_whatsapp-webjs-session-data asistente-whatsapp-mvp_whatsapp-webjs-cache-data
docker compose up --build
```

### Correccion de resolucion DNS

El servicio local define DNS explicito en Docker Compose:

```yaml
dns:
  - 1.1.1.1
  - 8.8.8.8
```

Si la red corporativa bloquea WhatsApp Web, el contenedor seguira sin poder abrir `https://web.whatsapp.com/` aunque la aplicacion este correctamente configurada.

## Ambiente productivo

Comando recomendado:

```bash
cp .env.production.example .env.production
docker compose --env-file .env.production -f docker-compose.prod.yml up --build
```

Servicios productivos incluidos:

- `postgres`
- `backend-java`
- `frontend-react`

Servicios productivos excluidos:

- `whatsapp-web-service`
- `Chromium`
- `Xvfb`
- `noVNC`

Variables clave:

```text
APP_WHATSAPP_CHANNEL_PROVIDER=CLOUD_API
APP_WHATSAPP_WEB_ENABLED=false
APP_WHATSAPP_CLOUD_API_ENABLED=true
APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED=false
```

Para validar despliegue sin credenciales reales de Cloud API:

```text
APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED=true
```

## Componentes Java agregados

- `com.asistentewhatsapp.channels.domain.CanalWhatsApp`
- `com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebAdapter`
- `com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudApiAdapter`
- `com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudApiProperties`
- `com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookController`
- `com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookValidator`
- `com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookParser`
- `com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudApiMetrics`
- `com.asistentewhatsapp.channels.application.WhatsAppInboundMessageService`
- `com.asistentewhatsapp.channels.application.WhatsAppDeliveryStatusService`

## Variables de entorno WhatsApp Cloud API

| Variable | Descripcion | Default |
|---|---|---|
| `APP_WHATSAPP_CLOUD_API_ENABLED` | Habilita el adaptador Cloud API | `false` |
| `APP_WHATSAPP_CLOUD_API_BASE_URL` | URL base de Graph API | `https://graph.facebook.com` |
| `APP_WHATSAPP_CLOUD_API_VERSION` | Version de Graph API | `v23.0` |
| `APP_WHATSAPP_CLOUD_API_PHONE_NUMBER_ID` | ID del numero de telefono en Meta | `` |
| `APP_WHATSAPP_CLOUD_API_BUSINESS_ACCOUNT_ID` | WABA ID (WhatsApp Business Account ID) | `` |
| `APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN` | Token de acceso del system user | `` |
| `APP_WHATSAPP_CLOUD_API_WEBHOOK_VERIFY_TOKEN` | Token para verificacion del webhook (GET) | `` |
| `APP_WHATSAPP_CLOUD_API_APP_SECRET` | App Secret de la app de Meta | `` |
| `APP_WHATSAPP_CLOUD_API_WEBHOOK_SIGNATURE_REQUIRED` | Exigir firma HMAC en webhooks | `true` |
| `APP_WHATSAPP_CLOUD_API_CONNECT_TIMEOUT_SECONDS` | Timeout de conexion a Graph API | `5` |
| `APP_WHATSAPP_CLOUD_API_READ_TIMEOUT_SECONDS` | Timeout de lectura de Graph API | `15` |
| `APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED` | Modo simulado sin llamadas externas | `false` |
| `APP_WHATSAPP_CLOUD_API_DEFAULT_PHONE_NUMBER` | Numero por defecto para el negocio | `56950954580` |

## Migracion de WhatsApp Web a Cloud API

1. Configurar las variables de Cloud API en el ambiente.
2. Ejecutar `docker compose -f docker-compose.prod.yml up --build` con `APP_WHATSAPP_CLOUD_API_ENABLED=true` y `APP_WHATSAPP_WEB_ENABLED=false`.
3. Opcional: usar `APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED=true` para validar despliegue sin token real.
4. Configurar el webhook en el dashboard de Meta apuntando a `https://tudominio.cl/api/v1/integrations/whatsapp-cloud/webhook`.
5. Verificar que los mensajes entrantes y salientes fluyen correctamente.

## Rollback

Para volver a WhatsApp Web:
1. Detener el stack con `docker compose -f docker-compose.prod.yml down`.
2. Cambiar `APP_WHATSAPP_CLOUD_API_ENABLED=false` y `APP_WHATSAPP_WEB_ENABLED=true`, `APP_WHATSAPP_CHANNEL_PROVIDER=WEB`.
3. Los datos de conversaciones, clientes y mensajes se conservan intactos.

## Tipos de mensaje soportados (Cloud API)

- `text` - Mensaje de texto
- `interactive.button_reply` - Respuesta de boton interactivo
- `interactive.list_reply` - Respuesta de lista interactiva
- `button` - Respuesta de boton simple
- `image` - Imagen (solo metadata, sin descarga automatica)
- `document` - Documento (solo metadata)
- `audio` - Audio/nota de voz (solo metadata)
- `video` - Video (solo metadata)
- `sticker` - Sticker (solo metadata)
- `location` - Ubicacion
- `contacts` - Contacto compartido
- Los tipos no soportados se registran de forma segura sin causar reintentos infinitos.

## Regla arquitectonica

El frontend y los casos de uso no dependen de `whatsapp-web.js`. Solo dependen del backend y del puerto `CanalWhatsApp`. Por esto, el cambio desde WhatsApp Web local hacia Cloud API productivo no requiere reescribir conversaciones, clientes, mensajes ni auditoria.
