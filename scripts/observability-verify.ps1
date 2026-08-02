#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Verifica el estado del stack de observabilidad local.
.DESCRIPTION
  Comprueba:
    - Contenedores Docker saludables (prometheus, loki, tempo, alloy, grafana)
    - Backend /actuator/health = UP (perfil observability)
    - Prometheus: targets de scrape activos
    - Loki: endpoint /ready
    - Tempo: endpoint /ready y trazas recibidas (alerta TempoSinTrazas)
    - Grafana: /api/health y dashboards instalados
.PARAMETER TimeoutSeconds
  Timeout por cada healthcheck. Default: 15s
.EXAMPLE
  .\scripts\observability-verify.ps1
#>

param(
  [int]$TimeoutSeconds = 15
)

$failed = $false

function Write-Step { param([string]$Msg) Write-Host "`n$Msg" -ForegroundColor Cyan }
function Write-OK   { param([string]$Msg) Write-Host "  [OK] $Msg" -ForegroundColor Green }
function Write-Fail { param([string]$Msg) Write-Host "  [FAIL] $Msg" -ForegroundColor Red; $script:failed = $true }

Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  VERIFICACION DE OBSERVABILIDAD LOCAL    ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Cyan

# ── 1. Contenedores Docker ──────────────────────────────────
Write-Step "1/6 - Verificando contenedores Docker..."

$containers = @("asistente-prometheus", "asistente-loki", "asistente-tempo", "asistente-alloy", "asistente-grafana")
foreach ($name in $containers) {
  $inspect = docker inspect --format='{{.State.Health.Status}}' $name 2>$null
  if ($LASTEXITCODE -ne 0) {
    Write-Fail "Contenedor $name no existe o no esta corriendo"
    continue
  }
  if ($inspect -eq "healthy") {
    Write-OK "$name = healthy"
  } elseif ($inspect -eq "starting") {
    Write-Fail "$name = starting (aun no listo)"
  } else {
    Write-Fail "$name = $inspect"
  }
}

# ── 2. Backend + metricas ────────────────────────────────────
Write-Step "2/6 - Verificando backend con perfil observability..."

try {
  $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec $TimeoutSeconds
  if ($health.status -eq "UP") {
    Write-OK "Backend /actuator/health = UP"
  } else {
    Write-Fail "Backend health = $($health.status)"
  }
} catch {
  Write-Fail "Backend health endpoint no responde: $_"
}

try {
  $metrics = Invoke-WebRequest -Uri "http://localhost:8080/actuator/prometheus" -Method Get -TimeoutSec $TimeoutSeconds -UseBasicParsing
  if ($metrics.StatusCode -eq 200 -and $metrics.Content -match "assistente_whatsapp_mensajes_recibidos_total") {
    Write-OK "Metricas funcionales asistente_* expuestas"
  } elseif ($metrics.StatusCode -eq 200) {
    Write-Fail "Metrics expuestas pero sin metricas asistente_* (revisa instrumentacion)"
  } else {
    Write-Fail "Prometheus endpoint HTTP $($metrics.StatusCode)"
  }
} catch {
  Write-Fail "Prometheus endpoint no responde: $_"
}

# ── 3. Prometheus targets ────────────────────────────────────
Write-Step "3/6 - Verificando Prometheus..."

try {
  $targets = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/targets" -Method Get -TimeoutSec $TimeoutSeconds
  $downTargets = @($targets.data.activeTargets | Where-Object { $_.health -ne "up" })
  if ($downTargets.Count -eq 0) {
    Write-OK "Todos los targets de scrape estan UP ($(@($targets.data.activeTargets).Count) activos)"
  } else {
    foreach ($t in $downTargets) { Write-Fail "Target $($t.scrapeUrl) = $($t.health)" }
  }
} catch {
  Write-Fail "Prometheus /api/v1/targets no responde: $_"
}

# ── 4. Loki ──────────────────────────────────────────────────
Write-Step "4/6 - Verificando Loki..."

try {
  $ready = Invoke-WebRequest -Uri "http://localhost:3100/ready" -Method Get -TimeoutSec $TimeoutSeconds -UseBasicParsing
  if ($ready.StatusCode -eq 200) {
    Write-OK "Loki /ready = 200"
  } else {
    Write-Fail "Loki /ready = $($ready.StatusCode)"
  }
} catch {
  Write-Fail "Loki no responde: $_"
}

# ── 5. Tempo (alerta TempoSinTrazas) ─────────────────────────
Write-Step "5/6 - Verificando Tempo..."

try {
  $ready = Invoke-WebRequest -Uri "http://localhost:3200/ready" -Method Get -TimeoutSec $TimeoutSeconds -UseBasicParsing
  if ($ready.StatusCode -eq 200) {
    Write-OK "Tempo /ready = 200"
  } else {
    Write-Fail "Tempo /ready = $($ready.StatusCode)"
  }
} catch {
  Write-Fail "Tempo no responde: $_"
}

try {
  $query = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/query?query=tempo_request_duration_seconds_count" -Method Get -TimeoutSec $TimeoutSeconds
  $samples = @($query.data.result)
  if ($samples.Count -gt 0) {
    $total = ($samples | ForEach-Object { [double]$_.value[1] } | Measure-Object -Sum).Sum
    Write-OK "Tempo ha recibido trazas ($total spans/series)"
  } else {
    Write-Fail "Tempo sin trazas recibidas (alerta TempoSinTrazas se activaria). Genera trafico e intentalo de nuevo."
  }
} catch {
  Write-Fail "Consulta de trazas a Prometheus fallo: $_"
}

# ── 6. Grafana ───────────────────────────────────────────────
Write-Step "6/6 - Verificando Grafana..."

try {
  $health = Invoke-RestMethod -Uri "http://localhost:3000/api/health" -Method Get -TimeoutSec $TimeoutSeconds
  if ($health.database -eq "ok") {
    Write-OK "Grafana /api/health = ok"
  } else {
    Write-Fail "Grafana health = $($health | ConvertTo-Json -Compress)"
  }
} catch {
  Write-Fail "Grafana no responde: $_"
}

try {
  $envFile = Join-Path $PSScriptRoot "..\.env.local"
  $grafanaPassword = if (Test-Path $envFile) {
    ((Get-Content $envFile | Where-Object { $_ -match '^GRAFANA_ADMIN_PASSWORD=' }) -replace '^GRAFANA_ADMIN_PASSWORD=', '').Trim()
  } else { "" }
  if ([string]::IsNullOrWhiteSpace($grafanaPassword)) { throw "GRAFANA_ADMIN_PASSWORD no definido en .env.local" }
  $dashboards = Invoke-RestMethod -Uri "http://localhost:3000/api/search?type=dash-db" -Method Get -Headers @{ Authorization = "Basic $([Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin:$grafanaPassword")))" } -TimeoutSec $TimeoutSeconds
  $asistenteDashboards = @($dashboards | Where-Object { $_.uid -match "^asistente-" })
  if ($asistenteDashboards.Count -ge 6) {
    Write-OK "Dashboards instalados ($($asistenteDashboards.Count) de asistente)"
  } elseif ($asistenteDashboards.Count -gt 0) {
    Write-Fail "Dashboards parciales ($($asistenteDashboards.Count)/6). Revisa provisioning en monitoring/grafana/"
  } else {
    Write-Fail "Dashboards no instalados. Revisa provisioning en monitoring/grafana/"
  }
} catch {
  Write-Fail "No se pudo listar dashboards en Grafana: $_"
}

# ── Resumen ─────────────────────────────────────────────────
Write-Host ""
if ($failed) {
  Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Yellow
  Write-Host "║  VERIFICACION COMPLETADA CON FALLOS      ║" -ForegroundColor Yellow
  Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Yellow
  exit 1
} else {
  Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
  Write-Host "║  VERIFICACION COMPLETADA: TODO OK        ║" -ForegroundColor Green
  Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
  exit 0
}
