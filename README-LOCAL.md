# Asistente WhatsApp MVP — Entorno Local

## Modos de uso local

| Modo | Comando | Requisitos | Auto-respuesta IA |
|------|---------|------------|-------------------|
| **Demo base** (sin WhatsApp real) | `docker compose -f docker-compose.local.yml up -d` | Solo Docker | Desactivada por defecto |
| **Demo con túnel público** | `docker compose -f docker-compose.local.yml --profile public-link up -d` | + Cloudflare Tunnel automático | Desactivada por defecto |
| **Demo con canal simulado** | `APP_WHATSAPP_CHANNEL_PROVIDER=SIMULATED` (default) + simular mensajes en `/admin/whatsapp-simulator` | Solo Docker | Desactivada por defecto |
| **Demo con Cloud API controlada** | `SPRING_PROFILES_ACTIVE=local,local-meta-controlled` + credenciales Meta + `APP_LOCAL_META_CONTROLLED_ACKNOWLEDGED=true` + lista permitida de teléfonos | + Token de acceso y webhook Meta | Activar manualmente en `.env.local` |

> **Importante:** La auto-respuesta IA (`APP_AI_AGENTS_AUTO_REPLY_ENABLED`) está desactivada por defecto en entorno local.
> Para probarla, simula un mensaje entrante en `/admin/whatsapp-simulator` (equivale a `POST /api/v1/test/whatsapp-inbound`).

## Modalidades de aislamiento local (Fase 3)

El ambiente local tiene **dos modalidades intencionales** separadas por perfil de Spring:

| Modalidad | Perfil (default compose) | Comportamiento |
|---|---|---|
| **Local segura (simulada)** | `local,local-safe` | Sin tráfico externo: WhatsApp `SIMULATED`, Cloud API off, OpenAI off, correo solo Mailpit (espejo Gmail off), calendario Google off, pagos `SIMULATED`. La compuerta de arranque rechaza cualquier combinación que permita salida externa y el guard de tráfico bloquea llamadas HTTP a hosts externos. |
| **Local Meta controlada** | `local,local-meta-controlled` | Integración real controlada con WhatsApp Cloud API: exige `APP_LOCAL_META_CONTROLLED_ACKNOWLEDGED=true` (doble confirmación), lista permitida de teléfonos de prueba (`APP_WHATSAPP_CLOUD_API_ALLOWED_TEST_PHONES`), credenciales completas, firma de webhook obligatoria y dry-run desactivado. Solo se procesan mensajes de números autorizados. |

El arranque con perfil `local-safe` o `local-meta-controlled` valida la configuración y **falla con mensaje claro** si algún valor habilita tráfico no autorizado (p. ej. auto-reply activo, Cloud API habilitado, espejo Gmail activo o proveedor de pago externo en modo seguro). Los valores se corrigen en `docker-compose.local.yml` o `.env.local`; nunca se imprime el valor de secretos, solo el nombre de la propiedad.

> **Nota:** el perfil `local-whatsapp-cloud` (legacy) sigue disponible para compatibilidad, pero no tiene doble confirmación ni lista permitida. Se recomienda migrar a `local-meta-controlled`.

## Gestión de Secretos (Windows Credential Manager)

Los secretos reales (JWT, WhatsApp App Secret, Access Token, Gmail App Password y OpenAI API Key) **no están en ningún archivo**.
Se almacenan cifrados con DPAPI en Windows Credential Manager.
Sentry es opcional: si existe `SENTRY_DSN` en Credential Manager, `restore-local-secrets.ps1` lo restaura y activa `SENTRY_ENABLED=true`; si no existe, queda desactivado sin romper el arranque.
El email local usa doble entrega: Mailpit captura todos los correos en `http://localhost:8025` y, si `APP_EMAIL_MIRROR_ENABLED=true` (desactivado por defecto desde Fase 3; se activa solo para la integración controlada), también se envían por Gmail usando el password guardado como `GMAIL_PASSWORD`.

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

## Con Canal Simulado

El canal de WhatsApp en local usa el proveedor `SIMULATED` embebido en el backend (sin servicio externo, sin QR):

```bash
# Simular un mensaje entrante (equivalente a la UI /admin/whatsapp-simulator)
curl -X POST http://localhost:8080/api/v1/test/whatsapp-inbound \
  -H "Content-Type: application/json" \
  -d '{"sessionKey":"demo","from":"+56950954580","body":"Hola, quiero agendar una hora"}'
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
| backend-java | 8080 | 8080 | `local,local-safe` (+`observability`) | Spring Boot 3 |
| frontend-react | 5173 | 5173 | — | Vite dev server |
| public-tunnel | — | — | `public-link` | Cloudflare Tunnel (trycloudflare.com) |
| prometheus | 9090 | 9090 | `observability` | Métricas (scrape backend) |
| loki | 3100 | 3100 | `observability` | Logs |
| tempo | 3200 | 3200 | `observability` | Trazas |
| alloy | — | 12345 | `observability` | Agente de recolección |
| grafana | 3000 | 3000 | `observability` | Dashboards y alertas |

## Comandos Útiles

```powershell
# PowerShell (recomendado)
.\scripts\store-local-secrets.ps1  # guardar/actualizar secretos en Credential Manager
.\scripts\local-start.ps1           # levantar servicios base (restaura secretos)
.\scripts\local-start.ps1 -Build    # reconstruir imágenes y levantar
# Nota: local-start.ps1 restaura automáticamente los secretos desde Credential Manager

.\scripts\dev.ps1 up              # levantar servicios base (sin restaurar secretos)
.\scripts\dev.ps1 logs            # seguir logs
.\scripts\dev.ps1 down            # detener (preserva volúmenes)
.\scripts\dev.ps1 reset           # borrar volúmenes + rebuild + up
.\scripts\dev.ps1 ps              # estado de servicios
.\scripts\dev.ps1 build           # reconstruir imágenes

# npm (desde frontend-react/)
cd frontend-react
pnpm run docker:up                # levantar servicios base
pnpm run docker:logs              # seguir logs
pnpm run docker:down              # detener
pnpm run docker:reset             # borrar volúmenes + rebuild + up
pnpm run docker:ps                # estado de servicios

# Docker Compose directo (desde la raíz)
# NOTA: si usas docker compose directo, primero restaura secretos manualmente:
#   .\scripts\restore-local-secrets.ps1
# (setea las variables de entorno que docker compose heredará)
docker compose --env-file .env.local -f docker-compose.local.yml up -d
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

## Observabilidad Local

Stack de Prometheus + Loki + Tempo + Alloy + Grafana para métricas funcionales, logs JSON, trazas y errores de frontend. Ver `OBSERVABILIDAD_LOCAL.md` para la guía completa.

```powershell
# Requisito: GRAFANA_ADMIN_PASSWORD en .env.local
Add-Content .env.local "GRAFANA_ADMIN_PASSWORD=cambia-esta-password"

# Levantar (backend + frontend + observabilidad)
.\scripts\observability-start.ps1

# Verificar contenedores, endpoints, trazas y dashboards
.\scripts\observability-verify.ps1

# Detener (preserva datos) / reset (borra datos)
.\scripts\observability-stop.ps1
.\scripts\observability-reset.ps1
```

Grafana: http://localhost:3000 (admin / `GRAFANA_ADMIN_PASSWORD`).

## Documentación Relacionada

- `DOCUMENTATION_INDEX.md` — Índice documental por categoría y vigencia
- `LOCAL_MATURITY_REPORT.md` — Informe de madurez local vigente
- `docs/AMBIENTES_WHATSAPP.md` — Proveedores de canal (SIMULATED / Cloud API)
- `CHECKLIST_DEMO_LOCAL.md` — Checklist para demo funcional (histórico)
- `DEMO_GUIDE.md` — Guía de demo
- `OBSERVABILIDAD_LOCAL.md` — Stack de observabilidad local (métricas, logs, trazas, dashboards, alertas)

La validación de la documentación (enlaces y comandos) se ejecuta con `.\scripts\validate-docs.ps1`.

## Scripts del Túnel Público

| Script | Función |
|--------|---------|
| `scripts/start-public-link.ps1` | Inicia túnel, detecta URL, actualiza `.env.local` |
| `scripts/stop-public-link.ps1` | Detiene solo el túnel (servicios locales continúan) |
| `scripts/check-public-link.ps1` | Verifica estado y validez del túnel |
