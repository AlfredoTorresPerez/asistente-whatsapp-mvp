# =============================================================================
# TEST RECOVERY NEGATIVE - PostgreSQL (PowerShell 7+)
# Uso: .\scripts\test-recovery-negative.ps1 [[-BackupDir] <dir>]
# Ejecuta las pruebas negativas de la capacidad de recuperacion (Fase 9):
#   1. Archivo vacio                      -> restore debe fallar y no dejar BD temporal
#   2. Suma SHA-256 incorrecta            -> restore debe rechazar el respaldo
#   3. Archivo truncado                   -> pg_restore debe fallar; limpieza de temporal
#   4. Falta de espacio (proxy: directorio de salida no escribible) -> backup debe fallar limpio
#   5. PostgreSQL no disponible           -> backup y restore deben fallar; recuperacion al volver
#   6. Migracion incompatible (Flyway V999 en BD restaurada) -> backend NO debe arrancar
# Exit 0 si todas pasan; exit 1 si alguna falla.
# =============================================================================
param(
  [string]$BackupDir = ".\backups",
  [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = 'Continue'
$Root = Split-Path -Parent $PSScriptRoot
$BackupDir = [System.IO.Path]::GetFullPath((Join-Path $Root $BackupDir))
$results = [ordered]@{}
$failed = $false
$TempWork = Join-Path $env:TEMP "recovery_negative_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Force -Path $TempWork | Out-Null

function Report {
  param([string]$Test, [bool]$Ok, [string]$Detail)
  $results[$Test] = if ($Ok) { "PASS" } else { "FAIL" }
  if (-not $Ok) { $script:failed = $true }
  $color = if ($Ok) { "Green" } else { "Red" }
  Write-Host ("[{0}] {1}: {2}" -f $(if ($Ok) { "PASS" } else { "FAIL" }), $Test, $Detail) -ForegroundColor $color
}

function Get-TempDbCount {
  docker exec asistente-postgres psql -U assistant -d postgres -t -A -c "SELECT count(*) FROM pg_database WHERE datname LIKE 'asistente_whatsapp_restore_%';" 2>$null | Out-String | ForEach-Object { $_.Trim() }
}

function Invoke-RecoveryScript {
  param([string]$Path)
  try {
    & $Path @args *> $null
    return $LASTEXITCODE
  } catch {
    return 1
  }
}

# ---------- Preparacion: respaldo real de referencia ----------
Write-Host "Preparando respaldo de referencia..." -ForegroundColor Cyan
$realBackup = Get-ChildItem -LiteralPath $BackupDir -File -Filter "asistente_whatsapp_*.dump" -ErrorAction SilentlyContinue |
  Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $realBackup) {
  & (Join-Path $PSScriptRoot "backup-db.ps1") -OutputDir $BackupDir
  if ($LASTEXITCODE -ne 0) { Write-Host "No se pudo generar el respaldo de referencia"; exit 1 }
  $realBackup = Get-ChildItem -LiteralPath $BackupDir -File -Filter "asistente_whatsapp_*.dump" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
Write-Host "Respaldo de referencia: $($realBackup.Name)"

# ---------- 1. Archivo vacio ----------
Write-Host "`n=== 1. Archivo vacio ===" -ForegroundColor Cyan
try {
  $empty = Join-Path $TempWork "empty.dump"
  Set-Content -LiteralPath $empty -Value "" -Encoding ascii
  $h = (Get-FileHash -LiteralPath $empty -Algorithm SHA256).Hash.ToLowerInvariant()
  Set-Content -LiteralPath "$empty.sha256" -Value "$h  empty.dump" -Encoding ascii
  $before = Get-TempDbCount
  $exit = Invoke-RecoveryScript (Join-Path $PSScriptRoot "restore-db.ps1") -BackupFile $empty
  $after = Get-TempDbCount
  Report "archivo_vacio" ($exit -ne 0) "restore exit=$exit (esperado != 0)"
  Report "archivo_vacio_sin_BD_temporal" ($after -eq $before) "BD temporales antes=$before despues=$after"
} catch {
  Report "archivo_vacio" $false "excepcion: $_"
}

# ---------- 2. Suma SHA-256 incorrecta ----------
Write-Host "`n=== 2. Suma SHA-256 incorrecta ===" -ForegroundColor Cyan
try {
  $realSha = Get-Content -LiteralPath "$($realBackup.FullName).sha256" -Raw
  Set-Content -LiteralPath "$($realBackup.FullName).sha256" -Value ("0" * 64 + "  $($realBackup.Name)") -Encoding ascii
  $before = Get-TempDbCount
  try {
    $exit = Invoke-RecoveryScript (Join-Path $PSScriptRoot "restore-db.ps1") -BackupFile $realBackup.FullName
  } finally {
    Set-Content -LiteralPath "$($realBackup.FullName).sha256" -Value $realSha -Encoding ascii
  }
  $after = Get-TempDbCount
  Report "suma_incorrecta" ($exit -ne 0) "restore exit=$exit (esperado != 0)"
  Report "suma_incorrecta_sin_BD_temporal" ($after -eq $before) "BD temporales antes=$before despues=$after"
} catch {
  Report "suma_incorrecta" $false "excepcion: $_"
}

# ---------- 3. Archivo truncado ----------
Write-Host "`n=== 3. Archivo truncado ===" -ForegroundColor Cyan
try {
  $truncated = Join-Path $TempWork "truncated.dump"
  $fs = [System.IO.File]::OpenRead($realBackup.FullName)
  $buf = [byte[]]::new([Math]::Min(1024, $fs.Length))
  $fs.Read($buf, 0, $buf.Length) | Out-Null
  $fs.Dispose()
  [System.IO.File]::WriteAllBytes($truncated, $buf)
  $h = (Get-FileHash -LiteralPath $truncated -Algorithm SHA256).Hash.ToLowerInvariant()
  Set-Content -LiteralPath "$truncated.sha256" -Value "$h  truncated.dump" -Encoding ascii
  $before = Get-TempDbCount
  $exit = Invoke-RecoveryScript (Join-Path $PSScriptRoot "restore-db.ps1") -BackupFile $truncated
  $after = Get-TempDbCount
  Report "archivo_truncado" ($exit -ne 0) "restore exit=$exit (esperado != 0, pg_restore debe fallar)"
  Report "archivo_truncado_limpieza" ($after -eq $before) "BD temporales antes=$before despues=$after"
} catch {
  Report "archivo_truncado" $false "excepcion: $_"
}

# ---------- 4. Falta de espacio (proxy: directorio no escribible) ----------
Write-Host "`n=== 4. Falta de espacio (proxy: directorio no escribible) ===" -ForegroundColor Cyan
try {
  $blocker = Join-Path $TempWork "blocker.txt"
  Set-Content -LiteralPath $blocker -Value "x" -Encoding ascii
  $badDir = Join-Path $blocker "sub"
  $exit = Invoke-RecoveryScript (Join-Path $PSScriptRoot "backup-db.ps1") -OutputDir $badDir
  Report "falta_espacio" ($exit -ne 0) "backup exit=$exit (esperado != 0, fallo de escritura manejado limpio)"
} catch {
  Report "falta_espacio" $false "excepcion: $_"
}

# ---------- 5. PostgreSQL no disponible ----------
Write-Host "`n=== 5. PostgreSQL no disponible ===" -ForegroundColor Cyan
try {
  docker stop asistente-postgres 2>$null | Out-Null
  Start-Sleep -Seconds 3
  $exitBk = Invoke-RecoveryScript (Join-Path $PSScriptRoot "backup-db.ps1") -OutputDir $BackupDir
  $exitRs = Invoke-RecoveryScript (Join-Path $PSScriptRoot "restore-db.ps1") -BackupFile $realBackup.FullName
  Report "postgres_caido_backup" ($exitBk -ne 0) "backup exit=$exitBk (esperado != 0)"
  Report "postgres_caido_restore" ($exitRs -ne 0) "restore exit=$exitRs (esperado != 0)"

  docker start asistente-postgres 2>$null | Out-Null
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  $pgUp = $false
  while ((Get-Date) -lt $deadline) {
    docker exec asistente-postgres pg_isready -U assistant -d asistente_whatsapp -h localhost 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $pgUp = $true; break }
    Start-Sleep -Seconds 5
  }
  Report "postgres_recuperado" $pgUp "pg_isready OK tras el reinicio"

  $backendUp = $false
  while ((Get-Date) -lt $deadline) {
    try {
      $h = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 5
      if ($h.status -eq "UP") { $backendUp = $true; break }
    } catch { }
    Start-Sleep -Seconds 5
  }
  Report "backend_recuperado" $backendUp "backend /actuator/health UP tras el reinicio de postgres"
} catch {
  Report "postgres_caido" $false "excepcion: $_"
  docker start asistente-postgres 2>$null | Out-Null
}

# ---------- 6. Migracion incompatible (Flyway V999) ----------
Write-Host "`n=== 6. Migracion incompatible (Flyway V999) ===" -ForegroundColor Cyan
try {
  $freshBackup = $realBackup
  $exitBk = Invoke-RecoveryScript (Join-Path $PSScriptRoot "backup-db.ps1") -OutputDir $BackupDir
  if ($exitBk -ne 0) { throw "backup de referencia fallo (exit=$exitBk); no se puede probar migracion incompatible" }
  $freshBackup = Get-ChildItem -LiteralPath $BackupDir -File -Filter "asistente_whatsapp_*.dump" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  Write-Host "Backup fresco para test 6: $($freshBackup.Name)"
  $exitRestore = Invoke-RecoveryScript (Join-Path $PSScriptRoot "restore-db.ps1") -BackupFile $freshBackup.FullName
  if ($exitRestore -ne 0) { throw "restore de referencia fallo (exit=$exitRestore); no se puede probar migracion incompatible" }
  $tempDb = (docker exec asistente-postgres psql -U assistant -d postgres -t -A -c "SELECT datname FROM pg_database WHERE datname LIKE 'asistente_whatsapp_restore_%' ORDER BY datname DESC LIMIT 1;" | Out-String).Trim()
  docker exec asistente-postgres psql -U assistant -d $tempDb -c "UPDATE flyway_schema_history SET version='999' WHERE installed_rank=(SELECT max(installed_rank) FROM flyway_schema_history);" *> $null
  $exitBe = Invoke-RecoveryScript (Join-Path $PSScriptRoot "restore-backend-check.ps1") -DbName $tempDb -ExpectFailure -FailurePattern "flyway" -TimeoutSeconds $TimeoutSeconds
  Report "migracion_incompatible" ($exitBe -eq 0) "backend-check exit=$exitBe (esperado 0: fallo esperado confirmado)"
  docker exec asistente-postgres psql -U assistant -d postgres -c "DROP DATABASE IF EXISTS $tempDb;" *> $null
} catch {
  Report "migracion_incompatible" $false "excepcion: $_"
}

# ---------- Resumen ----------
Write-Host "`n=== RESUMEN PRUEBAS NEGATIVAS ===" -ForegroundColor Cyan
foreach ($k in $results.Keys) {
  Write-Host ("  {0,-38} {1}" -f $k, $results[$k]) -ForegroundColor $(if ($results[$k] -eq "PASS") { "Green" } else { "Red" })
}
Remove-Item -LiteralPath $TempWork -Recurse -Force -ErrorAction SilentlyContinue
if ($failed) { Write-Host "PRUEBAS NEGATIVAS: FALLIDAS (exit 1)" -ForegroundColor Red; exit 1 }
Write-Host "PRUEBAS NEGATIVAS: TODAS APROBADAS (exit 0)" -ForegroundColor Green
exit 0
