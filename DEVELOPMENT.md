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

```bash
# Stack completo (backend + frontend + PostgreSQL)
docker compose up -d

# Con backup sidecar
docker compose --profile backup up -d

# Con observabilidad local (Prometheus, Loki, Tempo, Alloy, Grafana)
docker compose --env-file .env.local -f docker-compose.local.yml --profile observability up -d
```

El perfil `observability` de Docker Compose activa además el perfil Spring `observability`
del backend (logs JSON, trazas OTLP hacia Tempo, health checks extendidos y métricas
funcionales `assistente_*`). Ver `OBSERVABILIDAD_LOCAL.md`.

## Scripts auxiliares

- `scripts/run-all.ps1` — PowerShell: build + backend + frontend + tests
- `scripts/run-all.sh` — Bash (Linux/Mac): build + backend + frontend + tests

## Convenciones

- **Commits**: convencional (`feat:`, `fix:`, `chore:`, etc.)
- **Ramas**: `feature/*`, `fix/*` — merge a `develop`
- **Estilo**: Prettier (frontend), formato automático en commits
- **BD**: Flyway migrations en `backend-java/src/main/resources/db/migration/`
