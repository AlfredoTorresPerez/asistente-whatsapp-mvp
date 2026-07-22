# Asistente WhatsApp MVP — Entorno Local

## Modos de uso local

| Modo | Comando | Requisitos | Auto-respuesta IA |
|------|---------|------------|-------------------|
| **Demo base** (sin WhatsApp real) | `docker compose -f docker-compose.local.yml up -d` | Solo Docker | Desactivada por defecto |
| **Demo con WhatsApp Web** | `docker compose -f docker-compose.local.yml --profile whatsapp up -d` | + Escanear QR en /admin/whatsapp-web | Desactivada por defecto |
| **Debug auto-respuesta IA** | `APP_AI_AGENTS_AUTO_REPLY_ENABLED=true` + modo WhatsApp | + Sesión WhatsApp CONNECTED | Activar manualmente en `.env.local` |

> **Importante:** La auto-respuesta IA (`APP_AI_AGENTS_AUTO_REPLY_ENABLED`) está desactivada por defecto en entorno local.
> Para probarla, ver `docs/DEBUGGING_AUTO_REPLY_LOCAL.md`.

## Levantar en 3 Comandos

```bash
# 1. Clonar y configurar
cp .env.local.template .env.local

# 2. Iniciar servicios base (postgres + backend + frontend)
docker compose -f docker-compose.local.yml up -d

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

## Puertos y Servicios

| Servicio | Puerto Host | Interno | Perfil | Descripción |
|----------|-------------|---------|--------|-------------|
| postgres | 5433 | 5432 | — | PostgreSQL 16 |
| backend-java | 8080 | 8080 | — | Spring Boot 3 |
| frontend-react | 5173 | 5173 | — | Vite dev server |
| whatsapp-web-service | 3001 | 3001 | `whatsapp` | whatsapp-web.js + Chromium |

## Comandos Útiles

```bash
# PowerShell (recomendado)
.\scripts\dev.ps1 up              # levantar servicios base
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
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml --profile whatsapp up -d
docker compose -f docker-compose.local.yml logs -f --tail=100
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml down -v   # borrar volúmenes
docker compose -f docker-compose.local.yml ps
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
