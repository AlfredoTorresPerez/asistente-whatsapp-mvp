# Ambientes local y produccion

## Ambiente local

Usa:

- backend Java 21 + Spring Boot
- frontend React + TypeScript + Vite
- PostgreSQL 16 Alpine
- whatsapp-web.js
- Puppeteer
- Chromium
- Xvfb
- noVNC

Archivo principal:

```powershell
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

El servicio local de WhatsApp expone:

- API interna: http://localhost:3001
- visor visual: http://localhost:6080/vnc.html?autoconnect=true&resize=scale

## Produccion

Usa el puerto abstracto `CanalWhatsApp`.

Implementaciones:

- `WhatsAppWebAdapter`: implementacion local basada en whatsapp-web.js.
- `WhatsAppCloudApiAdapter`: implementacion productiva/futura para WhatsApp Cloud API.

Archivo principal:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml up --build
```

En produccion no se levanta Chromium, Xvfb, noVNC ni whatsapp-web.js.

## Seleccion de proveedor

Variables relevantes:

```env
APP_WHATSAPP_CHANNEL_PROVIDER=WEB
APP_WHATSAPP_WEB_ENABLED=true
APP_WHATSAPP_CLOUD_API_ENABLED=false
```

Para produccion futura:

```env
APP_WHATSAPP_CHANNEL_PROVIDER=CLOUD_API
APP_WHATSAPP_WEB_ENABLED=false
APP_WHATSAPP_CLOUD_API_ENABLED=true
```
