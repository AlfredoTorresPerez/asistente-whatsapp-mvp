# Reporte de Madurez Local — Asistente WhatsApp MVP

**Fecha:** 2026-08-02
**Branch:** `master`
**Commit:** `d85923b` (línea base Fase 2) + cambios de Fase 3, Fase 4 y Fase 5 pendientes de commit
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
- **Fase 7:** contexto multi-turno persistente validado: reservas fragmentadas ("quiero agendar mañana a las 16:00" → "depilación bozo") continúan el flujo leyendo el contexto de `route`; preview es solo lectura por diseño (no contamina contextos reales).
- **Fase 7:** flujos conversacionales de agenda en verde (reserva, reprogramación, cancelación, ambigüedad, consultas comerciales) — 311 tests de IA en las 7 clases de la matriz + flujos de agenda, 0 fallos.

### 2.5 Reproducibilidad — ✅ ALTA
- `README-LOCAL.md` (canónico), `DEVELOPMENT.md`, `CHANGELOG.md`.
- **Fase 4:** un solo flujo oficial de arranque con `docker-compose.local.yml` como fuente canónica: `local-start.ps1` (pre-flight: docker, archivo compose, perfiles válidos, `config --quiet`) / `local-stop.ps1` / `local-verify.ps1` / `local-reset.ps1`; todos devuelven código de error ante fallo y detienen/verifican también los perfiles opcionales.
- **Fase 4:** perfiles opcionales integrados — `observability`, `backup` (nuevo: backup-sidecar pg_dump cron diario), `public-link`, `https`; matriz de arranque `docker compose config --quiet` 9/9 combinaciones OK (Compose v5.1.3).
- **Fase 4:** compose base renombrado a `docker-compose.full.yml` con contenedores `asistente-full-*`, volúmenes y red propios (cero colisiones de nombres/puertos/volúmenes con el stack local); NO simultáneo con el local (puertos compartidos).
- Scripts de ciclo de vida: `local-setup/start/stop/reset/verify/package/clean` + `observability-verify.ps1` (6/6 OK, 530 spans en Tempo) + `grafana-captures.mjs`.
- 105 migraciones Flyway versionadas (`backend-java/src/main/resources/db/migration/V1..V105`), 0 repetibles.
- Backend: `mvn spotless:apply` previo obligatorio antes de compilar.
- **Fase 5:** `QUICKSTART_15_MIN.md` como guía única de arranque en ~15 minutos (enlazada desde `README-LOCAL.md`), con requisitos unificados y comandos con resultado esperado paso a paso.
- **Fase 5:** `scripts/diagnose-local.ps1`/`.sh` — diagnóstico completo (toolchain, recursos, puertos, config, secretos solo presencia/longitud, stack, frontend) con acción correctiva por fallo y reporte sanitizado compartible (`-OutFile`, gitignored).
- **Fase 5:** `local-reset-demo.ps1`/`.sh` — datos demo regenerables desde cero (recrea volumen postgres; Flyway + `LocalDataInitializer` refrescan seeds y fechas), sin tocar código ni artefactos.
- **Fase 5:** equivalentes Linux/macOS del flujo oficial (`local-setup.sh`, `local-start.sh`, `local-stop.sh`, `local-verify.sh`, `local-reset.sh`, `clean-local.sh`).
- **Fase 5:** requisito de Node unificado a 20.19+ (Dockerfile `node:20-alpine`, CI `20`, `.nvmrc`, `engines` en `package.json`, docs) — antes 18/20/22/24 mezclados.
- **Fase 6:** **versiones de imágenes fijadas a tags exactas** en los 4 composes, ambos Dockerfiles y los 2 workflows CI (verificado con `docker manifest inspect` antes de aplicar): `postgres:16.14-alpine`, `caddy:2.11.4-alpine`, `cloudflare/cloudflared:2026.7.0`, `maven:3.9.15-eclipse-temurin-21`, `eclipse-temurin:21.0.11_10-jre-jammy`, `node:20.19.0-alpine`, `nginx:1.27.4-alpine`. Cero `:latest`/tags sin patch restantes.
- **Fase 6:** Node en CI al tag exacto **20.19.0** (`frontend-ci.yml`, `e2e.yml`).
- **Fase 6:** `local-package.ps1`/`.sh` reescritos — paquete único ZIP con lista blanca (`git ls-files`), manifiesto `asistente-package-manifest-v1` (revisión, herramientas, dependencias runtime, SHA-256 por archivo) y `SHA256SUMS.txt`.
- **Fase 6:** `verify-package.ps1`/`.sh` — reconstrucción verificada desde el paquete (SHA-256 de todos los archivos, exclusiones, `pnpm install --frozen-lockfile`, `mvnw package`, `pnpm build`, `docker compose config`), idempotente (2 ejecuciones consecutivas OK).
- **Fase 6:** `frontend-react/e2e/reports/` removido del control de versiones (reportes de ejecución Playwright; ya ignorado) — no entran en el paquete.

### 2.6 Protección de secretos — ✅ FUERTE (con hallazgo corregido)
- `.env.local` con `GRAFANA_ADMIN_PASSWORD` y credenciales Meta no versionado.
- `database/manual/`, `docs/observabilidad-capturas/`, `registro_ejecucion_IA.json`, `frontend-react/e2e/reports/` y `MEMORY.md` añadidos a `.gitignore` (2026-08-02).
- Escaneo `git log -S "EAAV..."`: sin token en historial.

### 2.7 Recuperación (Fase 9, 2026-08-03) — ✅ FUERTE
- **Backup real verificable**: formato `custom` (`pg_dump -Fc -Z5 --no-owner --no-acl`),
  suma SHA-256 verificada antes de restaurar, metadatos `.metadata.json` sanitizados.
- **Restauración segura a base temporal** (nunca toca la principal por defecto); swap a la
  principal con doble confirmación explícita (`-RestoreToMain` + `CONFIRMAR` + nombre).
- **Verificación estructural/funcional** de la restaurada vs referencia: conectividad,
  93 tablas, 225 FKs, 0 secuencias, 0 defaults `nextval`, Flyway 105/105 con cadena
  `version:checksum`, 31 registros de control, integridad referencial comparada contra la
  referencia.
- **Backend funcional contra la BD restaurada** (puerto 8081): health UP, login, company,
  inbound con escritura real.
- **6 pruebas negativas (13/13 PASS)**: vacío, suma incorrecta, truncado, falta de espacio,
  postgres caído + recuperación, migración incompatible.
- **Observabilidad del respaldo**: `backup-exporter` (:9100) + job Prometheus
  `backup-sidecar` + alertas `BackupFallido/BackupDesactualizado/BackupSinMetricas`
  (verificadas `inactive/ok`).
- **RPO ≤ 24 h** (cron diario 04:00), **RTO local ≈ 1 min** (restore ~6 s + verificación +
  arranque backend); línea base sobre dataset demo.
- Base principal intacta tras la batería completa; sin BD temporales residuales.

## 3. Evidencia de ejecución

- **Frontend:** 19 archivos de prueba, 180/180 OK (Vitest).
- **Backend — Fase 7 (ejecutado 2026-08-02):** suite completa **697 tests / 0 fallos / 0 errores / 0 skipped** (`mvn test`; proveedor SIMULADO, Testcontainers aislado, `openai.enabled=false`). Línea base previa: 697/38F/11E (7 clases de IA/agenda, tests desactualizados por la regla `isBusinessAiActive` y cambios de formato — corregidos, sin relajar reglas de producción; único cambio de producción: formato de duración "minutos").
- **Backend — Fase 7 `mvn verify`:** BUILD SUCCESS — 697 unit + **7 integration** Failsafe (`BookingConcurrencyTest` 5 + `WhatsAppChannelSimulatorControllerIntegrationTest` 2).
- **Backend — Fase 7 cobertura JaCoCo** (unit + integration, 745 clases): instrucciones 29.0 %, líneas 29.4 %, ramas 23.4 %. Mejor cubiertos: `aiagents.application` 54.1 %, `agenda.application` 30.1 %, `channels.*` 49–69 %; módulos de administración/catálogo e infraestructura < 10 % (hueco documentado en CHANGELOG).
- **Backend — Fase 7 evidencia preservada:** `resultados/FASE7_TESTS_JAVA_20260802T213641/` (informes Surefire 70 clases, Failsafe 2 clases + summary, JaCoCo CSV+HTML) — fuera del código.
- **E2E Playwright:** `evidencia-reprogramacion.spec.ts` y `reservar-fecha-hora.spec.ts` (evidencia en `frontend-react/e2e/reports/`).
- **Observabilidad:** `scripts/observability-verify.ps1` → 6/6 OK; 530 spans en Tempo; 6 capturas Grafana en `docs/observabilidad-capturas/` (no versionadas); dashboards `asistente-*` (resumen-general, agenda-reservas, inteligencia-artificial, whatsapp-cloud-api, infraestructura, registros-trazas).
- **Conteos:** 105 migraciones versionadas; 12 servicios compose local (4 core / 5 observability / 1 backup / 1 public-link / 1 https); 10 contenedores corriendo.
- **Fase 4 — matriz de arranque (Compose v5.1.3, `config --quiet` + `--services`):** core (4 servicios) OK; +observability (9) OK; +monitoring alias (9) OK; +backup (5) OK; +public-link (5) OK; +https (5) OK; +all (12) OK; full core (3) OK; full +backup (4) OK.
- **Fase 4 — sintaxis PowerShell:** parse OK de `local-start.ps1`, `local-stop.ps1`, `local-verify.ps1`, `local-reset.ps1`, `dev.ps1`, `clean-local.ps1`, `backup-db.ps1`.
- **Fase 4 — prueba de inicio limpio (ejecutada 2026-08-02):** `local-stop.ps1` detuvo y removió los 10 contenedores en 4.1s (incl. observabilidad y túnel); `local-start.ps1 -Profile bogus` falla con `exit 1` (validación de perfiles); `local-start.ps1 -Profile all` levantó 12 contenedores en 47.7s (core + observability + backup + public-link + https); `local-verify.ps1` → 12/12 OK (4 core healthy, 5 observability healthy, backup-sidecar/tunnel/caddy running), backend UP, frontend 200, login + `/api/v1/company` OK.
- **Fase 5 — diagnóstico (ejecutado 2026-08-02):** `diagnose-local.ps1 -OutFile local-diagnostics.txt` → 17 OK / 1 aviso (SENTRY_DSN opcional) / 0 errores, exit 0; reporte sanitizado sin valores de secretos.
- **Fase 5 — setup desde estado actual (ejecutado 2026-08-02):** `local-setup.ps1` validó plataforma/Java 25/Docker/Compose/Node 24/pnpm 10.18.3/lockfile, `pnpm install --frozen-lockfile` idempotente (lockfile up to date), backend compilado (BUILD SUCCESS) y frontend construido (vite 8). Exit 0.
- **Fase 5 — regeneración de datos demo (ejecutada 2026-08-02):** `local-reset-demo.ps1 -Force` detuvo postgres/backend-java, eliminó volumen `asistente_postgres-data`, recreó postgres (healthy), reinició backend; `local-verify.ps1` → TODO OK (12 contenedores, login + company id `11111111-1111-1111-1111-111111111111`); reservas demo con fechas futuras verificadas en BD (2026-08-03).
- **Fase 5 — sintaxis:** PowerShell parse OK (8 scripts), Bash `bash -n` OK (7 scripts).
- **Fase 6 — pnpm desde limpio (ejecutado 2026-08-02):** `pnpm install --frozen-lockfile` con store frío en dir temporal → Done in 9.3s, exit 0 (344 paquetes, store 204.2 MB).
- **Fase 6 — Maven desde caché vacía (ejecutado 2026-08-02):** `mvnw go-offline` con repo local vacío → 253s, 5,581 archivos; `mvnw package` con ese repo → BUILD SUCCESS 36s; `dependency:list` runtime → 138 dependencias.
- **Fase 6 — empaquetado (ejecutado 2026-08-02):** `local-package.ps1` → 1,524 archivos fuente con lista blanca, manifiesto 1,526 entradas, ZIP `asistente-package-6a8d5d7.zip` **105.67 MB** (referencia sin lista blanca: 391.31 MB, −73%); `SHA256SUMS.txt` emitido.
- **Fase 6 — reconstrucción desde paquete (ejecutado 2026-08-02):** `verify-package.ps1` ×2 consecutivas → TODO OK en ambas (SHA-256 100%, 15 patrones de exclusión limpios, pnpm/mvnw/build/compose OK), exit 0.
- **Fase 6 — migraciones (ejecutado 2026-08-02):** 105 migraciones en repo = 105 aplicadas (success=true, última V105); 2 reservas demo — mismo estado que Fase 5.
- **Fase 9 — respaldo (ejecutado 2026-08-03):** backups reales `asistente_whatsapp_20260803_095924.dump` (580 KB) y `_102631.dump` (583 KB) + sidecar `_135937.dump` y `_144340.dump` (~1 MB, 1 s), todos con `.sha256` + `.metadata.json` + `metrics`; contadores sidecar `success_total=2, failures_total=0`.
- **Fase 9 — restore+verify (ejecutado 2026-08-03):** restauración a `asistente_whatsapp_restore_20260803_102924` en 6.5 s; verify completo en verde (tablas 93, FKs 225, Flyway 105, 31 controles, huérfanos 1046 = referencia); base principal intacta (`business=1, booking=3, customer=5, lead=3`).
- **Fase 9 — backend contra BD restaurada (ejecutado 2026-08-03):** `restore-backend-check.ps1` → health UP, login OK, `GET /api/v1/company` id demo, inbound 200 (escritura real); contenedor temporal eliminado en `finally`.
- **Fase 9 — negativos (ejecutado 2026-08-03):** `test-recovery-negative.ps1` → **13/13 PASS** (vacío, suma incorrecta, truncado, falta de espacio, postgres caído+recuperación, migración incompatible); `local-verify.ps1` TODO OK (13 contenedores).
- **Fase 9 — observabilidad (ejecutado 2026-08-03):** Prometheus target `backup-sidecar` **up** con `backup_sidecar_success_total` (job/instance labels correctos); grupo `asistente-respaldo` con 3 alertas `inactive/ok`; query directa al exporter :9100 OK.

## 4. Limitaciones y riesgos vigentes

| Riesgo | Estado |
|---|---|
| ~~38 fallos + 11 errores de tests backend pre-existentes (clases de IA y agenda)~~ | **RESUELTO en Fase 7 (2026-08-02): 0 fallos / 0 errores** |
| Cobertura de infraestructura (< 10 % en módulos de administración/catálogo) y outbox con jobs | Hueco documentado en CHANGELOG 0.9.0; no bloqueante |
| Token de Cloud API estuvo en 2 scripts no versionados (raíz) | Eliminados; **rotar token** |
| Integración Meta real solo con números autorizados de prueba | Controlado por diseño (Fase 3: allowlist + doble confirmación) |
| Perfil legacy `local-whatsapp-cloud` sin doble confirmación ni allowlist | Compatibilidad; se recomienda migrar a `local-meta-controlled` |
| Docs históricos con referencias a WhatsApp Web/QR | Marcados HISTÓRICO e indexados (2026-08-02) |
| Tras la prueba de inicio limpio, el túnel público tiene una URL nueva y el webhook registrado en Meta apunta a la URL anterior | Ejecutar `.\scripts\start-public-link.ps1` para regenerar URL, actualizar `.env.local` y re-registrar el webhook |
| Imágenes del stack en ejecución construidas con `node:24-alpine` (cambio a `node:20-alpine` aplica en el próximo `local-start.ps1 -Build`) | No bloqueante; alinear imágenes en el próximo build |
| Tags de imágenes fijados en composes/Dockerfiles pero el stack corriendo aún usa las versiones previas | No bloqueante; se descargan al recrear con `local-start.ps1` (próximo `up`) |
| Números de negocio como defaults en `docker-compose.local.yml` y `docker-compose.prod.yml` | Configuración de despliegue (no documentación); revisar si deben migrar a env explícito |
| Swap de restauración a la principal (`-RestoreToMain`) no estresado en sesión | Diseñado con doble confirmación; validar con simulación de desastre controlada |
| RPO/RTO medidos sobre dataset demo (~583 KB) | Re-medir con volumen real en producción |

## 5. Línea base de la Fase 2 (estado del árbol)

Línea base aplicada y pusheada (11 commits `3dd2663..d85923b` en `master`). Las Fases 3 y 4 quedan como cambios pendientes de revisión y commit. Referencias y detalles completos: `DOCUMENTATION_INDEX.md` (índice documental).

## 6. Próximos pasos

- Commitear las Fases 5, 6, 7 y 9 (cambios en `master` desde `6a8d5d7`).
- Validar el swap a la base principal (`restore-db.ps1 -RestoreToMain`) en una simulación de desastre controlada.
- Ejecutar `.\scripts\start-public-link.ps1` para regenerar la URL del túnel (cambió tras la prueba de inicio limpio) y re-registrar el webhook en Meta (la regeneración de datos demo recreó la BD; re-verificar el estado del webhook).
- Reconstruir imágenes con Node 20.19+ (`.\scripts\local-start.ps1 -Build`) para alinear el stack en ejecución.
- Cerrar huecos de cobertura documentados: outbox de envío con jobs (reintentos/fallos), flujo E2E único temporary→confirmación, `confirmReschedule` por link público, token de confirmación inválido/ya usado.
- Rotar el token de Cloud API.
- Migrar `.env.local` real de `local-whatsapp-cloud` a `local-meta-controlled` (con ACK y allowlist).
- CI/CD con GitHub Actions y validación de documentos en pipeline.
