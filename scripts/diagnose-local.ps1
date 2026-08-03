# =============================================================================
# diagnose-local.ps1 - Diagnostico del ambiente local de desarrollo
#
# Verifica toolchain, recursos, puertos, configuracion, secretos (solo
# presencia y longitud, nunca valores), estado del stack Docker y estado
# del frontend. Produce un reporte sanitizado compartible con -OutFile.
#
# Uso:
#   pwsh scripts/diagnose-local.ps1
#   pwsh scripts/diagnose-local.ps1 -OutFile local-diagnostics.txt
#
# Exit code: 0 = sin errores criticos; 1 = existen errores criticos.
# =============================================================================

param(
  [string]$OutFile = ""
)

$ErrorActionPreference = "Continue"

$ROOT = Split-Path -Parent $PSScriptRoot

$global:results = New-Object System.Collections.Generic.List[object]

function Add-Result {
  param([string]$Section, [string]$Status, [string]$Message, [string]$Action = "")
  $global:results.Add([pscustomobject]@{
    Section = $Section; Status = $Status; Message = $Message; Action = $Action
  })
}

function Write-OK   { param([string]$M) Write-Host "  [OK]   $M" -ForegroundColor Green }
function Write-Warn { param([string]$M) Write-Host "  [AVISO] $M" -ForegroundColor Yellow }
function Write-Fail { param([string]$M) Write-Host "  [ERROR] $M" -ForegroundColor Red }

function Get-CommandPath {
  param([string]$Name)
  $cmd = Get-Command $Name -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  return ""
}

# -----------------------------------------------------------------------------
# A. Toolchain
# -----------------------------------------------------------------------------
Write-Host "`n== A. Toolchain ====================================================="

$javaPath = Get-CommandPath "java"
if ($javaPath) {
  $javaVer = (& java -version 2>&1 | Select-Object -First 1)
  if ($javaVer -match '"(\d+)') {
    if ([int]$matches[1] -ge 21) {
      Write-OK "Java: $javaVer"
      Add-Result "Toolchain" "OK" "Java $($matches[1])+" ""
    } else {
      Write-Fail "Java version baja: $javaVer (se requiere 21+)"
      Add-Result "Toolchain" "FAIL" "Java $($matches[1]) detectado, se requiere 21+" "Instalar JDK 21 (Temurin) y verificar con java -version"
    }
  } else {
    Write-Fail "No se pudo interpretar la version de Java"
    Add-Result "Toolchain" "FAIL" "Version Java no interpretable" "Verificar instalacion de JDK 21"
  }
} else {
  Write-Fail "Java no encontrado en el PATH"
  Add-Result "Toolchain" "FAIL" "Java ausente" "Instalar JDK 21 (Temurin) y agregarlo al PATH"
}

$mvnwExists = Test-Path (Join-Path $ROOT "backend-java\mvnw")
if ($mvnwExists) {
  Write-OK "Wrapper Maven presente (backend-java/mvnw)"
  Add-Result "Toolchain" "OK" "mvnw presente" ""
} else {
  Write-Fail "Falta backend-java/mvnw"
  Add-Result "Toolchain" "FAIL" "mvnw ausente" "Reconstruir el repositorio o restaurar el wrapper Maven"
}

$mvnPath = Get-CommandPath "mvn"
if ($mvnPath) { Write-OK "Maven global: $mvnPath" }
else {
  Write-Warn "mvn global no encontrado (el wrapper mvnw es suficiente)"
  Add-Result "Toolchain" "WARN" "mvn global ausente (opcional)" "Usar backend-java/mvnw, no requiere Maven global"
}

$nodePath = Get-CommandPath "node"
if ($nodePath) {
  $nodeVer = (& node --version 2>&1 | Select-Object -First 1)
  if ($nodeVer -match '^v(\d+)\.(\d+)') {
    $major = [int]$matches[1]; $minor = [int]$matches[2]
    if ($major -gt 20 -or ($major -eq 20 -and $minor -ge 19)) {
      Write-OK "Node: $nodeVer"
      Add-Result "Toolchain" "OK" "Node $nodeVer" ""
    } else {
      Write-Fail "Node version baja: $nodeVer (se requiere 20.19+)"
      Add-Result "Toolchain" "FAIL" "Node $nodeVer, se requiere 20.19+" "Instalar Node 20.19+ (nvm install 20.19.0; ver frontend-react/.nvmrc)"
    }
  } else {
    Write-Fail "No se pudo interpretar la version de Node"
    Add-Result "Toolchain" "FAIL" "Version Node no interpretable" "Verificar instalacion de Node 20.19+"
  }
} else {
  Write-Fail "Node no encontrado en el PATH"
  Add-Result "Toolchain" "FAIL" "Node ausente" "Instalar Node 20.19+ (ver frontend-react/.nvmrc)"
}

$pnpmPath = Get-CommandPath "pnpm"
if ($pnpmPath) {
  $pnpmVer = (& pnpm --version 2>&1 | Select-Object -First 1)
  if ($pnpmVer -match '^(\d+)') {
    if ([int]$matches[1] -eq 10) {
      Write-OK "pnpm: $pnpmVer"
      Add-Result "Toolchain" "OK" "pnpm $pnpmVer" ""
    } else {
      Write-Fail "pnpm version incorrecta: $pnpmVer (se requiere 10.x)"
      Add-Result "Toolchain" "FAIL" "pnpm $pnpmVer, se requiere 10.x" "corepack prepare pnpm@10.18.3 --activate"
    }
  } else {
    Write-Fail "No se pudo interpretar la version de pnpm"
    Add-Result "Toolchain" "FAIL" "Version pnpm no interpretable" "Verificar instalacion de pnpm 10.x"
  }
} else {
  Write-Fail "pnpm no encontrado en el PATH"
  Add-Result "Toolchain" "FAIL" "pnpm ausente" "corepack enable && corepack prepare pnpm@10.18.3 --activate"
}

$dockerPath = Get-CommandPath "docker"
if ($dockerPath) {
  $dockerVer = (& docker --version 2>&1 | Select-Object -First 1)
  $serverOk = $false
  if ($dockerVer -match '(\d+)\.(\d+)\.(\d+)') {
    if ([int]$matches[1] -ge 20) { $serverOk = $true }
  }
  if ($serverOk) {
    Write-OK "Docker CLI: $dockerVer"
    Add-Result "Toolchain" "OK" "Docker CLI $dockerVer" ""
  } else {
    Write-Warn "Docker CLI: $dockerVer"
    Add-Result "Toolchain" "WARN" "Docker CLI $dockerVer" "Se recomienda Docker Desktop 20+"
  }
  $composeOut = & docker compose version 2>&1
  $composeVer = ($composeOut | Out-String).Trim()
  if ($LASTEXITCODE -eq 0 -and $composeVer -match 'v\d+\.\d+') {
    Write-OK "Docker Compose: $composeVer"
    Add-Result "Toolchain" "OK" "Compose $composeVer" ""
  } else {
    Write-Fail "Docker Compose plugin no disponible"
    Add-Result "Toolchain" "FAIL" "Compose v2 ausente" "Actualizar Docker Desktop (incluye el plugin compose)"
  }
} else {
  Write-Fail "Docker no encontrado en el PATH"
  Add-Result "Toolchain" "FAIL" "Docker ausente" "Instalar Docker Desktop y verificar docker --version"
}

# -----------------------------------------------------------------------------
# B. Recursos del sistema
# -----------------------------------------------------------------------------
Write-Host "`n== B. Recursos del sistema ==========================================="

try {
  $os = Get-CimInstance Win32_OperatingSystem
  $totalGB = [math]::Round($os.TotalVisibleMemorySize / 1MB, 1)
  $freeGB = [math]::Round($os.FreePhysicalMemory / 1MB, 1)
  Write-Host "  RAM total: $totalGB GB | libre: $freeGB GB"
  if ($totalGB -lt 4) {
    Write-Fail "RAM insuficiente ($totalGB GB): el stack completo requiere 8+ GB"
    Add-Result "Recursos" "FAIL" "RAM $totalGB GB" "Cerrar otras aplicaciones o ampliar RAM a 8+ GB"
  } elseif ($totalGB -lt 8) {
    Write-Warn "RAM reducida ($totalGB GB): recomendado 8+ GB para el stack completo"
    Add-Result "Recursos" "WARN" "RAM $totalGB GB" "Cerrar otras aplicaciones para liberar memoria"
  } else {
    Write-OK "RAM suficiente: $totalGB GB"
    Add-Result "Recursos" "OK" "RAM $totalGB GB" ""
  }
} catch {
  Write-Warn "No se pudo consultar la RAM"
  Add-Result "Recursos" "WARN" "RAM no consultable" ""
}

try {
  $drive = Get-PSDrive -Name $ROOT.Substring(0, 1)
  $freeGBDisk = [math]::Round($drive.Free / 1GB, 1)
  Write-Host "  Disco libre en $($drive.Name): $freeGBDisk GB"
  if ($freeGBDisk -lt 5) {
    Write-Fail "Disco libre muy bajo ($freeGBDisk GB)"
    Add-Result "Recursos" "FAIL" "Disco libre $freeGBDisk GB" "Liberar espacio en disco (recomendado 10+ GB)"
  } elseif ($freeGBDisk -lt 10) {
    Write-Warn "Disco libre reducido ($freeGBDisk GB): recomendado 10+ GB"
    Add-Result "Recursos" "WARN" "Disco libre $freeGBDisk GB" "Liberar espacio en disco"
  } else {
    Write-OK "Disco libre suficiente: $freeGBDisk GB"
    Add-Result "Recursos" "OK" "Disco libre $freeGBDisk GB" ""
  }
} catch {
  Write-Warn "No se pudo consultar el disco"
  Add-Result "Recursos" "WARN" "Disco no consultable" ""
}

# -----------------------------------------------------------------------------
# C. Puertos en uso (informativos)
# -----------------------------------------------------------------------------
Write-Host "`n== C. Puertos del stack local ========================================"

$ports = @{
  "5433" = "PostgreSQL (postgres)"; "8080" = "Backend (backend-java)";
  "5173" = "Frontend (vite)"; "8025" = "MailHog";
  "3000" = "Tunel publico / public-link"; "9090" = "Prometheus";
  "3100" = "Loki"; "3200" = "Tempo"; "3001" = "Grafana"
}
foreach ($port in $ports.Keys | Sort-Object { [int]$_ }) {
  try {
    $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
      $procName = ""
      try {
        $proc = Get-Process -Id $conn[0].OwningProcess -ErrorAction SilentlyContinue
        if ($proc) { $procName = $proc.ProcessName }
      } catch {}
      Write-Host "  Puerto $port ocupado ($($ports[$port])): PID $($conn[0].OwningProcess) - $procName"
    } else {
      Write-Host "  Puerto $port libre ($($ports[$port]))"
    }
  } catch {
    Write-Host "  Puerto ${port}: no consultable"
  }
}
Add-Result "Puertos" "INFO" "Puertos listados (5433/8080/5173/8025/3000/9090/3100/3200/3001)" ""

# -----------------------------------------------------------------------------
# D. Archivos de configuracion
# -----------------------------------------------------------------------------
Write-Host "`n== D. Configuracion del ambiente ===================================="

$composeLocal = Test-Path (Join-Path $ROOT "docker-compose.local.yml")
if ($composeLocal) { Write-OK "docker-compose.local.yml presente" }
else {
  Write-Fail "Falta docker-compose.local.yml"
  Add-Result "Configuracion" "FAIL" "docker-compose.local.yml ausente" "Reconstruir el repositorio"
}
Add-Result "Configuracion" $(if ($composeLocal) { "OK" } else { "FAIL" }) "docker-compose.local.yml" ""

$envLocal = Join-Path $ROOT ".env.local"
if (Test-Path $envLocal) {
  $keys = @()
  try {
    Get-Content $envLocal | ForEach-Object {
      if ($_ -match '^\s*([A-Za-z0-9_]+)\s*=') { $keys += $matches[1] }
    }
  } catch {}
  Write-OK ".env.local presente ($($keys.Count) variables definidas)"
  Write-Host "  Variables definidas (solo nombres, sin valores): $($keys -join ', ')"
  if ($keys -notcontains "APP_JWT_SECRET") {
    Write-Warn "APP_JWT_SECRET no esta definido en .env.local"
    Add-Result "Configuracion" "WARN" "APP_JWT_SECRET ausente en .env.local" "Copiar .env.local.template a .env.local y definir APP_JWT_SECRET"
  }
  Add-Result "Configuracion" "OK" ".env.local presente, $($keys.Count) variables" ""
} else {
  Write-Fail "Falta .env.local (no se puede levantar el stack)"
  Add-Result "Configuracion" "FAIL" ".env.local ausente" "Copiar .env.local.template a .env.local y definir JWT_SECRET (ver QUICKSTART_15_MIN.md)"
}

$template = Test-Path (Join-Path $ROOT ".env.local.template")
if ($template) { Write-OK ".env.local.template presente" }
else {
  Write-Warn "Falta .env.local.template"
  Add-Result "Configuracion" "WARN" ".env.local.template ausente" "Restaurar la plantilla desde el repositorio"
}

$caddy = Test-Path (Join-Path $ROOT "caddy\Caddyfile.local")
if ($caddy) { Write-OK "caddy/Caddyfile.local presente (perfil https)" }
else { Write-Host "  caddy/Caddyfile.local ausente (perfil https no disponible)" }

$coreScripts = @("local-start.ps1", "local-stop.ps1", "local-verify.ps1", "local-setup.ps1", "local-reset.ps1", "clean-local.ps1", "diagnose-local.ps1")
foreach ($s in $coreScripts) {
  $present = Test-Path (Join-Path $ROOT "scripts\$s")
  if (-not $present) {
    Write-Warn "Falta script core: scripts/$s"
    Add-Result "Configuracion" "WARN" "scripts/$s ausente" "Restaurar el script desde el repositorio"
  }
}

# -----------------------------------------------------------------------------
# E. Secretos (solo presencia y longitud, nunca valores)
# -----------------------------------------------------------------------------
Write-Host "`n== E. Secretos (Credential Manager) =================================="
Write-Host "  Solo se reporta presencia y longitud; nunca valores."

try {
  . (Join-Path $PSScriptRoot "lib\CredentialManager.ps1")
  Initialize-CredentialManager
  $secrets = @("JWT_SECRET", "WHATSAPP_APP_SECRET", "WHATSAPP_ACCESS_TOKEN", "GMAIL_PASSWORD", "OPENAI_API_KEY", "SENTRY_DSN")
  foreach ($name in $secrets) {
    $val = Get-LocalSecret -Name $name -ErrorAction SilentlyContinue
    if ($null -ne $val -and $val.Length -gt 0) {
      Write-OK "Secret presente: $name (longitud $($val.Length))"
      Add-Result "Secretos" "OK" "$name presente (len $($val.Length))" ""
    } else {
      $optional = $name -eq "SENTRY_DSN"
      $prefix = $(if ($optional) { "Write-Warn" } else { "Write-Fail" })
      & $prefix "Secret ausente: $name $(if ($optional) { '(opcional)' } else { '(requerido)' })"
      Add-Result "Secretos" $(if ($optional) { "WARN" } else { "FAIL" }) "$name ausente" "Ejecutar scripts/local-setup.ps1 para definir los secretos"
    }
  }
} catch {
  Write-Warn "Credential Manager no disponible: $($_.Exception.Message)"
  Add-Result "Secretos" "WARN" "Credential Manager no disponible" "Windows con PowerShell 7: ejecutar scripts/local-setup.ps1"
}

# -----------------------------------------------------------------------------
# F. Stack Docker
# -----------------------------------------------------------------------------
Write-Host "`n== F. Stack Docker =================================================="

if ($dockerPath) {
  $daemon = (& docker info --format '{{.ServerVersion}}' 2>&1)
  if ($LASTEXITCODE -eq 0) {
    Write-OK "Daemon Docker activo (server $daemon)"
    Add-Result "Stack" "OK" "Daemon Docker activo" ""
    $containers = (& docker ps -a --format '{{.Names}}|{{.Status}}' 2>&1 | Where-Object { $_ -match '^asistente-' })
    if ($containers) {
      foreach ($line in $containers) {
        $parts = $line -split '\|'
        $name = $parts[0]; $status = $parts[1]
        $health = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $name 2>&1)
        $health = $health | Select-Object -First 1
        if ($status -match 'Up') {
          if ($health -eq "healthy" -or $health -eq "none") {
            Write-OK "$name -> $status (health: $health)"
          } else {
            Write-Warn "$name -> $status (health: $health)"
            Add-Result "Stack" "WARN" "$name no healthy ($health)" "Revisar docker logs $name"
          }
        } else {
          Write-Warn "$name -> $status"
          Add-Result "Stack" "WARN" "$name detenido" "Ejecutar scripts/local-start.ps1"
        }
      }
    } else {
      Write-Warn "No hay contenedores del stack local (asistente-*)"
      Add-Result "Stack" "WARN" "Stack detenido" "Ejecutar scripts/local-start.ps1"
    }
  } else {
    Write-Fail "Daemon Docker no disponible"
    Add-Result "Stack" "FAIL" "Daemon Docker inactivo" "Iniciar Docker Desktop y esperar a que este listo"
  }
}

# -----------------------------------------------------------------------------
# G. Frontend
# -----------------------------------------------------------------------------
Write-Host "`n== G. Frontend ======================================================="

$lockfile = Test-Path (Join-Path $ROOT "frontend-react\pnpm-lock.yaml")
if ($lockfile) { Write-OK "pnpm-lock.yaml presente" }
else {
  Write-Fail "Falta frontend-react/pnpm-lock.yaml"
  Add-Result "Frontend" "FAIL" "pnpm-lock.yaml ausente" "Ejecutar scripts/local-setup.ps1 (instala dependencias con lockfile)"
}
Add-Result "Frontend" $(if ($lockfile) { "OK" } else { "FAIL" }) "pnpm-lock.yaml" ""

$nodeModules = Test-Path (Join-Path $ROOT "frontend-react\node_modules")
if ($nodeModules) { Write-OK "node_modules presente (frontend)" }
else {
  Write-Warn "node_modules ausente (se instalara con local-setup.ps1)"
  Add-Result "Frontend" "WARN" "node_modules ausente" "Ejecutar scripts/local-setup.ps1"
}

$pkgJson = Join-Path $ROOT "frontend-react\package.json"
if (Test-Path $pkgJson) {
  $engines = (Get-Content $pkgJson -Raw | ConvertFrom-Json).engines
  if ($engines.node) {
    Write-OK "engines.node en package.json: $($engines.node)"
  } else {
    Write-Warn "package.json sin engines.node"
    Add-Result "Frontend" "WARN" "Sin engines.node" "Agregar engines.node >=20.19.0"
  }
  if ($engines.pnpm) {
    Write-OK "engines.pnpm en package.json: $($engines.pnpm)"
  }
}

# -----------------------------------------------------------------------------
# Reporte sanitizado
# -----------------------------------------------------------------------------
Write-Host "`n== Resumen =========================================================="

$fails = @($global:results | Where-Object { $_.Status -eq "FAIL" })
$warns = @($global:results | Where-Object { $_.Status -eq "WARN" })
$oks = @($global:results | Where-Object { $_.Status -eq "OK" })
Write-Host "  OK: $($oks.Count) | AVISOS: $($warns.Count) | ERRORES: $($fails.Count)"

if ($fails.Count -gt 0) {
  Write-Host "`n  Acciones requeridas:"
  foreach ($f in $fails) {
    Write-Host "    - [$($f.Section)] $($f.Message)"
    if ($f.Action) { Write-Host "      Accion: $($f.Action)" }
  }
}

if ($OutFile) {
  $lines = @()
  $lines += "Diagnostico del ambiente local - $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
  $lines += "Host: $env:COMPUTERNAME | OS: $((Get-CimInstance Win32_OperatingSystem).Caption)"
  $lines += "NOTA: reporte sanitizado; no incluye valores de secretos ni de .env.local."
  $lines += ""
  foreach ($r in $global:results) {
    if ($r.Status -eq "INFO") { continue }
    $lines += "[$($r.Status)] $($r.Section): $($r.Message)"
    if ($r.Action) { $lines += "    Accion: $($r.Action)" }
  }
  $lines += ""
  $lines += "Resumen: $($oks.Count) OK | $($warns.Count) avisos | $($fails.Count) errores"
  $lines | Set-Content -Path $OutFile -Encoding utf8
  Write-Host "`nReporte sanitizado guardado en: $OutFile"
}

if ($fails.Count -gt 0) {
  Write-Host "`nResultado: ERRORES CRITICOS (corregir antes de continuar)." -ForegroundColor Red
  exit 1
}
Write-Host "`nResultado: ambiente OK (pueden existir avisos no bloqueantes)." -ForegroundColor Green
exit 0
