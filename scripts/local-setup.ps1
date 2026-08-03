#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Verifica prerequisitos, instala dependencias y construye backend + frontend
  para entorno local.
.DESCRIPTION
  Ejecuta en orden:
    1. Verificar plataforma, Java 21+, Docker, Node 20.19+, pnpm 10.x y
       lockfile del frontend
    2. Instalar dependencias del frontend (pnpm install --frozen-lockfile)
    3. Compilar backend (mvnw clean package -DskipTests)
    4. Construir frontend (pnpm build)
  La instalacion de dependencias NUNCA se omite por la mera existencia de
  node_modules: siempre valida el lockfile (pnpm install --frozen-lockfile es
  idempotente). Usar -SkipInstall solo cuando se sabe que las dependencias
  estan al dia.
.PARAMETER SkipBackend
  Omite compilacion del backend.
.PARAMETER SkipFrontend
  Omite build del frontend.
.PARAMETER SkipInstall
  Omite instalacion de dependencias (no recomendado).
.PARAMETER Force
  Omite confirmaciones.
.EXAMPLE
  .\scripts\local-setup.ps1
  .\scripts\local-setup.ps1 -SkipBackend
#>

param(
  [switch]$SkipBackend,
  [switch]$SkipFrontend,
  [switch]$SkipInstall,
  [switch]$Force
)

$ROOT = Split-Path -Parent $PSScriptRoot
$ErrorActionPreference = 'Stop'

function Write-Step { param([string]$Msg) Write-Host "`n>>> $Msg" -ForegroundColor Cyan }
function Write-OK   { param([string]$Msg) Write-Host "  [OK] $Msg" -ForegroundColor Green }
function Write-Fail { param([string]$Msg, [string]$Action = "") Write-Host "  [FAIL] $Msg" -ForegroundColor Red; if ($Action) { Write-Host "  Accion: $Action" -ForegroundColor Yellow }; exit 1 }

# ── 0. Verificar plataforma ────────────────────────────────
Write-Step "Verificando plataforma..."

if ($env:OS -ne "Windows_NT") {
  Write-Fail "Este script es para Windows. En Linux/macOS usa scripts/local-setup.sh"
}

# ── 1. Verificar prerequisitos ─────────────────────────────
Write-Step "Verificando prerequisitos..."

# Java 21+
$javaOutput = (& java -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaOutput -notmatch 'version "(\d+)') {
  Write-Fail "Java no encontrado" "Instala Java 21+ (Eclipse Temurin recomendado) y verifica con java -version"
}
$javaVerNum = $matches[1]
if ([int]$javaVerNum -lt 21) { Write-Fail "Se requiere Java 21+, detectado: $javaVerNum" "Actualiza tu JDK a 21+ (Temurin)" }
Write-OK "Java $javaVerNum"

# Docker
$dockerVer = & docker --version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Fail "Docker no encontrado" "Instala Docker Desktop y verificaLo con docker --version" }
Write-OK "Docker: $dockerVer"
$dockerCompose = & docker compose version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Fail "Plugin Docker Compose no disponible" "Actualiza Docker Desktop (incluye el plugin compose)" }
Write-OK "Compose: $dockerCompose"

# Node 20.19+
$nodeVer = & node --version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Fail "Node.js no encontrado" "Instala Node 20.19+ (ver frontend-react/.nvmrc, ej: nvm install 20.19.0)" }
if ($nodeVer -match 'v(\d+)\.(\d+)') {
  $nodeMajor = [int]$matches[1]; $nodeMinor = [int]$matches[2]
  if (-not ($nodeMajor -gt 20 -or ($nodeMajor -eq 20 -and $nodeMinor -ge 19))) {
    Write-Fail "Se requiere Node 20.19+, detectado: $nodeVer" "Instala Node 20.19+ (ver frontend-react/.nvmrc)"
  }
}
Write-OK "Node: $nodeVer"

# pnpm 10.x
$pnpmVer = & pnpm --version 2>&1
if ($LASTEXITCODE -ne 0) {
  Write-Fail "pnpm no encontrado" "Activa corepack: corepack enable && corepack prepare pnpm@10.18.3 --activate"
}
if ($pnpmVer -notmatch '^10\.') {
  Write-Fail "Se requiere pnpm 10.x, detectado: $pnpmVer" "corepack prepare pnpm@10.18.3 --activate (usa la version fijada en package.json)"
}
Write-OK "pnpm: $pnpmVer"

# Lockfile del frontend
$lockfile = Join-Path $ROOT "frontend-react\pnpm-lock.yaml"
if (-not (Test-Path $lockfile)) {
  Write-Fail "Falta frontend-react/pnpm-lock.yaml" "El lockfile es obligatorio para instalar con --frozen-lockfile; restaurar el archivo desde el repositorio"
}
Write-OK "Lockfile presente: frontend-react/pnpm-lock.yaml"

# ── 2. Instalar dependencias ───────────────────────────────
if (-not $SkipInstall) {
  Write-Step "Instalando dependencias del frontend (pnpm install --frozen-lockfile)..."
  Push-Location "$ROOT\frontend-react"
  try {
    if (Test-Path "node_modules") {
      Write-Host "  node_modules existe: se revalida contra el lockfile (--frozen-lockfile es idempotente)" -ForegroundColor Yellow
    }
    pnpm install --frozen-lockfile
    if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm install fallo" "Revisa el error; si es de red, reintenta. Si el lockfile esta desactualizado, ejecuta pnpm install y commitear el lockfile" }
    Write-OK "pnpm install --frozen-lockfile"
  } finally { Pop-Location }
} else {
  Write-Host "  -SkipInstall: se omite la instalacion de dependencias" -ForegroundColor Yellow
}

# ── 3. Compilar backend ────────────────────────────────────
if (-not $SkipBackend) {
  Write-Step "Compilando backend (mvnw clean package -DskipTests)..."
  Push-Location "$ROOT\backend-java"
  try {
    .\mvnw.cmd clean package -DskipTests -B
    if ($LASTEXITCODE -ne 0) { Write-Fail "mvnw package fallo" "Revisa el log de Maven; usualmente es un test o compilacion. Intenta: .\backend-java\mvnw.cmd clean package -DskipTests -B -q" }
    Write-OK "Backend compilado"
  } finally { Pop-Location }
}

# ── 4. Construir frontend ──────────────────────────────────
if (-not $SkipFrontend) {
  Write-Step "Construyendo frontend (pnpm build)..."
  Push-Location "$ROOT\frontend-react"
  try {
    pnpm build
    if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm build fallo" "Revisa el error de TypeScript/Vite en la consola; corrige y reintenta" }
    Write-OK "Frontend construido"
  } finally { Pop-Location }
}

# ── Resumen ─────────────────────────────────────────────────
Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  SETUP COMPLETADO" -ForegroundColor Green
Write-Host "  Usa .\scripts\local-start.ps1 para" -ForegroundColor Green
Write-Host "  levantar los servicios con Docker." -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
