# Development

## Prerrequisitos

- **Backend**: Java 21+, Maven 3.9+
- **Frontend**: Node.js 20+, pnpm 10+
- **Docker**: Docker Desktop (para PostgreSQL en perfil `local`)
- **IDE recomendado**: IntelliJ IDEA Ultimate

## Perfiles Maven

| Perfil | Comando | Descripción |
|--------|---------|-------------|
| `unit` | `mvn test -P unit` | Solo pruebas unitarias (sin Testcontainers) |
| `integration` | `mvn verify -P integration` | Pruebas de integración con Testcontainers |
| `local` | `mvn spring-boot:run -P local` | Ejecución local sin Docker |

## Backend

```bash
cd backend-java

# Pruebas unitarias
.\mvnw.cmd test -P unit

# Pruebas completas (unitarias + integración)
.\mvnw.cmd verify

# Ejecutar con perfil local (h2 embebido, sin seed data)
.\mvnw.cmd spring-boot:run -P local
```

## Frontend

```bash
cd frontend-react

# Instalar dependencias
pnpm install

# Desarrollo (hot-reload en :5173)
pnpm dev

# Pruebas
pnpm test -- --run

# Lint
pnpm lint

# Formato
pnpm format:check

# Build producción
pnpm build
```

## Docker Compose

El archivo canónico del entorno local es `docker-compose.local.yml` (usa `.env.local`
con `--env-file`). El flujo oficial son los scripts `scripts/local-*.ps1`, que restauran
secretos desde Windows Credential Manager, validan el compose y devuelven códigos de
error ante fallos:

```bash
# Levantar (restaura secretos + valida config antes de `up`)
.\scripts\local-start.ps1
.\scripts\local-start.ps1 -Build              # reconstruir imágenes
.\scripts\local-start.ps1 -Profile observability,backup
.\scripts\local-start.ps1 -Profile all -Verify # todos los perfiles + healthcheck

# Detener (incluye todos los perfiles opcionales; -Volumes borra la BD)
.\scripts\local-stop.ps1

# Verificar health + smoke test
.\scripts\local-verify.ps1

# Reset completo (limpia artefactos, reconstruye y levanta)
.\scripts\local-reset.ps1
```

Comandos docker compose directos equivalentes (requieren restaurar secretos antes):

```bash
# Stack base (postgres + backend + frontend + mailpit)
docker compose --env-file .env.local -f docker-compose.local.yml up -d

# Con backup sidecar (cron diario de pg_dump)
docker compose --env-file .env.local -f docker-compose.local.yml --profile backup up -d

# Con observabilidad local (Prometheus, Loki, Tempo, Alloy, Grafana)
docker compose --env-file .env.local -f docker-compose.local.yml --profile observability up -d

# Con túnel público HTTPS (cloudflared quick tunnel)
docker compose --env-file .env.local -f docker-compose.local.yml --profile public-link up -d

# Con HTTPS local autosigned (Caddy)
docker compose --env-file .env.local -f docker-compose.local.yml --profile https up -d
```

El perfil `observability` de Docker Compose activa además el perfil Spring `observability`
del backend (logs JSON, trazas OTLP hacia Tempo, health checks extendidos y métricas
funcionales `assistente_*`). Ver `OBSERVABILIDAD_LOCAL.md`.

### Stack alternativo (`docker-compose.full.yml`)

El compose base del bootstrap se renombró a `docker-compose.full.yml` (Fase 4): es un
stack completo alternativo (PostgreSQL + backend + frontend + backup-sidecar) con
contenedores `asistente-full-*`, volúmenes propios y red propia. **No se levanta
simultáneamente con `docker-compose.local.yml`** (comparten puertos 5433/8080/5173).
Leer el encabezado del archivo antes de usarlo.

## Scripts auxiliares

- `scripts/run-all.ps1` — PowerShell: build + backend + frontend + tests
- `scripts/run-all.sh` — Bash (Linux/Mac): build + backend + frontend + tests

## Convenciones

- **Commits**: convencional (`feat:`, `fix:`, `chore:`, etc.)
- **Ramas**: `feature/*`, `fix/*` — merge a `develop`
- **Estilo**: Prettier (frontend), formato automático en commits
- **BD**: Flyway migrations en `backend-java/src/main/resources/db/migration/`
