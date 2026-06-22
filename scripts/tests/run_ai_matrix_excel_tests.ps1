Param(
  [switch]$Strict
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Backend = Join-Path $Root "backend-java"

$MavenArgs = @("-Dtest=AiExcelMatrixOrchestratorCoverageTest")
if ($Strict) {
  $MavenArgs += "-Dai.matrix.strict=true"
}
$MavenArgs += "test"

Push-Location $Backend
try {
  if ($Strict) {
    Write-Host "Ejecutando matriz IA en modo estricto..."
  } else {
    Write-Host "Ejecutando matriz IA en modo auditoria..."
  }

  try {
    .\mvnw.cmd @MavenArgs
  } catch {
    Write-Warning "No fue posible ejecutar Maven Wrapper. Intentando fallback con Docker Maven..."
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
      throw
    }
    docker run --rm -v "${Backend}:/app" -w /app maven:3.9.9-eclipse-temurin-21 mvn @MavenArgs
  }

  $Report = Join-Path $Backend "target\ai-matrix\reporte_matriz_excel_ia_v23_4_22.md"
  if (Test-Path $Report) {
    Write-Host "Reporte generado: $Report"
  } else {
    Write-Warning "No se encontro el reporte esperado: $Report"
  }
} finally {
  Pop-Location
}
