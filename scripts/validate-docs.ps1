# Validación automática de la documentación del repositorio.
# Verifica:
#   1. Referencias a scripts del repositorio (scripts\*.ps1, scripts\*.sh, ./scripts/*, scripts/*)
#   2. Referencias a migraciones Flyway (V###__nombre.sql, R*__nombre.sql)
#   3. Enlaces relativos Markdown (](ruta)) a archivos que existen
#   4. URLs locales que apuntan a puertos no expuestos por docker-compose.local.yml
# Uso: .\scripts\validate-docs.ps1
# Salida: lista de problemas; exit code 1 si hay errores, 0 si todo válido.

param(
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$errors = @()
$warnings = @()

$mdFiles = Get-ChildItem -Path $root -Recurse -Filter '*.md' -File |
    Where-Object {
        $_.FullName -notmatch 'node_modules|target|dist|\.git' -and
        $_.FullName -notmatch '\\docs\\cambios\\' -and
        $_.FullName -notmatch '\\database\\'
    } |
    Where-Object {
        $rel = $_.FullName.Substring($root.Length + 1)
        # archivos ignorados por git (evidencia, memoria, capturas)
        git -C $root check-ignore -q $rel 2>$null
        if ($LASTEXITCODE -eq 0) { return $false }
        # archivos marcados como históricos
        $head = (Get-Content -LiteralPath $_.FullName -TotalCount 4) -join "`n"
        if ($head -match 'ESTADO: ?HIST') { return $false }
        return $true
    }

# Puertos expuestos por docker-compose.local.yml (host:container)
$composePorts = @{}
foreach ($pattern in @('-\s*"?(\d{2,5}):(\d{2,5})"?\r?$', '-\s*"?\d{1,3}(?:\.\d{1,3}){3}:(\d{2,5}):(\d{2,5})"?\r?$')) {
    Select-String -Path (Join-Path $root 'docker-compose.local.yml') -Pattern $pattern -AllMatches |
        ForEach-Object {
            foreach ($m in $_.Matches) {
                $composePorts[$m.Groups[1].Value] = $m.Groups[2].Value
            }
        }
}

$scriptsDir = Join-Path $root 'scripts'
$migrationDir = Join-Path $root 'backend-java\src\main\resources\db\migration'

foreach ($file in $mdFiles) {
    $rel = $file.FullName.Substring($root.Length + 1)
    $content = Get-Content -LiteralPath $file.FullName -Raw

    # 1. Referencias a scripts
    foreach ($m in [regex]::Matches($content, '(?:scripts[\\/])([\w\-\.]+\.(?:ps1|sh|py|mjs))')) {
        $name = $m.Groups[1].Value
        $resolved = Join-Path $scriptsDir $name
        if (-not (Test-Path -LiteralPath $resolved)) {
            $errors += "$rel : script inexistente '$($m.Value)' (línea $($content.Substring(0, $m.Index).Split("`n").Count))"
        }
    }

    # 2. Referencias a migraciones Flyway
    foreach ($m in [regex]::Matches($content, '(V\d{1,4}__[\w\.]+\.sql|R__[\w\.]+\.sql)')) {
        $name = $m.Groups[1].Value
        $resolved = Join-Path $migrationDir $name
        if (-not (Test-Path -LiteralPath $resolved)) {
            $errors += "$rel : migración inexistente '$($m.Value)' (línea $($content.Substring(0, $m.Index).Split("`n").Count))"
        }
    }

    # 3. Enlaces relativos Markdown
    foreach ($m in [regex]::Matches($content, '\]\(([^)]+)\)')) {
        $target = $m.Groups[1].Value
        if ($target -match '^(https?://|mailto:|#|data:)') { continue }
        $clean = $target.Split('#')[0].Split('?')[0]
        if ([string]::IsNullOrWhiteSpace($clean)) { continue }
        # anclas internas al mismo archivo
        $linked = [System.IO.Path]::GetFullPath((Join-Path $file.DirectoryName $clean))
        if (-not (Test-Path -LiteralPath $linked)) {
            $errors += "$rel : enlace inexistente '$target' (línea $($content.Substring(0, $m.Index).Split("`n").Count))"
        }
    }

    # 4. URLs locales con puerto no expuesto
    # 4318 = OTLP HTTP interno backend -> Tempo (intencional, no expuesto al host)
    $internalPorts = @('4318')
    foreach ($m in [regex]::Matches($content, 'localhost:(\d{2,5})')) {
        $port = $m.Groups[1].Value
        if ($port -in $internalPorts) { continue }
        if (-not $composePorts.ContainsKey($port) -and $port -notin @('8080', '5173')) {
            $warnings += "$rel : localhost:$port no está expuesto en docker-compose.local.yml (línea $($content.Substring(0, $m.Index).Split("`n").Count))"
        }
    }
}

# Comandos documentados en docs vigentes de la raíz (referencias tipo .\scripts\xxx)
$rootDocs = ($mdFiles | Where-Object { $_.DirectoryName -eq $root } | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join "`n"
foreach ($m in [regex]::Matches($rootDocs, '\.\\scripts\\([\w\-\.]+\.(?:ps1|sh))')) {
    $name = $m.Groups[1].Value
    if (-not (Test-Path -LiteralPath (Join-Path $scriptsDir $name))) {
        $errors += "raíz : script inexistente '.\scripts\$name'"
    }
}

if (-not $Quiet) {
    Write-Host ''
    if ($errors.Count -gt 0) {
        Write-Host "ERRORES ($($errors.Count)):" -ForegroundColor Red
        $errors | Sort-Object -Unique | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    }
    if ($warnings.Count -gt 0) {
        Write-Host "ADVERTENCIAS ($($warnings.Count)):" -ForegroundColor Yellow
        $warnings | Sort-Object -Unique | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    }
    if ($errors.Count -eq 0 -and $warnings.Count -eq 0) {
        Write-Host "Validación completa: $($mdFiles.Count) archivos .md revisados, sin errores." -ForegroundColor Green
    }
    else {
        Write-Host "Validación: $($mdFiles.Count) archivos .md revisados, $($errors.Count) errores, $($warnings.Count) advertencias." -ForegroundColor Cyan
    }
}

exit $(if ($errors.Count -gt 0) { 1 } else { 0 })
