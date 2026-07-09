# Comandos de Ejecucion — Pruebas E2E Asistente de Negocios

## Requisitos previos

- Node.js >= 18
- pnpm >= 10
- Docker Desktop (para levantar el stack completo)
- Navegador Chromium instalado por Playwright

### Instalar dependencias

```bash
cd frontend-react
pnpm install
pnpm exec playwright install chromium
```

## Levantar el ambiente de pruebas

### Con Docker (recomendado)

```bash
# Desde la raiz del proyecto (asistente/)
docker compose -f docker-compose.local.yml up -d --build

# Verificar que todos los servicios esten arriba
docker compose -f docker-compose.local.yml ps

# Ver logs si es necesario
docker compose -f docker-compose.local.yml logs --tail=100 backend-java
docker compose -f docker-compose.local.yml logs --tail=100 frontend-react
```

### Solo frontend (si backend ya esta corriendo)

```bash
cd frontend-react
pnpm dev
```

## Ejecutar pruebas E2E

### Todas las pruebas

```bash
cd frontend-react
pnpm test:e2e
```

### Pruebas con navegador visible

```bash
pnpm test:e2e:headed
```

### Pruebas en modo debug

```bash
pnpm test:e2e:debug
```

### Pruebas con UI interactiva

```bash
pnpm test:e2e:ui
```

### Por proyecto (spec individual)

```bash
# Smoke tests
pnpm exec playwright test --project=smoke

# Auth tests
pnpm exec playwright test --project=auth

# Agenda basica
pnpm exec playwright test --project=agenda-basica

# Agenda visual
pnpm exec playwright test --project=agenda-visual

# Confirmacion publica
pnpm exec playwright test --project=confirmacion-publica

# WhatsApp simulado (requiere endpoint)
pnpm exec playwright test --project=whatsapp-reserva
pnpm exec playwright test --project=whatsapp-cancelacion
pnpm exec playwright test --project=whatsapp-reprogramacion
```

### Por tags

```bash
# Solo pruebas de WhatsApp simulado
pnpm exec playwright test --grep @wpp-sim

# Solo pruebas de cancelacion
pnpm exec playwright test --grep @wpp-cancel

# Solo pruebas de reprogramacion
pnpm exec playwright test --grep @wpp-reschedule
```

## Reportes

### Ver reporte HTML

```bash
pnpm test:e2e:report
```

### Generar reporte de estado Markdown/JSON

```bash
# Despues de ejecutar las pruebas, generar reporte consolidado
pnpm test:e2e:generate-report
```

Los reportes se generan en:
- `e2e/reports/test-status-report.json`
- `e2e/reports/test-status-report.md`
- `e2e/reports/html-report/` (Playwright HTML)

## Estructura de archivos

```
frontend-react/
├── e2e/
│   ├── 01-smoke.spec.ts                    # Smoke tests basicos
│   ├── 02-auth.spec.ts                     # Login/logout funcional
│   ├── 03-agenda-basica.spec.ts            # Visualizacion calendario
│   ├── 04-agenda-visual.spec.ts            # Pruebas visuales/solapamiento
│   ├── 05-whatsapp-reserva-simulada.spec.ts # Flujo reserva WhatsApp
│   ├── 06-whatsapp-cancelacion-simulada.spec.ts # Flujo cancelacion
│   ├── 07-whatsapp-reprogramacion-simulada.spec.ts # Flujo reprogramacion
│   ├── 08-confirmacion-publica.spec.ts     # Confirmacion publica
│   ├── fixtures.ts                         # Fixtures reutilizables
│   ├── helpers/
│   │   ├── auth.helper.ts                  # Login/logout/session mock
│   │   ├── api.helper.ts                   # Llamadas API
│   │   ├── agenda.helper.ts                # Interacciones agenda
│   │   ├── whatsapp-simulator.helper.ts    # Simulacion WhatsApp
│   │   ├── db.helper.ts                    # Helper DB (via API)
│   │   └── report.helper.ts               # Generacion de reportes
│   ├── setup/
│   │   ├── globalSetup.ts                  # Setup global
│   │   └── globalTeardown.ts               # Teardown global
│   ├── scripts/
│   │   └── generate-report.ts              # Generar reporte manual
│   ├── screenshots/                        # Evidencias visuales
│   ├── traces/                             # Playwright traces
│   └── reports/                            # Reportes generados
├── docs/
│   ├── MATRIZ_PRUEBAS_ASISTENTE_NEGOCIOS.md # Matriz completa
│   └── COMANDOS_EJECUCION.md               # Este archivo
└── playwright.config.ts                    # Configuracion Playwright
```
