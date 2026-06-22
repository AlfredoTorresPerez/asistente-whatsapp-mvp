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

## Regla arquitectonica

El frontend y los casos de uso no dependen de `whatsapp-web.js`. Solo dependen del backend y del puerto `CanalWhatsApp`. Por esto, el cambio desde WhatsApp Web local hacia Cloud API productivo no requiere reescribir conversaciones, clientes, mensajes ni auditoria.
