# Changelog

## 0.10.0 (2026-08-03) — Fase 9: respaldo y restauración de PostgreSQL comprobados

### Agregado
- **Backup real verificable**: `scripts/backup-db.ps1` / `.sh` reescritos — formato `custom`
  (`pg_dump -Fc -Z5 --no-owner --no-acl` → `.dump`), suma `.sha256`, metadatos `.metadata.json`
  sanitizados (sin credenciales ni valores sensibles). Evidencia en `backups/`.
- **Restauración segura** `scripts/restore-db.ps1` / `.sh`: verifica SHA-256, restaura a base
  temporal `asistente_whatsapp_restore_<ts>` (corrige `Invoke-PgAdmin` con param `Cmd`,
  bug de `$args` que hacía `sh -c ''` sin efecto), invoca `verify-restore-db.ps1`,
  y swap a la principal con doble confirmación explícita (`-RestoreToMain`) y limpieza de la
  temporal ante fallo.
- **Verificación estructural/funcional** `scripts/verify-restore-db.ps1` / `.sh`: conectividad,
  93 tablas, 225 FKs validadas, 0 secuencias, 0 defaults `nextval`, Flyway completo
  (rank 105, 105 success, cadena `version:checksum`), 31 tablas de control con conteos
  idénticos, e integridad referencial **comparada contra la referencia** (criterio correcto:
  la referencia tiene 1046 huérfanos de seed; here-string literal para el chequeo SQL).
- **Backend funcional contra la BD restaurada** `scripts/restore-backend-check.ps1` / `.sh`:
  contenedor temporal `backend-restore-verify` (puerto 8081, `SERVER_PORT=8081`, `ports
  8081:8081`, sin `extends` que herede el 8080 — fix de colisión), `/actuator/health` UP,
  login demo, `GET /api/v1/company` y `POST /api/v1/test/whatsapp-inbound` (escritura real).
- **6 pruebas negativas** `scripts/test-recovery-negative.ps1` / `.sh`: vacío, suma SHA
  incorrecta, truncado, falta de espacio (proxy), postgres caído+recuperación, migración
  incompatible (Flyway V999 → backend NO arranca). Resultado 13/13 PASS (2026-08-03).
- **Sidecar reforzado** `scripts/backup-sidecar-entrypoint.sh`: formato custom por defecto,
  `.sha256`, `.metadata.json`, archivo `metrics` (Prometheus text format: success/failures/
  last_success/duration/size/sha256_ok/result), retención por días, contadores persistentes,
  modo one-shot y simulación configurable. Probado 2× (1.0 MB, 1 s).
- **Observabilidad del respaldo**: servicio `backup-exporter` (imagen `busybox:1.36.1`, sirve
  `/backups/metrics` en :9100, perfil `backup`), job `backup-sidecar` en
  `monitoring/prometheus/prometheus.yml` con `fallback_scrape_protocol=PrometheusText0.0.4`
  (compat Prometheus v3 con Content-Type nulo), y grupo `asistente-respaldo` en
  `monitoring/prometheus/alerts.yml` (BackupFallido critical, BackupDesactualizado warning,
  BackupSinMetricas warning).
- Documentación canónica: `docs/informe_recuperacion_fase9.md`.

### Verificado (2026-08-03, ambiente local simulado)
- Restore+verify completo en verde contra `backups/asistente_whatsapp_20260803_102631.dump`
  (6.5 s); backend funcional contra la restaurada (puerto 8081) con escritura real.
- Conteos de control idénticos e integridad referencial coincidente con referencia.
- Base principal intacta tras todo el flujo (`business=1, booking=3, customer=5, lead=3`),
  sin BD temporales residuales (limpiadas las de sesiones de depuración).
- Prometheus: target `backup-sidecar` up y query `backup_sidecar_success_total` =
  `backup-exporter:9100` + 3 alertas `inactive/ok`.
- Negativos 13/13 PASS; `local-verify.ps1` TODO OK (13 contenedores).

### Brechas conocidas
- Swap a la principal (`-RestoreToMain`) diseñado pero no estresado en sesión (no destructor;
  validar con simulación de desastre).
- RPO/RTO medidos sobre dataset demo (~583 KB); re-medir con volumen real en producción.
- 1046 huérfanos de seed en la referencia documentados como contacto de datos (no corrupción).

## 0.9.0 (2026-08-02) — Fase 7: batería Java en verde con proveedor simulado

### Agregado
- JaCoCo (`jacoco-maven-plugin` 0.8.13) en `backend-java/pom.xml`: instrumentación en
  `prepare-agent` e informe en `verify` (sin gate de cobertura que bloquee el build).
  Informe generado en `target/site/jacoco/` y preservado en `resultados/FASE7_TESTS_JAVA_*/`.
- Preservación de evidencia fuera del código: `resultados/FASE7_TESTS_JAVA_<timestamp>/`
  con informes Surefire (70 clases), Failsafe (2 clases + summary) y JaCoCo (CSV + HTML).

### Cambiado
- **Tests desactualizados por la regla `isBusinessAiActive`** (5 clases): mockeaban
  `findSettingsOpt → Optional.empty()` y quedaron rotos cuando `AgentCoordinatorService`
  exigió settings activas para enrutar (L582-585). Corregidos para mockear settings
  activas (`active=true`, mode `auto`): `AiBookingConversationalFlowTest`,
  `AiAmbiguityAndErrorsTest`, `AiRescheduleCancelConversationalFlowTest`,
  `AiExcelMatrixOrchestratorCoverageTest`, `AiAgentCoherenceTest`.
- **Formato de duración consistente** en `AiBusinessKnowledgeService.categoryPriceResponse`
  (L106): `" (15 min)"` → `" (15 minutos)"`, alineado con `serviceInformationResponse`
  (L153) y las expectativas de los tests. Único cambio en código de producción de la fase.
- `CompleteDigitalAgendaServiceTest.createTemporaryBookingRejectsMissingInformedConsent...`:
  el request no enviaba `informedConsentAccepted=false` (campo añadido al record con 20
  parámetros quedó en `null`), por lo que la validación no se disparaba; ahora sí, y el
  test verifica el error `informedConsentAccepted` sin necesidad de horarios mockeados.
- `AiExcelMatrixOrchestratorCoverageTest`: `previewModePersistsContext...` renombrado a
  `persistedContextAllowsFragmentedClientAnswersToContinueBookingFlow`. El preview NO
  persiste contexto por diseño (gate `!request.dryRun()` desde 7e27b0e,
  `AiAdminController` → "Preview generado sin persistir"); el test ahora persiste el
  primer turno vía `coordinator.route` y verifica que el segundo turno (preview) continúa
  el flujo de booking leyendo el contexto persistido. Se añadió `routePersisted()` al
  Harness de prueba.

### Verificado
- **Batería unit completa:** `mvn test` → 697 tests, 0 failures, 0 errors, 0 skipped
  (línea base previa: 697/38F/11E). Cero tráfico externo: Testcontainers
  `postgres:16-alpine` aislado, `openai.enabled=false`, proveedor WhatsApp SIMULATED.
- **`mvn verify`** → BUILD SUCCESS: 697 unit + 7 integration Failsafe
  (`BookingConcurrencyTest` 5, `WhatsAppChannelSimulatorControllerIntegrationTest` 2).
- **Cobertura JaCoCo** (unit + integration): instrucciones 29.0 %, líneas 29.4 %,
  ramas 23.4 % (745 clases del proyecto). Paquetes mejor cubiertos: `aiagents.application`
  54.1 %, `agenda.application` 30.1 %, `channels.*` 49-69 %. Capas de infraestructura y
  módulos de administración/catálogo quedan por debajo del 10 % (hueco documentado).
- **Flujos de agenda validados** por tests en verde: reserva temporal + confirmación
  (por piezas: `CompleteDigitalAgendaServiceTest`, `BookingConfirmationServiceTest`,
  `AiBookingConversationalFlowTest`), reprogramación (`BookingServiceTest`,
  `BookingPolicyServiceTest`, `AiRescheduleCancelConversationalFlowTest`,
  `BookingConcurrencyTest`), cancelación (`BookingPublicActionServiceTest`,
  `AiRescheduleCancelConversationalFlowTest`), ambigüedad (`AiAmbiguityAndErrorsTest`),
  duplicados e idempotencia (`AvailabilityServiceTest`,
  `BookingPaymentServiceTest`), concurrencia (`BookingConcurrencyTest`,
  `rejectsSimultaneousCreationInSameSlot`), confirmación con link público
  (`BookingConfirmationServiceTest` + calendario), disponibilidad
  (`PublicLandingServiceAvailabilityTest`), validaciones de negocio
  (horarios, feriados, consentimiento informado).

### Brechas conocidas (documentadas, no bloqueantes)
- Outbox de envío WhatsApp: cobertura mínima (`AiReplyOutboxProcessorTest` solo prueba
  el caso "sin jobs pendientes"; falta envío/reintento/fallo con jobs reales).
- No existe un test único de flujo completo temporary → confirmación (se cubre por piezas).
- Reprogramación por link público: solo `previewReschedule` (sin `confirmReschedule`).
- Confirmación pública: sin test de token inexistente/inválido o link ya consumido.

## 0.8.0 (2026-08-02) — Fase 6: reproducibilidad y empaquetado

### Agregado
- `scripts/local-package.ps1` reescrito: genera el paquete distribuible del proyecto
  en un único ZIP (`target/package/asistente-package-<revision>.zip`). Pasos: compilar
  backend (`mvnw clean package -DskipTests`), construir frontend (`pnpm build`),
  registrar dependencias runtime (`mvn dependency:list`), copiar la fuente **solo con
  lista blanca** (`git ls-files` — nunca `node_modules`, `target`, `dist`, `.git`,
  `.env.*`, logs ni capturas), adjuntar artefactos compilados (jar, `frontend-dist.zip`)
  y configuración, y generar un **manifiesto JSON** (`metadata/manifest.json`, formato
  `asistente-package-manifest-v1`) con revisión git, versiones de herramientas,
  conteo de fuentes y dependencias, y **SHA-256 por archivo**. Emite además
  `SHA256SUMS.txt` con la suma del ZIP. Parámetros: `-OutputDir`, `-SkipBuild`.
- `scripts/local-package.sh`: equivalente Linux/macOS del empaquetado (mismo flujo,
  misma lista blanca, manifiesto y SHA256SUMS).
- `scripts/verify-package.ps1` y `scripts/verify-package.sh`: prueba automática de
  reconstrucción desde el paquete. Extrae el ZIP en un directorio temporal limpio y
  verifica: (1) SHA-256 de cada archivo del manifiesto, (2) ausencia de artefactos
  empaquetados accidentalmente (15 patrones auditados por segmento de ruta), (3)
  `pnpm install --frozen-lockfile`, (4) `mvnw package -DskipTests`, (5) `pnpm build`,
  (6) `docker compose config --quiet`. Exit 0/1.

### Cambiado
- **Versiones de imágenes de contenedores fijadas a tags exactas** (antes parcialmente
  variables o `latest`): `postgres:16.14-alpine` (los 4 composes), `caddy:2.11.4-alpine`
  (local/qa/prod), `cloudflare/cloudflared:2026.7.0` (local), `maven:3.9.15-eclipse-temurin-21`
  y `eclipse-temurin:21.0.11_10-jre-jammy` (`backend-java/Dockerfile`), `node:20.19.0-alpine`
  y `nginx:1.27.4-alpine` (`frontend-react/Dockerfile`). Todas verificadas con
  `docker manifest inspect` antes de aplicar.
- Node unificado al tag exacto **20.19.0** en CI: `.github/workflows/frontend-ci.yml`
  (`node-version: '20.19.0'`) y `frontend-react/.github/workflows/e2e.yml`
  (`NODE_VERSION: '20.19.0'`).
- `frontend-react/e2e/reports/` (reportes de ejecución de Playwright): removidos del
  control de versiones (`git rm --cached`); ya estaban en `.gitignore`. Ya no entran
  en el paquete.

### Verificado
- **pnpm desde limpio:** `pnpm install --frozen-lockfile` con store frío en directorio
  temporal: Done in 9.3s, exit 0 (344 paquetes; store 204.2 MB).
- **Maven desde caché vacía:** `mvnw go-offline` con `-Dmaven.repo.local` en repo
  temporal vacío (253s, 5,581 archivos) y `mvnw package` posterior con ese mismo repo:
  BUILD SUCCESS en 36s. Registro de dependencias runtime: 138.
- **Empaquetado ejecutado:** `local-package.ps1` en el entorno real — 1,524 archivos de
  fuente con lista blanca, manifiesto con 1,526 entradas (SHA-256 todas verificadas),
  ZIP final **105.67 MB** (la referencia "antes", repo completo sin lista blanca,
  pesaba **391.31 MB**; reducción ≈ 73%). `SHA256SUMS.txt` emitido.
- **Reconstrucción desde el paquete (2 ejecuciones consecutivas, criterio de
  aceptación):** `verify-package.ps1` → TODO OK en ambas (integridad SHA-256 100%,
  exclusiones limpias, pnpm install --frozen-lockfile OK, mvnw package BUILD SUCCESS,
  pnpm build OK, docker compose config válido). Exit 0.
- **Migraciones reproducibles:** 105 migraciones Flyway en el repo = 105 aplicadas en
  la BD regenerada (success=true), última aplicada `V105`; 2 reservas demo.
- Auditoría de valores variables restantes: ningún `:latest`, `:alpine` sin versión ni
  tag de Node sin patch en composes, Dockerfiles ni workflows.

### Notas
- Los composes mantienen defaults `${VAR:-fijo}` explícitos; los números de negocio
  como default están en `docker-compose.local.yml` y `docker-compose.prod.yml`
  (configuración de despliegue, no documentación).
- El stack en ejecución usa todavía las imágenes previas al pinning; las nuevas tags
  se descargan en el próximo `local-start` con recreación.

## 0.7.0 (2026-08-02) — Fase 5: experiencia de desarrollo

### Agregado
- `scripts/diagnose-local.ps1` y `scripts/diagnose-local.sh`: diagnóstico completo del
  ambiente local (toolchain Java/Maven/Node/pnpm/Docker/Compose con versiones mínimas,
  recursos de RAM/disco, puertos del stack, archivos de configuración, secretos —
  solo presencia y longitud, nunca valores —, estado de los contenedores `asistente-*`
  y estado del frontend). Cada fallo incluye una acción correctiva sugerida.
  Reporte sanitizado compartible con `-OutFile` / `-o` (gitignored: `local-diagnostics*.txt`).
  Exit code 0/1 según existan errores críticos.
- `scripts/local-reset-demo.ps1` y `scripts/local-reset-demo.sh`: regenera los datos demo
  desde cero (detiene postgres/backend, elimina el volumen `asistente_postgres-data`,
  recrea y reinicia backend para que Flyway reaplique los seeds y el
  `LocalDataInitializer` refresque las fechas de las reservas de ejemplo). No toca
  código, `node_modules`, `target`, `dist`, backups ni observabilidad. Requiere
  confirmación (o `-Force`/`--force`).
- Equivalentes Linux/macOS del flujo oficial: `scripts/local-setup.sh`,
  `scripts/local-start.sh`, `scripts/local-stop.sh`, `scripts/local-verify.sh`,
  `scripts/local-reset.sh`, `scripts/clean-local.sh` (antes solo existían los `.ps1`
  y los `.sh` de observabilidad/backup/smoke).
- `QUICKSTART_15_MIN.md`: guía única de arranque en ~15 minutos (requisitos, setup,
  start, verify, credenciales demo, diagnóstico, parada/limpieza, Linux y modos
  IDE vs contenedores). Enlazada desde `README-LOCAL.md`.
- `frontend-react/.nvmrc` (20.19.0) y `engines` en `frontend-react/package.json`
  (`node >=20.19.0`, `pnpm >=10 <11`).

### Cambiado
- Requisito de Node unificado a **20.19+** en todo el repositorio (antes 18/20/22/24
  mezclados): `frontend-react/Dockerfile` (dev y build usan `node:20-alpine`),
  `frontend-react/.github/workflows/e2e.yml` (`NODE_VERSION: '20'`, antes 22),
  `DEVELOPMENT.md`, `LOCAL_ENV_SETUP_PROMPT.md`, `frontend-react/docs/COMANDOS_EJECUCION.md`,
  `frontend-react/e2e/README_TESTS_AGENDA_WHATSAPP.md`. El lockfile ya exigía
  `^20.19.0 || ^22.12.0 || >=24.0.0`.
- `scripts/local-setup.ps1` reescrito: valida plataforma (Windows vs Linux/macOS),
  Node 20.19+ (antes >=18), pnpm 10.x, Docker Compose plugin y lockfile
  (`frontend-react/pnpm-lock.yaml`); **ya no omite la instalación solo porque exista
  `node_modules`** — siempre ejecuta `pnpm install --frozen-lockfile` (idempotente) a
  menos que se pase `-SkipInstall` explícito. Todos los mensajes de error incluyen
  acción correctiva. Corregido bug preexistente: `java -version` devolvía un array y
  `$matches` no se poblaba (fallaba con Java 25).
- `scripts/local-start.ps1` y `scripts/local-verify.ps1`: mensajes de error con acción
  correctiva sugerida.
- `DEVELOPMENT.md`: nueva sección "Desarrollo desde IDE vs Contenedores" (perfil Maven
  `local` con H2 embebido vs stack completo Docker) y scripts auxiliares nuevos.
- `README-LOCAL.md`: enlace a `QUICKSTART_15_MIN.md` y comandos nuevos
  (`local-reset-demo.ps1`, `diagnose-local.ps1`).

### Verificado
- `scripts/diagnose-local.ps1` en el entorno real: 17 OK / 1 aviso (SENTRY_DSN opcional)
  / 0 errores, exit 0. Falsos positivos corregidos durante la prueba (Compose v5.1.3,
  ubicación `caddy/Caddyfile.local`, health `none` como válido, clave
  `APP_JWT_SECRET` en `.env.local`).
- Sintaxis PowerShell (8 scripts) y Bash (`bash -n` de 7 scripts): OK.
- `local-setup.ps1` ejecutado: validación de prerequisitos OK, `pnpm install
  --frozen-lockfile` idempotente (lockfile up to date), backend compilado (BUILD
  SUCCESS, 546 fuentes) y frontend construido (vite 8, 899 módulos). Exit 0.
- **Prueba de regeneración de datos demo (ejecutada):** `local-reset-demo.ps1 -Force`
  detuvo postgres/backend-java, eliminó el volumen, recreó postgres (healthy), reinició
  backend y `local-verify.ps1` quedó TODO OK (12 contenedores, login + API con id de
  negocio `11111111-1111-1111-1111-111111111111`). Reservas demo verificadas en BD con
  fechas futuras (2026-08-03). Corregido durante la prueba: el servicio del compose se
  llama `backend-java`, no `backend`.
- `clean-local.ps1` auditado: nunca elimina datos sin confirmación (node_modules y
  volúmenes Docker requieren confirmación; nunca toca `.env.local`, backups ni datos
  fuente).

## 0.6.0 (2026-08-02) — Fase 4: orquestación y arranque local

### Agregado
- Perfil `backup` en `docker-compose.local.yml`: servicio `backup-sidecar` (contenedor `asistente-backup-sidecar`, pg_dump cron diario 04:00, retención 7 días configurable con `BACKUP_CRON_SCHEDULE`/`RETENTION_DAYS`, volumen `postgres-backups`). Activable con `.\scripts\local-start.ps1 -Profile backup`.
- Pre-flight en `local-start.ps1`: valida docker disponible, archivo compose, perfiles solicitados (`observability`, `monitoring` alias, `backup`, `public-link`, `https` o `all`) y `docker compose config --quiet` antes de levantar; advierte si `GRAFANA_ADMIN_PASSWORD` falta con el perfil `observability`; nuevo switch `-Verify` que ejecuta `local-verify.ps1` al terminar.
- `local-verify.ps1` ampliado: verifica los 4 servicios core (postgres, backend, frontend, mailpit) y los contenedores opcionales que estén corriendo (observability, backup-sidecar, public-tunnel, caddy), además de health/API.
- `backup-db.ps1`: si `pg_dump` no está en el PATH, cae a `docker compose exec postgres pg_dump`.

### Cambiado
- `docker-compose.yml` (base) renombrado a `docker-compose.full.yml`: contenedores `asistente-full-*`, volúmenes propios (`postgres-full-data`, `postgres-full-backups`), red propia `asistente-full` y encabezado con advertencia de colisión. NO debe levantarse simultáneamente con `docker-compose.local.yml` (puertos compartidos 5433/8080/5173).
- `local-stop.ps1`, `local-reset.ps1`, `clean-local.ps1`, `dev.ps1`: `down` incluye ahora todos los perfiles opcionales para que ningún contenedor del compose local quede corriendo; `dev.ps1` usa `--env-file .env.local` y agrega el comando `verify`.

### Documentado
- `README-LOCAL.md`: sección "Comandos oficiales (Fase 4)" con la matriz de perfiles, tabla de puertos ampliada (mailpit, backup-sidecar, caddy), backup de BD y nota sobre `docker-compose.full.yml`.
- `DEVELOPMENT.md`: sección Docker Compose reescrita con los comandos oficiales idénticos a `README-LOCAL.md`.
- `docs/AGENTS.md`: estructura esperada del repositorio y comandos contractuales actualizados (compose local con `--env-file`; base renombrada a `docker-compose.full.yml`).
- `docs/SOLUCION_DNS_MAVEN_DOCKER.md`: referencia a `docker-compose.full.yml`.

### Verificado
- Matriz de arranque `docker compose config --quiet` (Compose v5.1.3) 9/9 combinaciones OK: core, +observability, +monitoring, +backup, +public-link, +https, +all, full core, full +backup.
- Sintaxis PowerShell de los 7 scripts modificados: OK.
- **Prueba de inicio limpio (ejecutada):** `local-stop.ps1` removió los 10 contenedores en 4.1s; `local-start.ps1 -Profile bogus` falla con `exit 1`; `local-start.ps1 -Profile all` levantó 12 contenedores en 47.7s; `local-verify.ps1` 12/12 OK + login/API.
- **Corregido durante la prueba:** en `local-start.ps1` los `--profile` del pre-flight van antes del subcomando `config` (requisito de Compose v5), igual que en el resto del script.

## 0.5.0 (2026-08-02) — Fase 3: aislamiento local y modo simulado seguro

### Agregado
- Perfil Spring `local-safe`: modo local predeterminado sin tráfico externo (WhatsApp `SIMULATED`, Cloud API off, OpenAI off, espejo de correo off, calendario Google off, pagos `SIMULATED`, auto-reply off, safe-mode on). Activado por defecto en `docker-compose.local.yml` (`SPRING_PROFILES_ACTIVE` default `local,local-safe`).
- Perfil Spring `local-meta-controlled`: integración real controlada con WhatsApp Cloud API con doble confirmación (`APP_LOCAL_META_CONTROLLED_ACKNOWLEDGED=true`), lista permitida de teléfonos de prueba (`APP_WHATSAPP_CLOUD_API_ALLOWED_TEST_PHONES`), credenciales completas obligatorias, firma de webhook requerida y dry-run desactivado.
- Compuerta de arranque `LocalEnvironmentGate` (patrón `EmailConfigValidator`): valida la configuración efectiva al iniciar y **falla con mensaje claro** si el entorno habilita tráfico no autorizado; nunca imprime valores de secretos, solo nombres de propiedades.
- `EgressTrafficGuard`: interceptor en el `RestClient.Builder` compartido que bloquea llamadas HTTP salientes a hosts externos (Meta, OpenAI, Gmail, Mercado Pago, etc.) en el perfil `local-safe`; permite solo localhost y hosts del stack de contenedores.
- Lista permitida de teléfonos en el webhook Cloud API: los mensajes de números no autorizados se descartan (respuesta ACCEPTED para evitar reintentos) y se registran como rechazados.
- Tests: `LocalEnvironmentPolicyTest` (18), `EgressTrafficGuardTest` (7) y 3 casos de allowlist en `WhatsAppCloudWebhookParserTest`. Suite backend completa: 697 tests con 38 fallos/11 errores pre-existentes en clases de IA/agenda (sin cambios respecto de la línea base).

### Corregido
- **Seguridad (defaults de compose)**: `APP_AI_AGENTS_AUTO_REPLY_ENABLED` ahora `false` por defecto (antes `true`), `APP_AI_AGENTS_SAFE_MODE_ENABLED` ahora `true` (antes `false`), `APP_EMAIL_MIRROR_ENABLED` ahora `false` (antes `true` con host `smtp.gmail.com` real). El arranque por defecto ya no envía tráfico externo.
- `.env.local.template`: `APP_OPENAI_ENABLED=false` por defecto, mirror Gmail desactivado, nuevas variables de la fase Meta controlada y perfil default `local,local-safe`.
- Import estático roto en `WhatsAppCloudWebhookValidatorTest` (`org.assertj.core.Assertions` → `org.assertj.core.api.Assertions`).

### Documentado
- `README-LOCAL.md`: modalidades de aislamiento local (segura / Meta controlada), comportamientos de la compuerta y del guard, perfil legacy `local-whatsapp-cloud` marcado para migración.
- `docs/AMBIENTES_WHATSAPP.md`: tabla de modalidades locales y sus controles.

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
