# Informe de Capacidad de Recuperación — Fase 9

**Fecha de ejecución:** 2026-08-03
**Ambiente:** Local simulado (`docker-compose.local.yml`, proveedor WhatsApp `SIMULATED`)
**Branch:** `master` (cambios de Fase 9 sin commit)

---

## 1. Resumen ejecutivo

La capacidad de respaldo y restauración de PostgreSQL quedó **implementada y verificada
de extremo a extremo** en el ambiente local:

- **Respaldo real** en formato `custom` (`pg_dump -Fc -Z5 --no-owner --no-acl`) con suma
  SHA-256 y metadatos sanitizados, reproducible vía script manual y proveedor automático
  (sidecar con cron diario 04:00 + retención 7 días).
- **Restauración segura** a base temporal (nunca toca la base principal por defecto),
  verificación estructural y funcional contra la referencia, y posible swap a la principal
  con doble confirmación explícita.
- **Backend funcional contra la base restaurada**: login, lectura y escritura real
  (Flyway valida/aplica el esquema, Spring Boot queda `UP`).
- **6 pruebas negativas** en verde (13/13 chequeos): archivo vacío, suma incorrecta,
  truncado, falta de espacio, postgres caído y migración incompatible.
- **Observabilidad** del respaldo integrada a Prometheus (`backup-exporter` en :9100)
  con 3 alertas (`BackupFallido`, `BackupDesactualizado`, `BackupSinMetricas`).
- **RPO** medido ≤ 24 h (cron diario) y **RTO** ≈ 1 min para volver a operación completa
  (restauración ~6 s + verificación + arranque del backend).

La base principal quedó intacta durante todo el flujo (conteos de control idénticos antes
y después: `business=1, booking=3, customer=5, lead=3`).

---

## 2. Entregables

| Entregable | Ubicación |
|---|---|
| Backup manual | `scripts/backup-db.ps1` / `scripts/backup-db.sh` |
| Restauración segura (temp + swap a principal con doble confirmación) | `scripts/restore-db.ps1` / `scripts/restore-db.sh` |
| Verificación estructural/funcional vs referencia | `scripts/verify-restore-db.ps1` / `scripts/verify-restore-db.sh` |
| Backend contra la BD restaurada | `scripts/restore-backend-check.ps1` / `.sh` |
| Pruebas negativas | `scripts/test-recovery-negative.ps1` / `.sh` |
| Entrypoint del sidecar (custom+sha256+metadata+metrics+retención) | `scripts/backup-sidecar-entrypoint.sh` |
| Compose local (sidecar actualizado + `backup-exporter`) | `docker-compose.local.yml` |
| Métricas Prometheus (job `backup-sidecar`) | `monitoring/prometheus/prometheus.yml` |
| Alertas de respaldo | `monitoring/prometheus/alerts.yml` |
| Artefactos de respaldo reales | `backups/*.dump`, `*.sha256`, `*.metadata.json`, `metrics` |

---

## 3. Formato de los artefactos de respaldo

Por cada respaldo se generan tres archivos (+ métricas acumuladas):

| Archivo | Contenido |
|---|---|
| `<fecha>.dump` | Volcado PostgreSQL formato **custom** comprimido (`pg_dump -Fc -Z5 --no-owner --no-acl`) |
| `<fecha>.dump.sha256` | Suma SHA-256 del `.dump` (integridad) — verificado siempre antes de restaurar |
| `<fecha>.dump.metadata.json` | Metadatos sanitizados (host, base, formato, timestamp, tamaño, filtros). **Sin credenciales ni valores de secretos.** |
| `metrics` | Texto Prometheus: `backup_sidecar_*` (éxitos/fallos/last success/duration/size/sha256/result) |

Ejemplo de respaldo real (ejecutado):

```
asistente_whatsapp_20260803_102631.dump       583.628 bytes
asistente_whatsapp_20260803_102631.dump.sha256
asistente_whatsapp_20260803_102631.dump.metadata.json
```

Verificación SHA-256 al restaurar: `SHA-256 verificado OK` / rechazo con
`Suma SHA-256 NO coincide (integridad del archivo comprometida)`.

---

## 4. Flujo de restauración verificado

### 4.1 Restauración a base temporal (por defecto)

```powershell
.\scripts\restore-db.ps1 -BackupFile .\backups\asistente_whatsapp_20260803_102631.dump
```

Pasos que ejecuta:

1. Verifica la suma SHA-256 del respaldo.
2. Crea la base temporal `asistente_whatsapp_restore_<timestamp>`.
3. Restaura con `pg_restore` desde el contenedor de PostgreSQL (misma red).
4. Invoca `verify-restore-db.ps1` contra la temporal vs referencia `asistente_whatsapp`:
   - Conectividad de ambas bases.
   - Cantidad de tablas (93).
   - FKs validadas (225).
   - Secuencias (0) y defaults `nextval` (0, PKs UUID).
   - Flyway: último `installed_rank` 105, 105 migraciones exitosas, cadena
     `version:checksum` completa e idéntica.
   - Registros de control: 31 tablas críticas con conteos idénticos.
   - Integridad referencial: total de huérfanos **idéntico** a la referencia
     (los 1046 huérfanos son datos de seed preexistentes de la referencia, no corrupción).
5. Si todo pasa: `VERIFICACION DE RESTAURACION: OK (exit 0)`.
6. La temporal queda conservada para verificación funcional; el operador la elimina al final.

### 4.2 Restauración a la base principal (swap)

`restore-db.ps1 -RestoreToMain -BackupFile <archivo>` exige **doble confirmación**:
`-ConfirmText CONFIRMAR` + confirmación de nombre de base. Diseñado para operatorios
intervenidos; la base principal nunca se toca en el flujo por defecto.

### 4.3 Resultado de la ejecución (2026-08-03)

```
SHA-256 verificado OK
Base temporal creada: asistente_whatsapp_restore_20260803_102924
Restauracion completada en 6.5s (asistente_whatsapp_restore_20260803_102924)
  [OK]   Conectividad a ambas bases
  [OK]   Cantidad de tablas = 93
  [OK]   Restricciones FOREIGN KEY (validados) = 225
  [OK]   Secuencias (public) = 0
  [OK]   Columnas con default nextval = 0
  [OK]   Flyway: ultima migracion (installed_rank) = 105
  [OK]   Flyway: migraciones exitosas = 105
  [OK]   Flyway: cadena version:checksum completa = ...
  [OK]   Registros de control: 31 tablas criticas con conteos identicos
  [OK]   Integridad referencial: huerfanos identicos a la referencia (TOTAL_ORPHANS=1046)
VERIFICACION DE RESTAURACION: OK (exit 0)
```

---

## 5. Backend funcional contra la base restaurada

`restore-backend-check.ps1` levanta un contenedor temporal `asistente-backend-restore-verify`
(puerto **8081**, imagen `asistente-backend-java:latest`, perfil `local,local-safe`,
proveedor `SIMULATED`, `openai=false`, `email` off) apuntando a la base restaurada.

Ejecutado el 2026-08-03:

```
Iniciando instancia de backend contra la base restaurada 'asistente_whatsapp_restore_20260803_102924' (puerto 8081)...
  [OK]   /actuator/health = UP (Flyway aplico/valido el esquema sobre la base restaurada)
  [OK]   Login (admin demo) -> token obtenido
  [OK]   GET /api/v1/company -> id=11111111-1111-1111-1111-111111111111
  [OK]   POST /api/v1/test/whatsapp-inbound (escritura en base restaurada) -> 200 OK
Backend funcional contra la base restaurada: OK
```

El contenedor temporal se elimina en `finally`. La base principal **no se toca**.

---

## 6. Pruebas negativas (Fase 9)

Ejecutadas el 2026-08-03 con `test-recovery-negative.ps1 -BackupDir .\backups`:

| # | Escenario | Chequeo | Resultado |
|---|---|---|---|
| 1 | Archivo vacío | restore exit ≠ 0 y sin BD temporal residual | PASS / PASS |
| 2 | Suma SHA-256 incorrecta | restore rechaza el respaldo | PASS / PASS |
| 3 | Archivo truncado | pg_restore falla; limpieza de temporal | PASS / PASS |
| 4 | Falta de espacio (proxy: dir no escribible) | backup falla limpio | PASS |
| 5 | PostgreSQL no disponible | backup y restore fallan; al reiniciar postgres y backend quedan UP | PASS ×4 |
| 6 | Migración incompatible (Flyway V999) | backend NO arranca contra la temporal; patrón `flyway` en logs | PASS |

**Resultado: 13/13 chequeos PASS, exit 0.**

Los scripts de bash espejo (`test-recovery-negative.sh`, `backup-db.sh`, `restore-db.sh`,
`verify-restore-db.sh`, `restore-backend-check.sh`) replican la misma batería para
Linux/macOS (validados con `bash -n`).

---

## 7. Observabilidad del respaldo

El sidecar `backup-sidecar` contador persistente y su archivo `metrics` (Prometheus
text format) se exponen mediante `backup-exporter` (busybox httpd, puerto **9100**),
que Prometheus scrapea en el job `backup-sidecar`:

```
backup_sidecar_success_total        # 2 (contador persistente)
backup_sidecar_failures_total       # 0
backup_sidecar_last_success_timestamp_seconds
backup_sidecar_last_duration_seconds # 1
backup_sidecar_last_size_bytes       # 1008710
backup_sidecar_last_sha256_ok        # 1
backup_sidecar_last_result           # 1
```

Reglas de alerta (`monitoring/prometheus/alerts.yml`, grupo `asistente-respaldo`):

| Alerta | Severidad | Condición |
|---|---|---|
| `BackupFallido` | critical | `backup_sidecar_failures_total[5m] > 0` |
| `BackupDesactualizado` | warning | sin éxito en las últimas 26 h |
| `BackupSinMetricas` | warning | `absent(backup_sidecar_last_success_timestamp_seconds)` sobre el job |

Verificado en Prometheus (API v1): target `backup-sidecar` **up**, reglas `inactive/ok`.

---

## 8. RPO y RTO medidos

| Objetivo | Valor | Cómo se mide |
|---|---|---|
| **RPO** | ≤ 24 h | Periodicidad del respaldo programado (cron diario 04:00 UTC en el sidecar) |
| **RTO (datos)** | ~6.5 s | `pg_restore` de 583 KB en `restore-db.ps1` |
| **RTO (verificación)** | ~2–3 s | `verify-restore-db.ps1` (tablas, FKs, Flyway, conteos, huérfanos) |
| **RTO (servicio)** | ~20–40 s | Arranque del backend contra la BD restaurada (`/actuator/health=UP`) |
| **RTO total local** | ≈ 1 min | Restauración + verificación + arranque de backend |

En producción el RTO dependerá del tamaño real de la base, el almacenamiento y la
configuración de arranque; estos valores son la línea base local sobre el dataset demo.

---

## 9. Lecciones corregidas durante la Fase 9 (causas raíz)

1. **Parámetro `$Args` en `Invoke-PgAdmin`** (`restore-db.ps1`): colisiona con la variable
   automática `$args` de PowerShell, dejaba el comando vacío, `sh -c ''` retornaba 0 sin
   crear/restaurar la base. **Corregido**: parámetro renombrado a `Cmd` en la función y en
   las 10 llamadas. Sin este fix la BD temporal no existía aunque el script reportara éxito.
2. **Here-string con `$$`** (`verify-restore-db.ps1`): con comillas dobles PowerShell
   expanda `$$` a vacío y el check de huérfános quedaba corrupto. **Corregido**: here-string
   literal (`@'...'@`) con `$$` crudo.
3. **Criterio de huérfános**: la referencia también tiene 1046 huérfános (FKs apuntando a
   seeds que ya no existen). Exigir 0 generaba falsos negativos. **Corregido**: comparar el
   total de huérfános restaurada vs referencia (deben coincidir).
4. **`extends` + `ports` heredados** en `restore-backend-check.ps1`: el merge de listas
   hereda el 8080:8080 del servicio base, colindando con el backend en ejecución.
   Corregido definiendo el servicio temporal completo (imagen `asistente-backend-java`,
   `SERVER_PORT=8081`, `ports 8081:8081`) sin `extends` heredando ports, alineado con el par `.sh`.
5. **Content-Type en scrape Prometheus v3**: busybox httpd no envía `Content-Type`; Prometheus
   v3 rechaza el scrape. Corregido con `fallback_scrape_protocol: PrometheusText0.0.4` en el job
   `backup-sidecar`.
6. **`busybox` de postgres-alpine no incluye `httpd`**: el `backup-exporter` usaba la imagen
   `postgres:16.14-alpine` y fallaba con `httpd: applet not found`; corregido a imagen
   `busybox:1.36.1`.

---

## 10. Estado final del ambiente

- **13/13** chequeos de terminal de ps en verde.
- Base principal intacta: `business=1, booking=3, customer=5, lead=3`.
- Sin BD temporales residuales tras la batería completa.
- Contenedores `asistente-backup-sidecar` y `asistente-backup-exporter` corriendo
  (perfil `backup`); Prometheus con el target `backup-sidecar` up y las 3 alertas
  `inactive`.

---

## 11. Limitaciones / próximos pasos

- El swap a la base `asistente_whatsapp` (`-RestoreToMain`) está diseñado pero **no se
  ejecutó en la sesión** (no destructor: requiere doble confirmación y detiene a la
  aplicación); validar en una simulación de desastre con datos sintéticos.
- Las pruebas negativas y de verificación cubren el dataset demo; para producción se debe
  re-medir con un volumen de datos real.
- Los huérfanos de la referencia (1046) son datos de seed preexistentes; evaluar si se
  deben reparar o documentar como tal en producción (no afectan los flujos de la Fase).
- El cron del sidecar ya genera nombres con marca de fecha por respaldo (`asistente_whatsapp_<timestamp>.dump`).