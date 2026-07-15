# =============================================================================
# SMOKE TEST - Verifica que todos los servicios respondan correctamente
# Uso: .\scripts\smoke-test.ps1
# =============================================================================

$FAILED = 0

function Check-Service {
    param($Name, $Url, $Expected)
    Write-Host -NoNewline "  [$Name] $Url ... "
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 5 -UseBasicParsing
        if ($response.StatusCode -eq $Expected) {
            Write-Host "OK ($($response.StatusCode))"
        } else {
            Write-Host "FAIL (expected $Expected, got $($response.StatusCode))"
            $script:FAILED++
        }
    } catch {
        Write-Host "FAIL (error: $($_.Exception.Message))"
        $script:FAILED++
    }
}

function Check-Body {
    param($Name, $Url, $Pattern)
    Write-Host -NoNewline "  [$Name] $Url ... "
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 5 -UseBasicParsing
        if ($response.Content -match $Pattern) {
            Write-Host "OK"
        } else {
            Write-Host "FAIL (no contiene '$Pattern')"
            Write-Host "    Response: $($response.Content.Substring(0, [Math]::Min(200, $response.Content.Length)))"
            $script:FAILED++
        }
    } catch {
        Write-Host "FAIL (error: $($_.Exception.Message))"
        $script:FAILED++
    }
}

Write-Host "=== Smoke Test: Asistente WhatsApp ==="
Write-Host ""

Write-Host "--- Backend ---"
Check-Service "Health"  "http://localhost:8080/actuator/health" 200
Check-Body    "Status"  "http://localhost:8080/api/v1/health"   "UP"

Write-Host "--- Frontend ---"
Check-Service "Frontend" "http://localhost:5173/" 200

Write-Host ""
if ($FAILED -eq 0) {
    Write-Host "=== RESULTADO: Todos los servicios responden correctamente ==="
    exit 0
} else {
    Write-Host "=== RESULTADO: $FAILED servicio(s) fallaron ==="
    exit 1
}
