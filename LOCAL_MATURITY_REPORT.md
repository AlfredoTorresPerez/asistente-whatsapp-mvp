# Reporte de Madurez Local — Asistente WhatsApp MVP

**Fecha:** 2026-08-02
**Branch:** `master`
**Commit:** `d85923b` (línea base Fase 2) + cambios de Fase 3 y Fase 4 pendientes de commit
**Contexto de evaluación:** `LOCAL_MATURITY_CONTEXT.md` (obligatorio para evaluadores)

---

## 1. Modalidades del ambiente local

| Modalidad | Perfil (default) | Canal WhatsApp | Controles |
|---|---|---|---|
| **Local simulada segura** | `local,local-safe` (default compose) | `SIMULATED` (embebido en el backend, default) | Compuerta de arranque + guard de tráfico: sin Cloud API, OpenAI, espejo Gmail ni calendario Google; pagos `SIMULATED`; correo solo Mailpit |
| **Local de integración real controlada** | `local,local-meta-controlled` | `META_CLOUD_API` (webhook firmado `X-Hub-Signature-256`) | Doble confirmación (`APP_LOCAL_META_CONTROLLED_ACKNOWLEDGED=true`), lista permitida de teléfonos, credenciales completas, firma obligatoria, dry-run off |

La activación de WhatsApp Cloud API en local es una decisión intencional y necesaria: solo intervienen números autorizados (número empresarial prepago dedicado a pruebas + número personal autorizado como cliente de prueba), no hay clientes reales, Meta exige activación explícita, y el proveedor `SIMULATED` permanece disponible.

## 2. Estado por dimensión

### 2.1 Aislamiento local — ✅ FUERTE
- `docker-compose.local.yml`: 12 servicios declarados en 5 perfiles — core: `postgres`, `backend-java`, `frontend-react`, `mailpit`; `observability`: `prometheus`, `loki`, `tempo`, `alloy`, `grafana`; `backup`: `backup-sidecar` (Fase 4); `public-link`: `public-tunnel`; `https`: `caddy`.
- 10 contenedores en ejecución, todos healthy (verificado 2026-08-02).
- **Fase 3:** default de arranque = `local,local-safe`; auto-reply `false`, safe-mode `true`, espejo de correo `false` y OpenAI `false` por defecto (corregidos en compose y `.env.local.template`).
- **Fase 3:** `EgressTrafficGuard` (interceptor del `RestClient.Builder` compartido) bloquea toda llamada HTTP saliente a hosts externos en modo seguro; permite solo localhost y hosts del stack de contenedores.
- Sin credenciales en repositorio; `.env.local` no versionado (patrón `.env.*` en `.gitignore` con excepciones de plantillas).
- Datos exclusivamente de prueba (seeds demo multisucursal).
- No hay comunicación con producción desde el ambiente local.

### 2.2 Seguridad de la integración — ✅ FUERTE
- **Fase 3:** compuerta de arranque `LocalEnvironmentGate` valida la configuración efectiva (patrón `EmailConfigValidator`); impide iniciar en combinaciones que habiliten tráfico externo no autorizado, listando solo nombres de propiedades, nunca valores.
- **Fase 3:** doble confirmación explícita para la fase Meta controlada (`app.local-meta-controlled.acknowledged`) y lista permitida de teléfonos de prueba con descarte de mensajes no autorizados en el webhook.
- Activación explícita del perfil Meta (`SPRING_PROFILES_ACTIVE=local,local-meta-controlled` + variables de Cloud API solo en `.env.local`).
- Número empresarial prepago dedicado a pruebas; lista permitida de clientes de prueba.
- Validación de firma `X-Hub-Signature-256` (HMAC-SHA256) en el webhook.
- Prevención de duplicados por `external_message_id` único (V57) e idempotencia de operaciones de reserva (V86).
- Registros sanitizados (LogSanitizer) y trazabilidad de mensajes.
- **Hallazgo de seguridad de la línea base (Fase 2):** `encrypt-token.ps1` y `update-token.ps1` en la raíz contenían un token real de Cloud API en texto plano → **eliminados** (2026-08-02); verificado con `git log -S` que nunca estuvo en el historial. Recomendación: rotar el token.
- **Nunca** se incluyen números telefónicos completos, tokens ni secretos en informes o documentación.

### 2.3 Madurez de la integración real — ✅ ALTA (controlada)
- Verificación E2E conversacional completa (2026-08-01): 3 mensajes por `POST /api/v1/test/whatsapp-inbound` con JWT de `admin@demo.cl` → estado `ACCEPTED`; métricas `assistente_whatsapp_mensajes_recibidos_total` y `assistente_whatsapp_webhooks_recibidos_total` 0 → 3.
- Flujos probados: agenda, confirmación, reprogramación, cancelación y consultas comerciales.
- Proveedor simulado disponible en todo momento; respuestas pueden detenerse inmediatamente.

### 2.4 Madurez conversacional — ✅ ALTA
- Canal nativo del backend (sin servicio externo; `whatsapp-web-service` eliminado 2026-08-01).
- Cola persistente outbox (`ai_reply_outbox`, V28) con reintentos y `max_attempts`.
- Capa semántica IA: catálogos de intenciones/expresiones (V96-V102), entidades canónicas con resolución por alias, análisis por mensaje.
- Configuración de negocio editable en UI (Business AI: settings, prompts versionados, métricas, panel de prueba).

### 2.5 Reproducibilidad — ✅ ALTA
- `README-LOCAL.md` (canónico), `DEVELOPMENT.md`, `CHANGELOG.md`.
- **Fase 4:** un solo flujo oficial de arranque con `docker-compose.local.yml` como fuente canónica: `local-start.ps1` (pre-flight: docker, archivo compose, perfiles válidos, `config --quiet`) / `local-stop.ps1` / `local-verify.ps1` / `local-reset.ps1`; todos devuelven código de error ante fallo y detienen/verifican también los perfiles opcionales.
- **Fase 4:** perfiles opcionales integrados — `observability`, `backup` (nuevo: backup-sidecar pg_dump cron diario), `public-link`, `https`; matriz de arranque `docker compose config --quiet` 9/9 combinaciones OK (Compose v5.1.3).
- **Fase 4:** compose base renombrado a `docker-compose.full.yml` con contenedores `asistente-full-*`, volúmenes y red propios (cero colisiones de nombres/puertos/volúmenes con el stack local); NO simultáneo con el local (puertos compartidos).
- Scripts de ciclo de vida: `local-setup/start/stop/reset/verify/package/clean` + `observability-verify.ps1` (6/6 OK, 530 spans en Tempo) + `grafana-captures.mjs`.
- 105 migraciones Flyway versionadas (`backend-java/src/main/resources/db/migration/V1..V105`), 0 repetibles.
- Backend: `mvn spotless:apply` previo obligatorio antes de compilar.

### 2.6 Protección de secretos — ✅ FUERTE (con hallazgo corregido)
- `.env.local` con `GRAFANA_ADMIN_PASSWORD` y credenciales Meta no versionado.
- `database/manual/`, `docs/observabilidad-capturas/`, `registro_ejecucion_IA.json`, `frontend-react/e2e/reports/` y `MEMORY.md` añadidos a `.gitignore` (2026-08-02).
- Escaneo `git log -S "EAAV..."`: sin token en historial.

## 3. Evidencia de ejecución

- **Frontend:** 19 archivos de prueba, 180/180 OK (Vitest).
- **Backend:** suite completa 697 tests / 38 fallos / 11 errores, todos pre-existentes en 7 clases de IA/agenda (línea base Fase 2: 638 tests / 38F / 11E; sin fallos nuevos). Fase 3 aporta 28 tests nuevos (política de entorno, guard de tráfico, allowlist del webhook) — 68/68 OK.
- **E2E Playwright:** `evidencia-reprogramacion.spec.ts` y `reservar-fecha-hora.spec.ts` (evidencia en `frontend-react/e2e/reports/`).
- **Observabilidad:** `scripts/observability-verify.ps1` → 6/6 OK; 530 spans en Tempo; 6 capturas Grafana en `docs/observabilidad-capturas/` (no versionadas); dashboards `asistente-*` (resumen-general, agenda-reservas, inteligencia-artificial, whatsapp-cloud-api, infraestructura, registros-trazas).
- **Conteos:** 105 migraciones versionadas; 12 servicios compose local (4 core / 5 observability / 1 backup / 1 public-link / 1 https); 10 contenedores corriendo.
- **Fase 4 — matriz de arranque (Compose v5.1.3, `config --quiet` + `--services`):** core (4 servicios) OK; +observability (9) OK; +monitoring alias (9) OK; +backup (5) OK; +public-link (5) OK; +https (5) OK; +all (12) OK; full core (3) OK; full +backup (4) OK.
- **Fase 4 — sintaxis PowerShell:** parse OK de `local-start.ps1`, `local-stop.ps1`, `local-verify.ps1`, `local-reset.ps1`, `dev.ps1`, `clean-local.ps1`, `backup-db.ps1`.
- **Fase 4 — prueba de inicio limpio (ejecutada 2026-08-02):** `local-stop.ps1` detuvo y removió los 10 contenedores en 4.1s (incl. observabilidad y túnel); `local-start.ps1 -Profile bogus` falla con `exit 1` (validación de perfiles); `local-start.ps1 -Profile all` levantó 12 contenedores en 47.7s (core + observability + backup + public-link + https); `local-verify.ps1` → 12/12 OK (4 core healthy, 5 observability healthy, backup-sidecar/tunnel/caddy running), backend UP, frontend 200, login + `/api/v1/company` OK.

## 4. Limitaciones y riesgos vigentes

| Riesgo | Estado |
|---|---|
| 38 fallos + 11 errores de tests backend pre-existentes (clases de IA y agenda) | No bloqueantes para línea base; pendientes de corrección |
| Token de Cloud API estuvo en 2 scripts no versionados (raíz) | Eliminados; **rotar token** |
| Integración Meta real solo con números autorizados de prueba | Controlado por diseño (Fase 3: allowlist + doble confirmación) |
| Perfil legacy `local-whatsapp-cloud` sin doble confirmación ni allowlist | Compatibilidad; se recomienda migrar a `local-meta-controlled` |
| Docs históricos con referencias a WhatsApp Web/QR | Marcados HISTÓRICO e indexados (2026-08-02) |
| Tras la prueba de inicio limpio, el túnel público tiene una URL nueva y el webhook registrado en Meta apunta a la URL anterior | Ejecutar `.\scripts\start-public-link.ps1` para regenerar URL, actualizar `.env.local` y re-registrar el webhook |

## 5. Línea base de la Fase 2 (estado del árbol)

Línea base aplicada y pusheada (11 commits `3dd2663..d85923b` en `master`). Las Fases 3 y 4 quedan como cambios pendientes de revisión y commit. Referencias y detalles completos: `DOCUMENTATION_INDEX.md` (índice documental).

## 6. Próximos pasos

- Ejecutar `.\scripts\start-public-link.ps1` para regenerar la URL del túnel (cambió tras la prueba de inicio limpio) y re-registrar el webhook en Meta.
- Corregir los 38 fallos / 11 errores de tests backend pre-existentes.
- Rotar el token de Cloud API.
- Migrar `.env.local` real de `local-whatsapp-cloud` a `local-meta-controlled` (con ACK y allowlist).
- CI/CD con GitHub Actions y validación de documentos en pipeline.
