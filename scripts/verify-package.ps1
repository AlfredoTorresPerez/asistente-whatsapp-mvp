#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Prueba automatica de reconstruccion desde el paquete distribuible.
.DESCRIPTION
  Toma el ZIP generado por local-package.ps1, lo extrae en un directorio
  temporal limpio y verifica:
    1. Integridad: cada archivo del manifiesto coincide con su SHA-256
    2. Ausencia de artefactos empaquetados accidentalmente
       (node_modules, target, dist, .git, .env.*, logs, MEMORY.md)
    3. Reconstruccion del frontend: pnpm install --frozen-lockfile
    4. Reconstruccion del backend: mvnw package -DskipTests
    5. Build del frontend: pnpm build
    6. docker compose config --quiet (compose incluido en el paquete)
  Es idempotente: puede ejecutarse dos veces consecutivas y debe dar el
  mismo resultado (criterio de aceptacion de la Fase 6).
.PARAMETER Package
  Ruta al ZIP del paquete. Default: el ultimo asistente-package-*.zip en target/package.
.PARAMETER SkipRebuild
  Omite los pasos 3-5 (solo verifica integridad y exclusiones).
.EXAMPLE
  .\scripts\verify-package.ps1
  .\scripts\verify-package.ps1 -Package target\package\asistente-package-abc123.zip
#>

param(
  [string]$Package = "",
  [switch]$SkipRebuild
)

$ErrorActionPreference = 'Stop'
$ROOT = Split-Path -Parent $PSScriptRoot
$failed = $false

function Write-Step { param([string]$Msg) Write-Host "`n$Msg" -ForegroundColor Cyan }
function Write-OK   { param([string]$Msg) Write-Host "  [OK] $Msg" -ForegroundColor Green }
function Write-Fail { param([string]$Msg) Write-Host "  [FAIL] $Msg" -ForegroundColor Red; $script:failed = $true }

if (-not $Package) {
  $Package = Get-ChildItem (Join-Path $ROOT "target\package\asistente-package-*.zip") -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $Package -or -not (Test-Path $Package)) {
  Write-Fail "No se encontro el paquete. Ejecuta primero scripts/local-package.ps1"
  exit 1
}
$Package = (Resolve-Path $Package).Path

$work = Join-Path $env:TEMP ("verify-package-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $work -Force | Out-Null
Write-Host "=== VERIFICACION DEL PAQUETE: $([IO.Path]::GetFileName($Package)) ===" -ForegroundColor Cyan
Write-Host "  Extraido en: $work"

try {
  # ── 1. Extraer ────────────────────────────────────────────
  Write-Step "1/6 - Extrayendo el paquete..."
  Expand-Archive -Path $Package -DestinationPath $work -Force
  $manifestPath = Join-Path $work "metadata\manifest.json"
  if (-not (Test-Path $manifestPath)) {
    Write-Fail "Falta metadata/manifest.json en el paquete"
    exit 1
  }
  $manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
  Write-OK "Manifiesto leido (revision $($manifest.revision), $($manifest.totalFiles) archivos)"

  # ── 2. Verificar SHA-256 de cada archivo ─────────────────
  Write-Step "2/6 - Verificando SHA-256 del manifiesto..."
  $mismatches = 0
  foreach ($f in $manifest.files) {
    $target = Join-Path $work ($f.path -replace '/', '\')
    if (-not (Test-Path $target)) {
      Write-Fail "Archivo faltante: $($f.path)"
      $mismatches++
      continue
    }
    $hash = (Get-FileHash $target -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $f.sha256) {
      Write-Fail "SHA-256 no coincide: $($f.path)"
      $mismatches++
    }
  }
  if ($mismatches -eq 0) { Write-OK "SHA-256 verificado para $($manifest.files.Count) archivos" }

  # ── 3. Ausencia de artefactos accidentalmente empaquetados
  Write-Step "3/6 - Verificando exclusiones (lista blanca)..."
  $forbidden = @("node_modules", "target", "dist", ".git", ".env.local", ".env.qa", ".env.production", "MEMORY.md", "logs", "observabilidad-capturas", "registro_ejecucion_IA.json", "e2e/reports", "e2e/screenshots", "playwright-report", "test-results")
  $violations = 0
  $allFiles = Get-ChildItem $work -Recurse -Force -File -ErrorAction SilentlyContinue
  foreach ($f in $allFiles) {
    $rel = $f.FullName.Substring($work.Length).TrimStart('\').Replace('\', '/')
    foreach ($pat in $forbidden) {
      if ($rel -match ('(^|/)' + [regex]::Escape($pat) + '($|/)')) {
        Write-Fail "Artefacto empaquetado accidentalmente: $pat (en $rel)"
        $violations++
      }
    }
  }
  if ($violations -eq 0) { Write-OK "Sin artefactos empaquetados accidentalmente ($($forbidden.Count) patrones auditados)" }

  # ── 4. Reconstruccion frontend (install limpio) ──────────
  Write-Step "4/6 - Reconstruyendo frontend (pnpm install --frozen-lockfile)..."
  if ($SkipRebuild) { Write-Host "  [--] Omitido (-SkipRebuild)" -ForegroundColor DarkGray }
  else {
    Push-Location (Join-Path $work "frontend-react")
    try {
      pnpm install --frozen-lockfile
      if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm install fallo" } else { Write-OK "pnpm install --frozen-lockfile" }
    } finally { Pop-Location }
  }

  # ── 5. Reconstruccion backend ─────────────────────────────
  Write-Step "5/6 - Reconstruyendo backend (mvnw package -DskipTests)..."
  if ($SkipRebuild) { Write-Host "  [--] Omitido (-SkipRebuild)" -ForegroundColor DarkGray }
  else {
    Push-Location (Join-Path $work "backend-java")
    try {
      .\mvnw.cmd -B -DskipTests package | Out-Null
      if ($LASTEXITCODE -ne 0) { Write-Fail "mvnw package fallo" } else { Write-OK "mvnw package (jar generado)" }
    } finally { Pop-Location }
  }

  # ── 6. Build frontend + compose config ───────────────────
  Write-Step "6/6 - Build frontend y config compose..."
  if ($SkipRebuild) {
    Write-Host "  [--] Build omitido (-SkipRebuild)" -ForegroundColor DarkGray
  } else {
    Push-Location (Join-Path $work "frontend-react")
    try {
      pnpm build | Out-Null
      if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm build fallo" } else { Write-OK "pnpm build (dist generado)" }
    } finally { Pop-Location }
  }
  $composeIn = Join-Path $work "docker-compose.local.yml"
  if (Test-Path $composeIn) {
    docker compose -f $composeIn config --quiet
    if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose config fallo" } else { Write-OK "docker compose config valido" }
  } else {
    Write-Fail "Falta docker-compose.local.yml en el paquete"
  }
} finally {
  Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
if ($failed) {
  Write-Host "VERIFICACION DEL PAQUETE: FALLOS" -ForegroundColor Yellow
  exit 1
}
Write-Host "VERIFICACION DEL PAQUETE: TODO OK" -ForegroundColor Green
exit 0
