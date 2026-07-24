#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Empaqueta artefactos del proyecto para distribucion o deploy.
.DESCRIPTION
  Genera en target/package/:
    - backend-java-<version>.jar (del backend)
    - frontend-dist.zip (build del frontend)
    - docker-compose.local.yml + .env.example (config)
  Opcionalmente genera imagenes Docker locales.
.PARAMETER OutputDir
  Directorio de salida. Default: $ROOT/target/package
.PARAMETER DockerImages
  Construye imagenes Docker locales (docker compose build).
.PARAMETER SkipTests
  Omite tests al compilar.
.EXAMPLE
  .\scripts\local-package.ps1
  .\scripts\local-package.ps1 -DockerImages
  .\scripts\local-package.ps1 -OutputDir C:\dist
#>

param(
  [string]$OutputDir = "",
  [switch]$DockerImages,
  [switch]$SkipTests
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ErrorActionPreference = 'Stop'

if (-not $OutputDir) {
  $OutputDir = Join-Path $ROOT "target" "package"
}

$OutputDir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputDir)

if (-not (Test-Path $OutputDir)) {
  New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  EMPAQUETADO DE ARTEFACTOS               ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host "  OutputDir: $OutputDir" -ForegroundColor Gray

# ── 1. Compilar backend ────────────────────────────────────
Write-Host "`n>>> 1/4 - Compilando backend..." -ForegroundColor Cyan
Push-Location "$ROOT\backend-java"
try {
  $mvnArgs = "clean package -B"
  if ($SkipTests) { $mvnArgs += " -DskipTests" }
  .\mvnw.cmd $mvnArgs
  if ($LASTEXITCODE -ne 0) { throw "mvnw package fallo" }
  Write-Host "  [OK] Backend compilado" -ForegroundColor Green
} finally { Pop-Location }

# ── 2. Construir frontend ──────────────────────────────────
Write-Host "`n>>> 2/4 - Construyendo frontend..." -ForegroundColor Cyan
Push-Location "$ROOT\frontend-react"
try {
  pnpm build
  if ($LASTEXITCODE -ne 0) { throw "pnpm build fallo" }
  Write-Host "  [OK] Frontend construido" -ForegroundColor Green
} finally { Pop-Location }

# ── 3. Copiar artefactos ───────────────────────────────────
Write-Host "`n>>> 3/4 - Copiando artefactos a $OutputDir..." -ForegroundColor Cyan

# Backend JAR
$jarFiles = Get-ChildItem "$ROOT\backend-java\target\*.jar" | Where-Object { -not $_.Name.EndsWith("-sources.jar") -and -not $_.Name.EndsWith("-javadoc.jar") }
if ($jarFiles.Count -eq 0) { Write-Warning "No se encontraron JARs en backend-java/target/" }
foreach ($jar in $jarFiles) {
  Copy-Item $jar.FullName -Destination (Join-Path $OutputDir $jar.Name) -Force
  Write-Host "  [OK] $($jar.Name)" -ForegroundColor Green
}

# Frontend dist
$frontendDist = Join-Path $ROOT "frontend-react" "dist"
if (Test-Path $frontendDist) {
  $distZip = Join-Path $OutputDir "frontend-dist.zip"
  if (Test-Path $distZip) { Remove-Item $distZip -Force }
  Compress-Archive -Path "$frontendDist\*" -DestinationPath $distZip
  Write-Host "  [OK] frontend-dist.zip" -ForegroundColor Green
} else {
  Write-Warning "frontend-react/dist no encontrado, ejecuta pnpm build primero"
}

# Docker compose + env example
Copy-Item "$ROOT\docker-compose.local.yml" -Destination (Join-Path $OutputDir "docker-compose.local.yml") -Force
Write-Host "  [OK] docker-compose.local.yml" -ForegroundColor Green

$envExample = Join-Path $ROOT ".env.example"
if (Test-Path $envExample) {
  Copy-Item $envExample -Destination (Join-Path $OutputDir ".env.example") -Force
  Write-Host "  [OK] .env.example" -ForegroundColor Green
}

# ── 4. Docker images (opcional) ────────────────────────────
if ($DockerImages) {
  Write-Host "`n>>> 4/4 - Construyendo imagenes Docker..." -ForegroundColor Cyan
  $composeFile = Join-Path $ROOT "docker-compose.local.yml"
  docker compose -f $composeFile build --no-cache
  if ($LASTEXITCODE -ne 0) { throw "docker compose build fallo" }
  Write-Host "  [OK] Imagenes Docker construidas" -ForegroundColor Green
} else {
  Write-Host "`n>>> 4/4 - Imagenes Docker omitidas (usa -DockerImages para incluirlas)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  EMPAQUETADO COMPLETADO                   ║" -ForegroundColor Green
Write-Host "║  Artefactos en:                          ║" -ForegroundColor Green
Write-Host "║    $OutputDir                 ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
