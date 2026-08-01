#!/usr/bin/env pwsh
# =============================================================================
# run-all.ps1 — Central task runner
# Ejecuta validaciones en todos los componentes autorizados.
# Se detiene al primer error.
# =============================================================================
$ErrorActionPreference = 'Stop'
$rootDir = Split-Path -Parent $PSScriptRoot

$failed = $false

function Run-Step {
  param($Name, $ScriptBlock)
  Write-Host "`n==========================================" -ForegroundColor Cyan
  Write-Host "  $Name" -ForegroundColor Cyan
  Write-Host "==========================================" -ForegroundColor Cyan
  try {
    & $ScriptBlock
    Write-Host "  [OK] $Name" -ForegroundColor Green
  } catch {
    Write-Host "  [FAIL] $Name : $_" -ForegroundColor Red
    $failed = $true
    throw "Detenido por error en: $Name"
  }
}

try {
  # Backend
  Run-Step -Name "Backend: verify" -ScriptBlock {
    Push-Location "$rootDir/backend-java"
    .\mvnw.cmd verify -B "-Dspring.profiles.active=test"
    if ($LASTEXITCODE -ne 0) { throw "mvnw verify falló" }
    Pop-Location
  }

  # Frontend
  Run-Step -Name "Frontend: format check" -ScriptBlock {
    Push-Location "$rootDir/frontend-react"
    pnpm format:check
    Pop-Location
  }

  Run-Step -Name "Frontend: lint" -ScriptBlock {
    Push-Location "$rootDir/frontend-react"
    pnpm lint
    Pop-Location
  }

  Run-Step -Name "Frontend: test" -ScriptBlock {
    Push-Location "$rootDir/frontend-react"
    pnpm test -- --run
    Pop-Location
  }

  Run-Step -Name "Frontend: build" -ScriptBlock {
    Push-Location "$rootDir/frontend-react"
    pnpm build
    Pop-Location
  }

  Write-Host "`n==========================================" -ForegroundColor Green
  Write-Host "  TODOS LOS COMPONENTES AUTORIZADOS OK" -ForegroundColor Green
  Write-Host "==========================================" -ForegroundColor Green

} catch {
  Write-Host "`n==========================================" -ForegroundColor Yellow
  Write-Host "  BLOQUEO CONOCIDO Y DIFERIDO POR DECISION DEL PROPIETARIO" -ForegroundColor Yellow
  Write-Host "==========================================" -ForegroundColor Yellow
  exit 1
}
