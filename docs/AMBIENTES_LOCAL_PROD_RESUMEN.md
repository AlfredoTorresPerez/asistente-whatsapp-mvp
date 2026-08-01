# Ambientes local y produccion

## Ambiente local

Usa:

- backend Java 21 + Spring Boot
- frontend React + TypeScript + Vite
- PostgreSQL 16 (puerto 5433)
- mailpit para correo (1025/8025)
- canal WhatsApp con proveedor `SIMULATED` embebido en el backend (sin Chromium, sin Xvfb, sin noVNC, sin servicio externo ni QR)

Archivo principal:

```powershell
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

Servicios locales:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- PostgreSQL: localhost:5433

No existe servicio en el puerto 3001.

## Produccion

Usa el puerto abstracto `CanalWhatsApp`.

Implementaciones:

- `SimulatedWhatsAppProvider`: implementacion embebida para local/demo (proveedor `SIMULATED`).
- Proveedor `META_CLOUD_API`: implementacion productiva para WhatsApp Cloud API de Meta.

Archivo principal:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml up --build
```

En produccion no se levanta Chromium, Xvfb ni noVNC; el canal usa la Cloud API de Meta.

## Seleccion de proveedor

La variable `APP_WHATSAPP_CHANNEL_PROVIDER` selecciona el proveedor (default `SIMULATED`). El arranque falla rapido si el proveedor configurado no tiene bean disponible.

Para local (default):

```env
APP_WHATSAPP_CHANNEL_PROVIDER=SIMULATED
```

Para produccion:

```env
APP_WHATSAPP_CHANNEL_PROVIDER=META_CLOUD_API
```

No existen variables de entorno del antiguo modelo de adaptador web.
