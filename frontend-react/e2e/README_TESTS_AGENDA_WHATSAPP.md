# Suite de Pruebas E2E — Asistente de Negocios WhatsApp

## Resumen

Suite exhaustiva de 19 spec files con 82+ tests que cubren los 137 casos de prueba del Excel `agenda_digital_whatsapp_casuisticas.xlsx`, distribuidos en 10 niveles funcionales.

## Niveles cubiertos

| Nivel | Modulo | Spec | Tests |
|-------|--------|------|-------|
| 1 | Smoke | `01-smoke.spec.ts` | 7 |
| 2 | Auth | `02-auth.spec.ts` | 9 |
| 3 | Agenda básica | `03-agenda-basica.spec.ts` | 10 |
| 11 | Visual | `04-agenda-visual.spec.ts` | 10 |
| 7 | WhatsApp reserva | `05-whatsapp-reserva-simulada.spec.ts` | 7 |
| 9 | WhatsApp cancelación | `06-whatsapp-cancelacion-simulada.spec.ts` | 6 |
| 10 | WhatsApp reprogramación | `07-whatsapp-reprogramacion-simulada.spec.ts` | 8 |
| 8 | Confirmación pública | `08-confirmacion-publica.spec.ts` | 7 |
| 14 | Seguridad | `12-seguridad.spec.ts` | 10 |
| 7 | Reprogramación interfaz | `13-reprogramacion-interfaz.spec.ts` | 7 |
| 8 | Cancelación interfaz | `14-cancelacion-interfaz.spec.ts` | 6 |
| 13 | Sucursales | `15-sucursales.spec.ts` | 6 |
| 14 | Profesional | `16-profesional.spec.ts` | 7 |
| 15 | Base de datos | `17-base-datos.spec.ts` | 5 |
| 16 | Concurrencia | `18-concurrencia.spec.ts` | 2 |
| 17 | Regresión | `19-regresion-completa.spec.ts` | 3 |

## Estado actual

- **Passed**: ~51-52 tests
- **Failed**: 1 pre-existing (QA-14-010: stack trace exposure — Vite source map references)
- **Skipped/Blocked**: ~30 tests (requieren endpoint de simulación WhatsApp, DB de test, o seed data adicional)

## Prerrequisitos

1. Node.js 20.19+ y pnpm instalados globalmente
2. Navegador Chromium instalado (`npx playwright install chromium`)
3. Frontend corriendo (`pnpm dev` en `frontend-react/`)
4. Backend accesible en `http://localhost:8080`

## Ejecución

```bash
# Todo el suite (proyecto all-chromium)
cd frontend-react
pnpm exec playwright test --project=all-chromium

# Spec específico
pnpm exec playwright test --project=agenda-basica
pnpm exec playwright test e2e/03-agenda-basica.spec.ts

# Cross-browser (smoke + confirmación + seguridad)
pnpm exec playwright test --project=smoke --project=smoke-firefox --project=smoke-webkit
pnpm exec playwright test --project=confirmacion-publica --project=confirmacion-publica-firefox --project=confirmacion-publica-webkit

# Helper scripts
.\e2e\scripts\run-all.ps1          # Windows
bash e2e/scripts/run-all.sh        # Linux/Mac
.\e2e\scripts\run-all.ps1 -Smoke  # Solo smoke
.\e2e\scripts\run-all.ps1 -All    # Todos los proyectos
```

> **Nota**: Si el contenedor Docker `asistente-whatsapp-frontend` está corriendo en puerto 5173, Playwright lo reutilizará. Para usar el dev server local, detenga el contenedor: `docker stop asistente-whatsapp-frontend`.

## Reportes

- **HTML**: `e2e/reports/html-report/index.html`
- **JSON**: `e2e/reports/test-results.json`
- **MD**: `e2e/reports/test-status-report.md`
- **XML (JUnit)**: `e2e/reports/test-results.xml`

## Documentación relacionada

- `ANALISIS_REPOSITORIO_PRUEBAS.md` — Análisis completo del repositorio
- `MATRIZ_PRUEBAS_ASISTENTE_NEGOCIOS.md` — Matriz Excel → specs
- `agenda_digital_whatsapp_casuisticas.xlsx` — Fuente de verdad (137 casos)

## Casos bloqueados

- **WhatsApp simulado** (TST-001 a TST-056, 084-137): No existe endpoint `POST /api/v1/test/whatsapp-inbound`. Sin simulación, los 28 tests de flujo conversacional quedan BLOCKED.
- **Base de datos directa** (POST-RES-001/003/004): No hay conexión segura a PostgreSQL de pruebas.
- **Concurrencia E2E** (TST-034): Requiere autenticación real + requests paralelos.
- **Multi-sucursal 3-8** (TST-MUL-003 a 008): Demo data solo tiene 2 sucursales.
