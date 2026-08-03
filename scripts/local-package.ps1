#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Genera el paquete distribuible del proyecto: fuente (lista blanca via
  git ls-files), artefactos compilados, configuracion y manifiesto con
  revision, versiones, dependencias y sumas SHA-256.
.DESCRIPTION
  Pasos:
    1. Compilar backend (mvnw clean package -DskipTests)
    2. Construir frontend (pnpm build)
    3. Registrar dependencias (mvn dependency:list)
    4. Copiar la fuente con lista blanca (git ls-files): nunca incluye
       node_modules, target, dist, .git, logs, capturas, archivos de
       ejecucion, .env.* ni MEMORY.md
    5. Adjuntar artefactos compilados (jar, dist) y configuracion
    6. Generar manifiesto (revision, versiones fijadas, SHA-256 por archivo)
    7. Empaquetar en un unico ZIP y emitir SHA256SUMS del ZIP
.PARAMETER OutputDir
  Directorio de salida. Default: $ROOT/target/package
.PARAMETER SkipBuild
  Omite compilar backend y frontend (usa artefactos existentes).
.EXAMPLE
  .\scripts\local-package.ps1
  .\scripts\local-package.ps1 -SkipBuild
#>

param(
  [string]$OutputDir = "",
  [switch]$SkipBuild
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ErrorActionPreference = 'Stop'

if (-not $OutputDir) {
  $OutputDir = Join-Path $ROOT "target" "package"
}
$OutputDir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputDir)

# ── 0. Revision y versiones ─────────────────────────────────
$revision = (& git -C $ROOT rev-parse --short HEAD 2>$null)
if (-not $revision) { $revision = "sin-git" }
$staging = Join-Path $OutputDir "staging"
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
New-Item -ItemType Directory -Path $staging -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $staging "metadata") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $staging "artifacts") -Force | Out-Null

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  EMPAQUETADO DE ARTEFACTOS (Fase 6)" -ForegroundColor Cyan
Write-Host "  Revision: $revision" -ForegroundColor Cyan
Write-Host "  OutputDir: $OutputDir" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# ── 1. Compilar backend ────────────────────────────────────
if (-not $SkipBuild) {
  Write-Host "`n>>> 1/6 - Compilando backend..." -ForegroundColor Cyan
  Push-Location "$ROOT\backend-java"
  try {
    .\mvnw.cmd clean package -B -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "mvnw package fallo" }
    Write-Host "  [OK] Backend compilado" -ForegroundColor Green
  } finally { Pop-Location }
}

# ── 2. Construir frontend ──────────────────────────────────
if (-not $SkipBuild) {
  Write-Host "`n>>> 2/6 - Construyendo frontend..." -ForegroundColor Cyan
  Push-Location "$ROOT\frontend-react"
  try {
    pnpm build
    if ($LASTEXITCODE -ne 0) { throw "pnpm build fallo" }
    Write-Host "  [OK] Frontend construido" -ForegroundColor Green
  } finally { Pop-Location }
}

# ── 3. Registrar dependencias ──────────────────────────────
Write-Host "`n>>> 3/6 - Registrando dependencias (mvn dependency:list)..." -ForegroundColor Cyan
$depsFile = Join-Path $staging "metadata\dependencies.txt"
New-Item -ItemType Directory -Path (Split-Path $depsFile) -Force | Out-Null
Push-Location "$ROOT\backend-java"
try {
  .\mvnw.cmd -B -q dependency:list "-DincludeScope=runtime" "-DoutputFile=$depsFile"
  if ($LASTEXITCODE -ne 0) { throw "mvn dependency:list fallo" }
  $depCount = (Get-Content $depsFile | Where-Object { $_ -match ':jar:' }).Count
  Write-Host "  [OK] $depCount dependencias registradas" -ForegroundColor Green
} finally { Pop-Location }

# ── 4. Copiar fuente (lista blanca: git ls-files) ──────────
Write-Host "`n>>> 4/6 - Copiando fuente con lista blanca (git ls-files)..." -ForegroundColor Cyan
Push-Location $ROOT
try {
  & git ls-files | ForEach-Object {
    $rel = $_
    if (-not $rel) { return }
    $target = Join-Path $staging $rel
    $destDir = Split-Path $target -Parent
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item -Path (Join-Path $ROOT $rel) -Destination $target -Force
  }
} finally { Pop-Location }
$sourceCount = (& git -C $ROOT ls-files | Measure-Object | ForEach-Object Count)
Write-Host "  [OK] $sourceCount archivos de fuente copiados (sin node_modules/target/dist/.git/.env.*)" -ForegroundColor Green

# ── 5. Adjuntar artefactos y configuracion ─────────────────
Write-Host "`n>>> 5/6 - Adjuntando artefactos y configuracion..." -ForegroundColor Cyan

$jarFiles = Get-ChildItem "$ROOT\backend-java\target\*.jar" -ErrorAction SilentlyContinue | Where-Object { -not $_.Name.EndsWith("-sources.jar") -and -not $_.Name.EndsWith("-javadoc.jar") }
if ($jarFiles) {
  foreach ($jar in $jarFiles) {
    Copy-Item $jar.FullName -Destination (Join-Path $staging "artifacts\$($jar.Name)") -Force
  }
  Write-Host "  [OK] JAR del backend en artifacts/" -ForegroundColor Green
} else {
  Write-Warning "No hay JAR en backend-java/target/ (usa -SkipBuild solo si ya compilaste)"
}

$frontendDist = Join-Path $ROOT "frontend-react" "dist"
if (Test-Path $frontendDist) {
  $distZip = Join-Path $staging "artifacts\frontend-dist.zip"
  Compress-Archive -Path "$frontendDist\*" -DestinationPath $distZip
  Write-Host "  [OK] frontend-dist.zip en artifacts/" -ForegroundColor Green
} else {
  Write-Warning "frontend-react/dist no encontrado"
}

Copy-Item "$ROOT\docker-compose.local.yml" -Destination (Join-Path $staging "docker-compose.local.yml") -Force
if (Test-Path "$ROOT\.env.local.template") {
  Copy-Item "$ROOT\.env.local.template" -Destination (Join-Path $staging ".env.local.template") -Force
}

# ── 6. Manifiesto y ZIP ────────────────────────────────────
Write-Host "`n>>> 6/6 - Generando manifiesto y ZIP..." -ForegroundColor Cyan

function Get-FileSha256 {
  param([string]$Path)
  (Get-FileHash -Path $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

$nodeVer = (& node --version 2>$null) ?? "n/a"
$pnpmVer = (& pnpm --version 2>$null) ?? "n/a"
$javaVer = (& java -version 2>&1 | Select-Object -First 1) ?? "n/a"
$mvnVer = (& $ROOT\backend-java\mvnw.cmd --version 2>$null | Select-Object -First 1) ?? "n/a"
$dockerVer = (& docker --version 2>$null) ?? "n/a"
$composeVer = (& docker compose version 2>$null) ?? "n/a"

$files = @()
Get-ChildItem $staging -Recurse -File | ForEach-Object {
  $rel = $_.FullName.Substring($staging.Length + 1).Replace('\', '/')
  $files += [pscustomobject]@{
    path = $rel
    size = $_.Length
    sha256 = Get-FileSha256 $_.FullName
  }
}

$manifest = [pscustomobject]@{
  format = "asistente-package-manifest-v1"
  revision = $revision
  generated = (Get-Date -Format "yyyy-MM-dd'T'HH:mm:sszzz")
  tools = [pscustomobject]@{
    node = $nodeVer
    pnpm = $pnpmVer
    java = $javaVer
    maven = $mvnVer
    docker = $dockerVer
    compose = $composeVer
  }
  sourceFiles = $sourceCount
  dependenciesRuntime = $depCount
  totalFiles = $files.Count
  totalSizeBytes = ($files | Measure-Object -Property size -Sum).Sum
  files = $files
}
$manifestJson = $manifest | ConvertTo-Json -Depth 10 -WarningAction SilentlyContinue
$manifestJson | Set-Content -Path (Join-Path $staging "metadata\manifest.json") -Encoding utf8

$zipName = "asistente-package-$revision.zip"
$zipPath = Join-Path $OutputDir $zipName
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Compress-Archive -Path "$staging\*" -DestinationPath $zipPath
$zipSha = Get-FileSha256 $zipPath
Set-Content -Path (Join-Path $OutputDir "SHA256SUMS.txt") -Value "$zipSha  $zipName" -Encoding ascii
Remove-Item $staging -Recurse -Force

$zipSizeMB = [math]::Round((Get-Item $zipPath).Length / 1MB, 2)
Write-Host ""
Write-Host "  [OK] $zipName ($zipSizeMB MB)" -ForegroundColor Green
Write-Host "  [OK] SHA256SUMS.txt ($zipSha)" -ForegroundColor Green
Write-Host "  [OK] Manifiesto embebido en metadata/manifest.json" -ForegroundColor Green
Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  EMPAQUETADO COMPLETADO" -ForegroundColor Green
Write-Host "  $zipPath" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
