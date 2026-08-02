# Changelog

## 0.4.0 (2026-08-02) — Fase 2: línea base Git y documentación

### Agregado
- Índice documental `DOCUMENTATION_INDEX.md` con categorías: canónico, contratos/diseño, operativo, histórico, registro de cambios.
- Script `scripts/validate-docs.ps1`: validación automática de referencias a scripts, migraciones Flyway, enlaces relativos y puertos locales en la documentación (108 archivos .md revisados, 0 errores).
- `LOCAL_MATURITY_CONTEXT.md` y sección "Contexto de evaluación del ambiente local" en `AGENTS.md`.

### Corregido
- **Seguridad**: `encrypt-token.ps1` y `update-token.ps1` (raíz) contenían un token real de Cloud API en texto plano → eliminados; verificado con `git log -S` que nunca estuvo en el historial. Se recomienda rotar el token.
- `.gitignore`: salen del control de versiones `registro_ejecucion_IA.json`, `MEMORY.md`, `database/manual/`, `docs/observabilidad-capturas/`, `frontend-react/e2e/reports/`.
- `README.md`: eliminadas referencias a WhatsApp Web visual (`vnc.html`), canal experimental y migraciones de pagos desactualizadas; enlaces corregidos.
- `CHECKLIST_DEMO_LOCAL.md`: marcado HISTÓRICO; eliminadas referencias a `verify_mvp_local.ps1` inexistente y conteo de migraciones actualizado (105).
- Documentos marcados HISTÓRICO: `RESULTADOS_QA_IA_RESERVAS.md`, `docs/CORRECCION_FK_INTENT_LOG.md`, `docs/RESPUESTA_IA_REGLAS_WHATSAPP_WEB.md`, `docs/ia-negocio/revision_tecnica_prompts_reglas_v22.md`, `docs/PROMPT_CORRECCION_MVP_ORQUESTADOR_V23_4_10.md`, `docs/INFORME_ELIMINACION_WHATSAPP_WEB.md`, `docs/CENTRO_ESTETICO_MODULO.md`, `docs/REPARACION_DNS_DOCKER_WINDOWS.md`, `docs/ENLACE_NAVEGABLE_WHATSAPP_V23_4_11.md`, `frontend-react/e2e/ANALISIS_REPOSITORIO_PRUEBAS.md`.
- Referencias corregidas a scripts vigentes: `start-public-link.ps1`/`stop-public-link.ps1`/`check-public-link.ps1` (en vez de `start_mvp_public_link.*`), `local-package.ps1` (en vez de `package_demo_clean.ps1`), `local-verify.ps1` (en vez de `verify_mvp_local.ps1`).
- `docs/API_CONTRACTS.md`: webhook de integración actualizado a `POST /api/v1/integrations/whatsapp-cloud/webhook`.
- Contratos visuales y UI (`docs/UI_COMPONENTS.md`, `docs/visual-contract/*`): "WhatsApp Web" → "Canal WhatsApp".

### Documentado
- `LOCAL_MATURITY_REPORT.md` actualizado al estado vigente: rama `master`, HEAD `e9a321b`, 105 migraciones, 11 servicios compose (4 core / 5 observability / 1 public-link / 1 https), 180/180 tests frontend, observabilidad 6/6 OK.
- Propuesta de secuencia de confirmaciones convencionales para convertir el árbol (91 cambios restantes) en línea base revisable — no ejecutada, pendiente de aprobación.

## 0.3.0 (2026-08-02)

### Agregado
- Instrumentación de observabilidad del backend: `BusinessMetrics` con el contrato completo de métricas funcionales (`assistente_*`), health checks extendidos (`outbox`, `tareasProgramadas`, `iaProveedor`, `whatsApp`, `flyway`), spans OTLP para tareas programadas y propagación de `traceparent`/`X-Correlation-Id` en llamadas salientes (incluye OpenAI vía el builder compartido).
- Perfil Spring `observability`: `application-observability.yml` (tracing OTLP hacia Tempo con sampling 1.0, percentiles-histogram, tags `application=backend-java`) y `logback-spring.xml` (logs JSON Logstash con MDC `correlationId`/`traceId`/`spanId` bajo ese perfil).
- Endpoint público `POST /api/v1/observability/client-errors` (validación de origen, rate-limit 20/min por cliente, truncamientos, logger `APP_CLIENT_ERROR`).
- Frontend: `GlobalErrorBoundary` + `clientErrorReporter` que reportan errores de render, `window.onerror` y `unhandledrejection` al backend.
- 6 dashboards de Grafana provisionados en `monitoring/grafana/dashboards/` (Resumen General, Agenda y Reservas, WhatsApp Cloud API, Inteligencia Artificial, Registros y Trazas, Infraestructura).
- Scripts `observability-start/stop/verify/reset.{ps1,sh}` y guía `OBSERVABILIDAD_LOCAL.md`.
- Tests de observabilidad: 33 nuevos en backend (BusinessMetrics, health indicators, client-errors, LogSanitizer) y 7 en frontend (reporter + boundary). Suite backend 638 tests (38F/11E preexistentes de IA/agenda, sin cambios — verificado contra árbol limpio); frontend 180 tests OK.

### Corregido
- `docker-compose.local.yml`: clave `SPRING_PROFILES_ACTIVE` duplicada que rompía el YAML (sobrevive la que añade `observability`); healthchecks de Tempo y Alloy usaban `wget`/shell inexistente en sus imágenes (Tempo distroless → `CMD /busybox/wget`; Alloy → `bash -ec 'exec 3<>/dev/tcp/...'`).
- `monitoring/alloy/config.alloy`: filtro `discovery.docker` con `name = ["name"]` (array) → `name = "name"`.
- `monitoring/prometheus/alerts.yml`: expresión `PrometheusRecolectandoVacio` inválida (`==` entre escalares) → `absent(up{job="backend-java"})`.
- `scripts/observability-verify.ps1`: auth de Grafana hardcodeada `admin:admin` → lee `GRAFANA_ADMIN_PASSWORD` de `.env.local`; filtro de dashboards por uid `^asistente-`.

### Verificado (E2E)
- Stack `observability` completo healthy (prometheus, loki, tempo, alloy, grafana); `/actuator/health` UP con 11 componentes (incluye outbox, tareasProgramadas, iaProveedor, whatsApp, flyway).
- Mensajes simulados incrementan `assistente_whatsapp_mensajes_recibidos_total`/`webhooks_recibidos_total`; trazas y logs JSON correlacionados (traceId común Loki↔Tempo); `client-errors` responde 403 sin `Origin` y 202 con `Origin` válido.
- Capturas de los 6 dashboards en `docs/observabilidad-capturas/`.

## 0.2.1 (2026-08-01)

### Agregado
- Paso 4 "Fecha y hora" de la reserva pública rediseñado: selección obligatoria de tramo
  Mañana/Tarde (corte 12:00 centralizado en `SlotTimePeriod.NOON`), horarios filtrados por
  tramo y ordenados ascendentemente, conteos por tramo, tarjetas con jerarquía visual,
  estados de carga/error/vacío y botón "Actualizar" con reconciliación de selección.
- `PublicLandingService.normalizeAndSortSlots` con dedup por identidad completa
  (inicio/fin/profesional/cabina/sucursal) que corrige el descarte de horarios legítimos
  simultáneos de distintos profesionales (antes se deduplicaba solo por inicio/fin).
- Util `slotTimePeriod.ts` (clasificación por hora de pared del ISO, sin conversión de zona),
  accesibilidad del selector de fecha por teclado y `aria-*` en tramos y tarjetas.
- Tests: `SlotTimePeriodTest` (7), `PublicLandingServiceAvailabilityTest` (11),
  `CreatePublicBookingPage.test.tsx` (12) y e2e `reservar-fecha-hora.spec.ts` (desktop + móvil).
  Suite backend 629 tests (38F/11E preexistentes de IA/agenda, sin cambios); frontend 163 tests OK.
- Informe completo: `RESULTADO_MEJORA_PASO_FECHA_HORA_RESERVA_PUBLICA.md`.

### Corregido
- Dedup de horarios en `PublicLandingService.availability` (ver arriba).
- Selector de fecha no operativo por teclado (div sin rol ni manejador Enter/Espacio).

## 0.2.0 (2026-08-01)

### Eliminado
- `whatsapp-web-service` (servicio Node/Express con `whatsapp-web.js`, Puppeteer/Chromium, QR y puerto 3001): eliminado de repositorio, composes, scripts, UI, docs y configuración. El canal de WhatsApp es nativo del backend con proveedores `META_CLOUD_API` (WhatsApp Cloud API de Meta, webhook firmado `X-Hub-Signature-256`) y `SIMULATED` (embebido, default local). Ver informe completo en `docs/INFORME_ELIMINACION_WHATSAPP_WEB.md`.

### Corregido
- Referencias residuales a `whatsapp-web-service` en documentación de raíz y `docs/` actualizadas a la realidad actual.

## 0.1.0 (2026-07-15)

### Agregado
- Perfil `local` de Spring Boot (`application-local.yml`) para desarrollo desde IDE sin dependencia de Docker ni seed data automática.
- Perfiles Maven (`unit`, `integration`, `local`) con separación Surefire/Failsafe.
- MSW (Mock Service Worker) en frontend: `src/test/mocks/server.ts`, `handlers.ts`, `data/`.
- Pruebas de perfil de usuario (`ProfilePage.test.tsx`) — 7 tests (carga, error, formulario, botones, navegación). 48 → 55 tests frontend.
- Prettier: `.prettierrc`, `.prettierignore`, scripts `format` y `format:check`.
- Task runner central: `scripts/run-all.ps1` y `scripts/run-all.sh`.
- Backup sidecar en docker-compose (perfil `backup`, cron configurable, rotación 7 días).
- `CHANGELOG.md`.
- `DEVELOPMENT.md`, `CONTRIBUTING.md`.

### Corregido
- Tests de `BookingConfirmationPage`: fechas dinámicas para evitar bloqueo por ventana de cancelación cerrada (`changeWindowClosed`).
- Test de `NotificationsPage`: `'READ'` → `'Leída'` (texto en español).
- Eliminados 8 `console.log` de depuración en `CompleteAgendaPage.tsx`, `CreatePublicBookingPage.tsx`, `NewAppointmentPage.tsx`.
- CI/CD: workflow frontend actualizado a pnpm con format:check, lint, test, build.

### Seguridad
- Ningún `.env` con secretos trackeado por Git (solo `.env.example`).
- No se agregaron secretos ni credenciales reales.

### Brechas conocidas diferidas
- `whatsapp-web-service` (eliminado en 0.2.0, 2026-08-01): cobertura de pruebas en ese momento, 1 test (8 líneas). No se modificó por decisión del propietario.
- `start-visual.sh` faltante en Dockerfile: referenciado pero no existe. No se crea ni elimina por decisión del propietario.
- `format:check` reporta 156 archivos con estilo preexistente (no reformateados para evitar cambios masivos).

### Versiones
- Backend: `0.0.1-SNAPSHOT`
- Frontend: `0.1.0`
- WhatsApp Web Service: `0.4.0` (eliminado en 0.2.0, 2026-08-01)
