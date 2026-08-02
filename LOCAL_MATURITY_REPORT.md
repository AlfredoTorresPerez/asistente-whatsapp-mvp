# Reporte de Madurez Local — Asistente WhatsApp MVP

**Fecha:** 2026-08-02
**Branch:** `master`
**Commit:** `e9a321bca89d386ef0e80b7001deda91da714347` (HEAD) — árbol de trabajo con 91 cambios pendientes de línea base (Fase 2)
**Contexto de evaluación:** `LOCAL_MATURITY_CONTEXT.md` (obligatorio para evaluadores)

---

## 1. Modalidades del ambiente local

| Modalidad | Propósito | Canal WhatsApp |
|---|---|---|
| **Local simulada** | Desarrollo, pruebas automáticas, sin servicios externos | `SIMULATED` (embebido en el backend, default) |
| **Local de integración real controlada** | Pruebas conversacionales completas con WhatsApp Cloud API (Meta) | `META_CLOUD_API` (webhook firmado `X-Hub-Signature-256`) |

La activación de WhatsApp Cloud API en local es una decisión intencional y necesaria: solo intervienen números autorizados (número empresarial prepago dedicado a pruebas + número personal autorizado como cliente de prueba), no hay clientes reales, Meta exige activación explícita, y el proveedor `SIMULATED` permanece disponible.

## 2. Estado por dimensión

### 2.1 Aislamiento local — ✅ FUERTE
- `docker-compose.local.yml`: 11 servicios declarados en 4 perfiles — core: `postgres`, `backend-java`, `frontend-react`, `mailpit`; `observability`: `prometheus`, `loki`, `tempo`, `alloy`, `grafana`; `public-link`: `public-tunnel`; `https`: `caddy`.
- 10 contenedores en ejecución, todos healthy (verificado 2026-08-02).
- Sin credenciales en repositorio; `.env.local` no versionado (patrón `.env.*` en `.gitignore` con excepciones de plantillas).
- Datos exclusivamente de prueba (seeds demo multisucursal).
- No hay comunicación con producción desde el ambiente local.

### 2.2 Seguridad de la integración — ✅ FUERTE
- Activación explícita del perfil Meta (`APP_WHATSAPP_CHANNEL_PROVIDER` + variables de Cloud API solo en `.env.local`).
- Número empresarial prepago dedicado a pruebas; lista permitida de clientes de prueba.
- Validación de firma `X-Hub-Signature-256` (HMAC-SHA256) en el webhook.
- Prevención de duplicados por `external_message_id` único (V57) e idempotencia de operaciones de reserva (V86).
- Límites de frecuencia e interruptor de emergencia en el canal.
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
- Scripts de ciclo de vida: `local-setup/start/stop/reset/verify/package/clean` + `observability-verify.ps1` (6/6 OK, 530 spans en Tempo) + `grafana-captures.mjs`.
- 105 migraciones Flyway versionadas (`backend-java/src/main/resources/db/migration/V1..V105`), 0 repetibles.
- Backend: `mvn spotless:apply` previo obligatorio antes de compilar.

### 2.6 Protección de secretos — ✅ FUERTE (con hallazgo corregido)
- `.env.local` con `GRAFANA_ADMIN_PASSWORD` y credenciales Meta no versionado.
- `database/manual/`, `docs/observabilidad-capturas/`, `registro_ejecucion_IA.json`, `frontend-react/e2e/reports/` y `MEMORY.md` añadidos a `.gitignore` (2026-08-02).
- Escaneo `git log -S "EAAV..."`: sin token en historial.

## 3. Evidencia de ejecución

- **Frontend:** 19 archivos de prueba, 180/180 OK (Vitest).
- **Backend:** clasificación histórica 638 tests / 38 fallos / 11 errores, todos pre-existentes en 7 clases de IA/agenda (no introducidos por la instrumentación de observabilidad).
- **E2E Playwright:** `evidencia-reprogramacion.spec.ts` y `reservar-fecha-hora.spec.ts` (evidencia en `frontend-react/e2e/reports/`).
- **Observabilidad:** `scripts/observability-verify.ps1` → 6/6 OK; 530 spans en Tempo; 6 capturas Grafana en `docs/observabilidad-capturas/` (no versionadas); dashboards `asistente-*` (resumen-general, agenda-reservas, inteligencia-artificial, whatsapp-cloud-api, infraestructura, registros-trazas).
- **Conteos:** 105 migraciones versionadas; 11 servicios compose (4 core / 5 observability / 1 public-link / 1 https); 10 contenedores corriendo.

## 4. Limitaciones y riesgos vigentes

| Riesgo | Estado |
|---|---|
| 38 fallos + 11 errores de tests backend pre-existentes (clases de IA y agenda) | No bloqueantes para línea base; pendientes de corrección |
| Token de Cloud API estuvo en 2 scripts no versionados (raíz) | Eliminados; **rotar token** |
| Integración Meta real solo con números autorizados de prueba | Controlado por diseño |
| Docs históricos con referencias a WhatsApp Web/QR | Marcados HISTÓRICO e indexados (2026-08-02) |

## 5. Línea base de la Fase 2 (estado del árbol)

Referencias y detalles completos: `DOCUMENTATION_INDEX.md` (índice documental).

### Secuencia de confirmaciones convencionales propuesta (no ejecutada)

1. `chore(security): eliminar scripts con token de Cloud API en texto plano`
2. `chore: dejar fuera de control de versiones evidencia, capturas y SQL manual`
3. `docs: actualizar informe de madurez local y marcar documentos históricos`
4. `docs: corregir referencias a canal WhatsApp en contratos y guías`
5. `docs: crear índice documental y validación de enlaces`
6. `feat(observabilidad): métricas de negocio, logs sanitizados y cliente de errores` (+ infraestructura monitoring)
7. `feat(ia): catálogo semántico V103-V105 y ajustes de intención`
8. `feat(reservas): reprogramación pública y detalle ampliado`
9. `feat(reserva pública): mejora de paso fecha/hora y deduplicación de slots`
10. `test(e2e): escenarios de reprogramación y reserva pública`
11. `chore(release): changelog de la fase`

### Resultado esperado
`git status` limpio salvo cambios intencionales; árbol revisable commit a commit; documentación vigente e histórica diferenciada; validación automática de enlaces/comandos en `scripts/validate-docs.ps1`.

## 6. Próximos pasos

- Aplicar la secuencia de confirmaciones de la sección 5.
- Corregir los 38 fallos / 11 errores de tests backend pre-existentes.
- Rotar el token de Cloud API.
- CI/CD con GitHub Actions y validación de documentos en pipeline.
