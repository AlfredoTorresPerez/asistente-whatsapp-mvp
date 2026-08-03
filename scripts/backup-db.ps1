# =============================================================================
# BACKUP DB - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\backup-db.ps1 [[-OutputDir] <string>] [[-Format] custom|plain]
# Ejemplo: .\scripts\backup-db.ps1 -OutputDir C:\backups
#          .\scripts\backup-db.ps1 -Format plain -RetentionDays 15
#          .\scripts\backup-db.ps1 -Simulation
#
# Genera por respaldo:
#   asistente_whatsapp_<timestamp>.dump | .sql.gz
#   asistente_whatsapp_<timestamp>.dump.sha256     (suma SHA-256)
#   asistente_whatsapp_<timestamp>.dump.metadata.json (metadatos sanitizados)
#   metrics (texto Prometheus, formato exportable por backup-exporter)
#
# Formato por defecto: custom (pg_dump -Fc -Z 5). Usa pg_dump del contenedor
# postgres del stack local (versiones consistentes, sin binarios en el host).
# =============================================================================
param(
  [string]$OutputDir = ".\backups",
  [ValidateSet("custom", "plain")][string]$Format = "custom",
  [int]$RetentionDays = 7,
  [switch]$Simulation
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "docker-compose.local.yml"
$EnvFile = Join-Path $Root ".env.local"
$PgContainer = "asistente-postgres"

if ($Simulation) {
  Write-Host "SIMULACION (sin ejecutar nada)" -ForegroundColor Cyan
  Write-Host "  pg_dump -h localhost -U <user> -d <db> -Fc -Z 5 --no-owner --no-acl -f /tmp/<ts>.dump" -ForegroundColor DarkGray
  Write-Host "  -> asistente_whatsapp_<ts>.dump + .sha256 + .metadata.json + metrics" -ForegroundColor DarkGray
  Write-Host "  validacion: pg_restore -l (custom) / gzip -t (plain); retencion ${RetentionDays} dias" -ForegroundColor DarkGray
  exit 0
}

if (-not (Test-Path -LiteralPath $ComposeFile)) { Write-Error "No se encuentra $ComposeFile"; exit 1 }
if (-not (Test-Path -LiteralPath $EnvFile))    { Write-Error "No se encuentra $EnvFile"; exit 1 }

# Credenciales leidas del contenedor (no versionadas en el repo)
$pgUser = (docker exec $PgContainer printenv POSTGRES_USER 2>$null | Out-String).Trim()
$pgDb   = (docker exec $PgContainer printenv POSTGRES_DB 2>$null | Out-String).Trim()
$pgPass = (docker exec $PgContainer printenv POSTGRES_PASSWORD 2>$null | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($pgUser) -or [string]::IsNullOrWhiteSpace($pgDb)) {
  Write-Error "No se pudo leer POSTGRES_USER/POSTGRES_DB del contenedor $PgContainer. Revisa que el stack local este corriendo (local-start.ps1)."
  exit 1
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Ext = if ($Format -eq "plain") { "sql.gz" } else { "dump" }
$BaseName = "asistente_whatsapp_$Timestamp"
$Filename = "$BaseName.$Ext"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$DestFile = Join-Path $OutputDir $Filename

$started = Get-Date
Write-Host "Respaldo PostgreSQL (formato: $Format) -> $Filename ..." -ForegroundColor Cyan

# pg_dump dentro del contenedor (evita redireccion binaria del host)
$ContainerTmp = "/tmp/$Filename"
if ($Format -eq "plain") {
  $cmd = "pg_dump -h localhost -U '$pgUser' -d '$pgDb' --no-owner --no-acl | gzip -f - > '$ContainerTmp'"
} else {
  $cmd = "pg_dump -h localhost -U '$pgUser' -d '$pgDb' -Fc -Z 5 --no-owner --no-acl -f '$ContainerTmp'"
}
docker compose --env-file $EnvFile -f $ComposeFile exec -T -e "PGPASSWORD=$pgPass" postgres sh -c $cmd 2>$null
if ($LASTEXITCODE -ne 0) {
  docker compose --env-file $EnvFile -f $ComposeFile exec -T postgres rm -f "$ContainerTmp" 2>$null | Out-Null
  Write-Error "pg_dump fallo (exit=$LASTEXITCODE)"
  exit 1
}

# Traer el archivo al host y limpiar el temporal del contenedor
docker cp "${PgContainer}:${ContainerTmp}" $DestFile 2>$null
docker compose --env-file $EnvFile -f $ComposeFile exec -T postgres rm -f "$ContainerTmp" 2>$null | Out-Null
if (-not (Test-Path -LiteralPath $DestFile)) {
  Write-Error "No se pudo copiar el respaldo desde el contenedor"
  exit 1
}

# Validacion de integridad del artefacto
Write-Host "Validando integridad del respaldo..." -ForegroundColor DarkGray
$valid = $false
if ($Format -eq "plain") {
  try {
    $fs = [System.IO.File]::OpenRead($DestFile)
    $gz = [System.IO.Compression.GZipStream]::new($fs, [System.IO.Compression.CompressionMode]::Decompress)
    $buf = [byte[]]::new(8192)
    while ($gz.Read($buf, 0, $buf.Length) -gt 0) { }
    $gz.Dispose(); $fs.Dispose()
    $valid = $true
  } catch { $valid = $false }
} else {
  # pg_restore -l valida el archivador custom sin conectar a BD
  $InspectTmp = "/tmp/${BaseName}_inspect.dump"
  docker cp $DestFile "${PgContainer}:${InspectTmp}" 2>$null
  docker compose --env-file $EnvFile -f $ComposeFile exec -T postgres pg_restore -l $InspectTmp 2>$null | Out-Null
  $valid = ($LASTEXITCODE -eq 0)
  docker compose --env-file $EnvFile -f $ComposeFile exec -T postgres rm -f "$InspectTmp" 2>$null | Out-Null
}
if (-not $valid) {
  Write-Error "El respaldo no paso la validacion de integridad"
  exit 1
}

# Suma SHA-256 + metadatos sanitizados
$Hash = (Get-FileHash -LiteralPath $DestFile -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$DestFile.sha256" -Value "$Hash  $Filename" -Encoding ascii

$finished = Get-Date
$duration = [math]::Round(($finished - $started).TotalSeconds, 1)
$size = (Get-Item -LiteralPath $DestFile).Length

$metadata = [ordered]@{
  script      = "backup-db.ps1"
  simulated   = $false
  backup      = [ordered]@{
    file            = $Filename
    format          = $Format
    database        = $pgDb
    source_host     = $PgContainer
    size_bytes      = $size
    sha256          = $Hash
    sha256_ok       = 1
    duration_seconds = $duration
    started_at      = $started.ToString("o")
    finished_at     = $finished.ToString("o")
  }
  policy      = [ordered]@{ retention_days = $RetentionDays }
}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$DestFile.metadata.json" -Encoding utf8

# Archivo de metricas Prometheus (formato del sidecar para el exporter)
$metrics = @(
  "# HELP backup_sidecar_last_success_timestamp_seconds Ultimo respaldo exitoso (epoch)",
  "# TYPE backup_sidecar_last_success_timestamp_seconds gauge",
  "backup_sidecar_last_success_timestamp_seconds $([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())",
  "# HELP backup_sidecar_last_duration_seconds Duracion del ultimo respaldo",
  "# TYPE backup_sidecar_last_duration_seconds gauge",
  "backup_sidecar_last_duration_seconds $duration",
  "# HELP backup_sidecar_last_size_bytes Tamano del ultimo respaldo",
  "# TYPE backup_sidecar_last_size_bytes gauge",
  "backup_sidecar_last_size_bytes $size",
  "# HELP backup_sidecar_last_sha256_ok Suma SHA-256 verificada",
  "# TYPE backup_sidecar_last_sha256_ok gauge",
  "backup_sidecar_last_sha256_ok 1",
  "# HELP backup_sidecar_last_result Resultado (1 exito, 0 fallo)",
  "# TYPE backup_sidecar_last_result gauge",
  "backup_sidecar_last_result 1"
)
Set-Content -LiteralPath (Join-Path $OutputDir "metrics") -Value $metrics -Encoding ascii

# Politica de conservacion (por dias, sobre los artefactos de respaldo)
if ($RetentionDays -gt 0) {
  $cutoff = (Get-Date).AddDays(-$RetentionDays)
  $old = Get-ChildItem -LiteralPath $OutputDir -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "asistente_whatsapp_*" -and $_.LastWriteTime -lt $cutoff }
  foreach ($f in $old) { Remove-Item -LiteralPath $f.FullName -Force -ErrorAction SilentlyContinue }
  Write-Host "Retencion: $($old.Count) artefactos anteriores a $RetentionDays dias eliminados" -ForegroundColor DarkGray
}

Write-Host "Backup creado: $DestFile" -ForegroundColor Green
Write-Host "  sha256: $Hash" -ForegroundColor DarkGray
Write-Host "  duracion: ${duration}s | tamano: $size bytes" -ForegroundColor DarkGray
exit 0
