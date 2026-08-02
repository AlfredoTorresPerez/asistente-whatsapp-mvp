# Observabilidad Local — Asistente WhatsApp MVP

Stack completo de observabilidad para el entorno local, activado con el perfil de Docker Compose `observability` y el perfil Spring `observability`.

## Componentes y puertos

| Servicio | Contenedor | Puerto | Rol |
|----------|-----------|--------|-----|
| Backend (perfil `observability`) | asistente-backend | 8080 | Expone `/actuator/prometheus`, `/actuator/health` y `/api/v1/observability/client-errors`; logs JSON; trazas OTLP |
| Prometheus | asistente-prometheus | 9090 | Scrape de métricas y reglas de alerta |
| Loki | asistente-loki | 3100 | Logs (recibe JSON vía Alloy) |
| Alloy | asistente-alloy | 12345 | Agente de recolección: Prometheus remote-write, Loki, Tempo OTLP |
| Tempo | asistente-tempo | 3200 | Trazas (OTLP HTTP en 4318) |
| Grafana | asistente-grafana | 3000 | Dashboards, alertas y exploración (admin / `GRAFANA_ADMIN_PASSWORD`) |

> El backend activa el perfil Spring `observability` siempre en el compose local (`SPRING_PROFILES_ACTIVE=local,observability`).
> Ese perfil habilita: logs JSON (Logstash), tracing OTLP con sampling 1.0, métricas funcionales y health checks extendidos.

## Quick start

```powershell
# 1. Definir la password de Grafana (obligatorio)
Add-Content .env.local "GRAFANA_ADMIN_PASSWORD=cambia-esta-password"

# 2. Levantar el stack (backend + frontend + observabilidad)
.\scripts\observability-start.ps1

# 3. Verificar que todo responde
.\scripts\observability-verify.ps1

# 4. Entrar a Grafana
#    http://localhost:3000  (admin / GRAFANA_ADMIN_PASSWORD)
```

También funciona vía el script general:

```powershell
.\scripts\local-start.ps1 -Profile observability
```

## Endpoints de verificación

| Recurso | URL | Esperado |
|---------|-----|----------|
| Health backend | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Health detallado | http://localhost:8080/actuator/health/detail | Indica estado de `outbox`, `tareasProgramadas`, `iaProveedor`, `whatsApp`, `flyway` |
| Métricas Prometheus | http://localhost:8080/actuator/prometheus | `assistente_*` |
| Prometheus targets | http://localhost:9090/targets | backend-java UP |
| Loki ready | http://localhost:3100/ready | 200 |
| Tempo ready | http://localhost:3200/ready | 200 |
| Grafana health | http://localhost:3000/api/health | `{"database":"ok"}` |

## Métricas funcionales (prefijo `assistente_`)

Registradas por `shared/observability/BusinessMetrics.java`:

- **WhatsApp**: `assistente_whatsapp_mensajes_recibidos_total`, `..._enviados_total`, `..._fallidos_total`, `..._webhooks_recibidos_total`, `..._webhooks_firma_invalida_total`, `..._eventos_duplicados_total`
- **Conversaciones**: `assistente_conversaciones_iniciadas_total`, `..._derivadas_total`
- **Intenciones**: `assistente_intenciones_detectadas_total{intencion}`, `assistente_intenciones_ambiguas_total`
- **Reservas**: `assistente_reservas_creadas_total`, `..._confirmadas_total`, `..._reprogramadas_total`, `..._canceladas_total`, `..._conflicto_total`, `..._expiradas_total`, `assistente_reservas_operaciones_duracion_seconds{operacion}`
- **Disponibilidad**: `assistente_disponibilidad_consultas_total`, `..._sin_horarios_total`, `assistente_disponibilidad_duracion_seconds`
- **IA**: `assistente_ia_solicitudes_total`, `..._respuestas_exitosas_total`, `..._respuestas_fallidas_total`, `assistente_ia_derivaciones_total{proveedor}`, `assistente_ia_modo_seguro_total`, `assistente_ia_duracion_seconds`
- **Notificaciones**: `assistente_notificaciones_enviadas_total`, `..._fallidas_total`, `..._reintentos_total`
- **Outbox**: `assistente_outbox_procesadas_total`, `..._fallidas_total`, `assistente_outbox_pendientes`, `assistente_outbox_antiguedad_maxima_segundos`
- **Tareas programadas**: `assistente_tareas_programadas_exitosas_total{tarea}`, `..._fallidas_total{tarea}`
- **Infra**: `assistente_flyway_estado`

## Health checks extendidos

Expuestos en `/actuator/health` bajo el perfil `observability`:

| Indicador | Comportamiento |
|-----------|---------------|
| `outbox` | DOWN si pendientes > 50 o antigüedad > 15 min (configurable) |
| `tareasProgramadas` | DOWN si la última ejecución de una tarea programada falló |
| `iaProveedor` | DOWN si el último resultado del proveedor de IA fue un fallo |
| `whatsApp` | Siempre UP; detalle del proveedor (SIMULATED / META_CLOUD_API) |
| `flyway` | UP/DOWN según la inspección de migraciones al arranque |

## Trazas (Tempo)

- El backend envía trazas OTLP HTTP a `http://localhost:4318/v1/traces` (`OTLP_TRACING_ENDPOINT` sobreescribible).
- Cada petición HTTP del backend genera un span (auto-instrumentación Micrometer/OpenTelemetry).
- Las llamadas salientes (OpenAI, Meta Graph API, WhatsApp Cloud API) propagan `traceparent` y `X-Correlation-Id`.
- Las tareas programadas generan spans `tarea-programada.<tarea>` (aspecto `ScheduledTaskMetricsAspect`).
- Ver trazas en Grafana → Explore → Tempo. La alerta `TempoSinTrazas` avisa si el backend deja de emitir.

## Errores de frontend

El frontend envía errores no manejados a `POST /api/v1/observability/client-errors` (público):

- `GlobalErrorBoundary` captura errores de render + `window.onerror` + `unhandledrejection`.
- `clientErrorReporter` trunca los campos (500/8000/500/120/80), redacta datos sensibles y limita a 1 reporte cada 2 s.
- El backend valida `Origin`/`Referer`, limita a 20/min por cliente y registra en el logger `APP_CLIENT_ERROR`.
- Para probar: `curl -X POST http://localhost:8080/api/v1/observability/client-errors -H "Origin: http://localhost:5173" -H "Content-Type: application/json" -d '{"message":"demo","stack":"st","url":"http://localhost:5173/","component":"Demo","errorType":"TypeError"}'`

## Dashboards de Grafana

Provisionados desde `monitoring/grafana/dashboards/` (carpeta "Asistente", auto-actualización cada 30 s):

1. **Resumen General** (`asistente-resumen-general`)
2. **Agenda y Reservas**
3. **WhatsApp Cloud API**
4. **Inteligencia Artificial**
5. **Registros y Trazas** (Loki + Tempo)
6. **Infraestructura** (JVM, Postgres, colas)

## Alertas de Prometheus

Definidas en `monitoring/prometheus/alerts.yml`; se visualizan en Grafana → Alertas:

- `BackendJavaCaido`, `TasaErroresAlta`, `LatenciaP95Alta`, `MemoriaHeapAlta`
- `MetaFallosFrecuentes`, `FirmaMetaInvalida`
- `ColaIaAcumulada`, `ColaIaAntigua`, `IaProveedorNoDisponible`
- `TareaProgramadaFallida`, `ReservasConflicto`, `ReintentosAnormales`, `ReservasErrorRepetido`
- `PostgresNoDisponible`, `LokiSinRegistros`, `TempoSinTrazas`

## Scripts

| Script | Función |
|--------|---------|
| `scripts/observability-start.ps1` / `.sh` | Levanta prometheus, loki, tempo, alloy, grafana (valida `GRAFANA_ADMIN_PASSWORD`) |
| `scripts/observability-stop.ps1` / `.sh` | Detiene el stack (`-v` borra datos) |
| `scripts/observability-verify.ps1` / `.sh` | Verifica contenedores, endpoints, targets, trazas y dashboards |
| `scripts/observability-reset.ps1` / `.sh` | Detiene y borra los datos de observabilidad |

## Solución de problemas

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| Grafana pide login distinto | `GRAFANA_ADMIN_PASSWORD` vacío o cambiado | Definir en `.env.local` y `observability-reset.ps1`. Si el volumen `grafana-data` persiste una contraseña antigua, recrear con `docker compose rm -sf grafana && docker volume rm asistente_grafana-data` |
| Prometheus no arranca (error de reglas) | `alerts.yml` con expresión inválida | Usar `promtool check rules monitoring/prometheus/alerts.yml`; evitar `== 0` entre escalares (usar `absent(...)`) |
| Tempo `unhealthy` | Healthcheck usaba `wget` vía shell, pero la imagen es distroless | Healthcheck ya usa `CMD ["/busybox/wget", ...]` (sin shell) |
| Alloy `unhealthy` | Healthcheck usaba `wget` inexistente en la imagen | Healthcheck ya usa `bash -ec 'exec 3<>/dev/tcp/...'` (bash está disponible) |
| Prometheus muestra backend-java DOWN | Backend sin perfil `observability` o caído | `docker compose logs backend-java`, revisar `/actuator/prometheus` |
| Sin trazas en Tempo | Backend arrancó sin tracing o Tempo no listo | `observability-verify.ps1` (chequea `TempoSinTrazas`); reiniciar backend |
| Sin logs JSON en Loki | Alloy no reenvía, o backend sin perfil `observability` | `docker compose logs alloy`; `docker compose restart backend-java` |
| Dashboards vacíos | Sin tráfico reciente | Simular mensajes en `/admin/whatsapp-simulator` y esperar 1 min |

## Verificación E2E realizada

Con el stack arriba y el perfil `observability` activo, se validó el flujo completo:

- 3 mensajes simulados (`POST /api/v1/test/whatsapp-inbound` con JWT de `admin@demo.cl`) → `assistente_whatsapp_mensajes_recibidos_total` 0 → 3 y `assistente_whatsapp_webhooks_recibidos_total` 0 → 3.
- Trazas en Tempo con spans `tarea-programada.*` (tareas programadas) y spans HTTP con `X-Correlation-Id` + `traceparent`.
- Logs JSON en Loki con `mdc.traceId/spanId/correlationId` (el `traceId` de Loki coincide con el de Tempo).
- `client-errors`: sin `Origin` → `403 SOURCE_NOT_ALLOWED`; con `Origin` válido → `202 ACCEPTED` y línea `APP_CLIENT_ERROR` sanearizada en Loki (mensaje/stack truncados, sin secretos).
- Capturas de los 6 dashboards en `docs/observabilidad-capturas/` (generadas con `scripts/grafana-captures.mjs`).

## Documentación relacionada

- `README-LOCAL.md` — entorno local completo (modos, secretos, túneles)
- `DEVELOPMENT.md` — perfiles Maven, pruebas, convenciones
- `CHANGELOG.md` — historial de versiones
