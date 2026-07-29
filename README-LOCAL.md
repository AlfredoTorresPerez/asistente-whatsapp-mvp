# Asistente WhatsApp MVP — Entorno Local

## Modos de uso local

| Modo | Comando | Requisitos | Auto-respuesta IA |
|------|---------|------------|-------------------|
| **Demo base** (sin WhatsApp real) | `docker compose -f docker-compose.local.yml up -d` | Solo Docker | Desactivada por defecto |
| **Demo con túnel público** | `docker compose -f docker-compose.local.yml --profile public-link up -d` | + Cloudflare Tunnel automático | Desactivada por defecto |
| **Demo con WhatsApp Web** | `docker compose -f docker-compose.local.yml --profile whatsapp up -d` | + Escanear QR en /admin/whatsapp-web | Desactivada por defecto |
| **Debug auto-respuesta IA** | `APP_AI_AGENTS_AUTO_REPLY_ENABLED=true` + modo WhatsApp | + Sesión WhatsApp CONNECTED | Activar manualmente en `.env.local` |

> **Importante:** La auto-respuesta IA (`APP_AI_AGENTS_AUTO_REPLY_ENABLED`) está desactivada por defecto en entorno local.
> Para probarla, ver `docs/DEBUGGING_AUTO_REPLY_LOCAL.md`.

## Gestión de Secretos (Windows Credential Manager)

Los secretos reales (JWT, WhatsApp App Secret, Access Token, Gmail App Password y OpenAI API Key) **no están en ningún archivo**.
Se almacenan cifrados con DPAPI en Windows Credential Manager.
Sentry es opcional: si existe `SENTRY_DSN` en Credential Manager, `restore-local-secrets.ps1` lo restaura y activa `SENTRY_ENABLED=true`; si no existe, queda desactivado sin romper el arranque.
El email local usa doble entrega: Mailpit captura todos los correos en `http://localhost:8025` y, si `APP_EMAIL_MIRROR_ENABLED=true`, también se envían por Gmail usando el password guardado como `GMAIL_PASSWORD`.

```powershell
# 1. Guardar/actualizar secretos (solo la primera vez)
.\scripts\store-local-secrets.ps1

# 2. Se restauran automáticamente al usar local-start.ps1
.\scripts\local-start.ps1
```

Si necesitas levantar con `docker compose` directo, los secretos deben estar
como variables de entorno en la shell o escritos directamente en `.env.local`.

## Levantar en 3 Comandos

```powershell
# 1. Guardar secretos en Windows Credential Manager (solicita los valores)
.\scripts\store-local-secrets.ps1

# 2. Iniciar servicios (restaura secretos automáticamente)
.\scripts\local-start.ps1

# 3. Ver logs
docker compose logs -f --tail=100
```

Frontend: http://localhost:5173
Backend:  http://localhost:8080
API Doc:  http://localhost:8080/swagger-ui.html

## Con WhatsApp Web

```bash
# Incluye whatsapp-web-service (Puppeteer + Chromium)
docker compose -f docker-compose.local.yml --profile whatsapp up -d

# Escanear QR desde la UI: http://localhost:5173/admin/whatsapp-web
```

## Con Túnel Público (trycloudflare.com)

Expone el frontend y backend local mediante Cloudflare Tunnel para acceder desde Internet.

```bash
# Iniciar con túnel público
docker compose -f docker-compose.local.yml --profile public-link up -d

# O usando el script automatizado (recomendado)
.\scripts\start-public-link.ps1
```

El script `start-public-link.ps1`:
1. Inicia los servicios locales (postgres + backend + frontend)
2. Crea el túnel Cloudflare
3. Espera la URL pública
4. Actualiza `.env.local` con las URLs públicas
5. Recrea backend y frontend para que usen la nueva URL

### Comandos del túnel

```bash
# Regenerar enlace expirado
docker compose -f docker-compose.local.yml --profile public-link up -d --force-recreate public-tunnel

# Consultar nueva dirección
docker compose -f docker-compose.local.yml logs --tail=200 public-tunnel

# Seguir registros
docker compose -f docker-compose.local.yml logs -f --tail=100 public-tunnel

# Detener solamente el túnel (servicios locales continúan)
.\scripts\stop-public-link.ps1
# o
docker compose -f docker-compose.local.yml --profile public-link stop public-tunnel

# Verificar estado
.\scripts\check-public-link.ps1
```

### ⚠️ Advertencias

- **trycloudflare.com es TEMPORAL.** La dirección cambia al reiniciar el contenedor cloudflared.
- **No usar en QA ni producción.** Solo para demos locales.
- **No registrar como webhook permanente de WhatsApp** — la URL expira.
- **No registrar como URL de devolución de Google Calendar** — la URL expira.
- **No incluir en correos o enlaces persistentes.**
- Los enlaces de confirmación de reservas generados con la URL anterior quedarán rotos.

## Puertos y Servicios

| Servicio | Puerto Host | Interno | Perfil | Descripción |
|----------|-------------|---------|--------|-------------|
| postgres | 5433 | 5432 | — | PostgreSQL 16 |
| backend-java | 8080 | 8080 | — | Spring Boot 3 |
| frontend-react | 5173 | 5173 | — | Vite dev server |
| whatsapp-web-service | 3001 | 3001 | `whatsapp` | whatsapp-web.js + Chromium |
| public-tunnel | — | — | `public-link` | Cloudflare Tunnel (trycloudflare.com) |

## Comandos Útiles

```powershell
# PowerShell (recomendado)
.\scripts\store-local-secrets.ps1  # guardar/actualizar secretos en Credential Manager
.\scripts\local-start.ps1           # levantar servicios base (restaura secretos)
.\scripts\local-start.ps1 -Profile whatsapp   # levantar con WhatsApp Web
.\scripts\local-start.ps1 -Build    # reconstruir imágenes y levantar
# Nota: local-start.ps1 restaura automáticamente los secretos desde Credential Manager

.\scripts\dev.ps1 up              # levantar servicios base (sin restaurar secretos)
.\scripts\dev.ps1 up:whatsapp     # levantar con WhatsApp Web
.\scripts\dev.ps1 logs            # seguir logs
.\scripts\dev.ps1 down            # detener (preserva volúmenes)
.\scripts\dev.ps1 reset           # borrar volúmenes + rebuild + up
.\scripts\dev.ps1 ps              # estado de servicios
.\scripts\dev.ps1 build           # reconstruir imágenes

# npm (desde frontend-react/)
cd frontend-react
pnpm run docker:up                # levantar servicios base
pnpm run docker:up:whatsapp       # levantar con WhatsApp Web
pnpm run docker:logs              # seguir logs
pnpm run docker:down              # detener
pnpm run docker:reset             # borrar volúmenes + rebuild + up
pnpm run docker:ps                # estado de servicios

# Docker Compose directo (desde la raíz)
# NOTA: si usas docker compose directo, primero restaura secretos manualmente:
#   .\scripts\restore-local-secrets.ps1
# (setea las variables de entorno que docker compose heredará)
docker compose --env-file .env.local -f docker-compose.local.yml up -d
docker compose --env-file .env.local -f docker-compose.local.yml --profile whatsapp up -d
docker compose --env-file .env.local -f docker-compose.local.yml --profile public-link up -d
docker compose --env-file .env.local -f docker-compose.local.yml logs -f --tail=100
docker compose --env-file .env.local -f docker-compose.local.yml down
docker compose --env-file .env.local -f docker-compose.local.yml down -v   # borrar volúmenes
docker compose --env-file .env.local -f docker-compose.local.yml ps
```

## Personalización Local

Crea `docker-compose.override.yml` (gitignorado) para sobreescribir settings sin
modificar `docker-compose.local.yml`. Ver `docker-compose.override.yml.example`.

### Ejemplos de override

```yaml
# Habilitar JDWP debug port
services:
  backend-java:
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    ports:
      - "5005:5005"
```

## Volúmenes

| Volumen | Directorio Host (bind mount) | Propósito |
|---------|-----------------------------|-----------|
| postgres-data | volumen Docker anónimo | Datos persistentes de PostgreSQL |
| whatsapp-webjs-session-data | `.docker/volumes/whatsapp-session` | Sesión autenticada de WhatsApp Web |
| whatsapp-webjs-cache-data | `.docker/volumes/whatsapp-cache` | Cache de whatsapp-web.js |

Los volúmenes de WhatsApp Web se crean automáticamente como bind mounts en
`.docker/volumes/`. En Linux, si el usuario `node` (uid=1000) no puede escribir,
ejecuta:

```bash
mkdir -p .docker/volumes/whatsapp-session .docker/volumes/whatsapp-cache
sudo chown -R 1000:1000 .docker/volumes/whatsapp-session .docker/volumes/whatsapp-cache
```

En Docker Desktop (Windows/Mac) los permisos se manejan automáticamente.

## Documentación Relacionada

- `docs/DEBUGGING_AUTO_REPLY_LOCAL.md` — Debug del flujo auto-reply IA
- `CHECKLIST_DEMO_LOCAL.md` — Checklist para demo funcional
- `DEMO_GUIDE.md` — Guía de demo

## Scripts del Túnel Público

| Script | Función |
|--------|---------|
| `scripts/start-public-link.ps1` | Inicia túnel, detecta URL, actualiza `.env.local` |
| `scripts/stop-public-link.ps1` | Detiene solo el túnel (servicios locales continúan) |
| `scripts/check-public-link.ps1` | Verifica estado y validez del túnel |
| `scripts/start_mvp_public_link.ps1` | (Legado) Versión anterior, migrar a start-public-link.ps1 |
