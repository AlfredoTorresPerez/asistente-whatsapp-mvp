#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Verifica prerequisitos, instala dependencias y construye backend + frontend
  para entorno local.
.DESCRIPTION
  Ejecuta en orden:
    1. Verificar Java 21+, Docker, Node >=18, pnpm
    2. Instalar dependencias del frontend (pnpm install --frozen-lockfile)
    3. Compilar backend (mvnw clean package -DskipTests)
    4. Construir frontend (pnpm build)
.PARAMETER SkipBackend
  Omite compilacion del backend.
.PARAMETER SkipFrontend
  Omite build del frontend.
.PARAMETER SkipInstall
  Omite instalacion de dependencias.
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
function Write-Fail { param([string]$Msg) Write-Host "  [FAIL] $Msg" -ForegroundColor Red; exit 1 }

# ── 1. Verificar prerequisitos ─────────────────────────────
Write-Step "Verificando prerequisitos..."

# Java 21+
$javaVersion = & java -version 2>&1
if ($LASTEXITCODE -ne 0 -or ($javaVersion -match 'version "(\d+)').Count -eq 0) {
  Write-Fail "Java no encontrado. Instala Java 21+ (Eclipse Temurin recomendado)"
}
$javaVerNum = if ($javaVersion -match 'version "(\d+)') { $matches[1] }
if ([int]$javaVerNum -lt 21) { Write-Fail "Se requiere Java 21+, detectado: $javaVerNum" }
Write-OK "Java $javaVerNum"

# Docker
$dockerVer = & docker --version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Fail "Docker no encontrado. Instala Docker Desktop" }
Write-OK "Docker: $dockerVer"

# Node >=18
$nodeVer = & node --version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Fail "Node.js no encontrado" }
if ($nodeVer -match 'v(\d+)') {
  if ([int]$matches[1] -lt 18) { Write-Fail "Se requiere Node >=18, detectado: $nodeVer" }
}
Write-OK "Node: $nodeVer"

# pnpm
$pnpmVer = & pnpm --version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm no encontrado. Instala: npm install -g pnpm" }
Write-OK "pnpm: $pnpmVer"

# ── 2. Instalar dependencias ───────────────────────────────
if (-not $SkipInstall) {
  Write-Step "Instalando dependencias del frontend..."
  Push-Location "$ROOT\frontend-react"
  try {
    if (Test-Path "node_modules") {
      Write-Host "  node_modules ya existe, saltando install" -ForegroundColor Yellow
    } else {
      pnpm install --frozen-lockfile
      if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm install fallo" }
      Write-OK "pnpm install --frozen-lockfile"
    }
  } finally { Pop-Location }
}

# ── 3. Compilar backend ────────────────────────────────────
if (-not $SkipBackend) {
  Write-Step "Compilando backend (mvnw clean package -DskipTests)..."
  Push-Location "$ROOT\backend-java"
  try {
    .\mvnw.cmd clean package -DskipTests -B
    if ($LASTEXITCODE -ne 0) { Write-Fail "mvnw package fallo" }
    Write-OK "Backend compilado"
  } finally { Pop-Location }
}

# ── 4. Construir frontend ──────────────────────────────────
if (-not $SkipFrontend) {
  Write-Step "Construyendo frontend (pnpm build)..."
  Push-Location "$ROOT\frontend-react"
  try {
    pnpm build
    if ($LASTEXITCODE -ne 0) { Write-Fail "pnpm build fallo" }
    Write-OK "Frontend construido"
  } finally { Pop-Location }
}

# ── Resumen ─────────────────────────────────────────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  SETUP COMPLETADO                        ║" -ForegroundColor Green
Write-Host "║  Usa .\scripts\local-start.ps1 para      ║" -ForegroundColor Green
Write-Host "║  levantar los servicios con Docker.     ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
