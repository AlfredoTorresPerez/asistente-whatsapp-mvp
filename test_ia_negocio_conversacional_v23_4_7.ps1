# Test V23.4.7 - Diagnostico de disponibilidad exacta
# Ejecutar desde la raiz del proyecto:
# powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_7.ps1

$ErrorActionPreference = "Stop"
$BaseUrl = $env:API_BASE_URL
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = "http://localhost:8080" }

$OutDir = "resultados-test-ia-v23-4-7"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "V23.4.7 - Prueba de diagnostico de disponibilidad exacta" -ForegroundColor Cyan
Write-Host "API: $BaseUrl"
Write-Host ""

$cases = @(
    [pscustomobject]@{
        Id = "T01"
        Categoria = "Reserva exacta 10:00 Providencia"
        Mensaje = "Hola, quiero reservar limpieza facial profunda mañana a las 10:00 en Providencia"
        Esperado = "Debe generar reserva si 10:00 esta disponible; si no, debe mostrar horario no disponible y logs EXACT_SLOT_REJECTED"
    },
    [pscustomobject]@{
        Id = "T02"
        Categoria = "Falta servicio"
        Mensaje = "Puedes agendarme para mañana a las 10:00"
        Esperado = "Debe preguntar servicio y no crear reserva"
    },
    [pscustomobject]@{
        Id = "T03"
        Categoria = "Reserva con hora alternativa sugerida"
        Mensaje = "Quiero reservar limpieza facial profunda mañana a las 11:00 en Providencia"
        Esperado = "Debe crear reserva si 11:00 esta disponible o explicar rechazo exacto"
    }
)

$results = @()
foreach ($case in $cases) {
    Write-Host "Caso $($case.Id): $($case.Mensaje)"
    $results += [pscustomobject]@{
        Caso = $case.Id
        Categoria = $case.Categoria
        PreguntaCliente = $case.Mensaje
        Esperado = $case.Esperado
        Observacion = "Ejecutar contra endpoint de IA configurado en el entorno local. Revisar logs EXACT_SLOT en backend-java."
    }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$csvPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_7_$timestamp.csv"
$jsonPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_7_$timestamp.json"
$htmlPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_7_$timestamp.html"

$results | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath
$results | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 -Path $jsonPath

$html = @"
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>Resultado test IA negocio V23.4.7</title></head>
<body>
<h1>Resultado test IA negocio V23.4.7</h1>
<p>Este test define casos de validacion. La confirmacion real se realiza revisando logs EXACT_SLOT en backend-java.</p>
<pre>docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "EXACT_SLOT","AVAILABILITY_CHECK_RESULT","TEMPORARY_BOOKING","CONFIRMATION_LINK","FLOW_ERROR"</pre>
<table border="1" cellspacing="0" cellpadding="6">
<tr><th>Caso</th><th>Categoria</th><th>Pregunta cliente</th><th>Esperado</th></tr>
$($results | ForEach-Object { "<tr><td>$($_.Caso)</td><td>$($_.Categoria)</td><td>$($_.PreguntaCliente)</td><td>$($_.Esperado)</td></tr>" })
</table>
</body>
</html>
"@
$html | Set-Content -Encoding UTF8 -Path $htmlPath

Write-Host ""
Write-Host "Archivos generados:" -ForegroundColor Green
Write-Host $csvPath
Write-Host $jsonPath
Write-Host $htmlPath
Write-Host ""
Write-Host "Comando recomendado para revisar trazas:" -ForegroundColor Yellow
Write-Host 'docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "EXACT_SLOT","AVAILABILITY_CHECK_RESULT","TEMPORARY_BOOKING","CONFIRMATION_LINK","FLOW_ERROR"'
