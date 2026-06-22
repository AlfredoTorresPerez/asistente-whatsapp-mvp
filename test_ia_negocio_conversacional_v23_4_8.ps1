# Test V23.4.8 - Envio real desde vista previa de reserva
# Ejecutar desde la raiz del proyecto:
# powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_8.ps1

$ErrorActionPreference = "Stop"
$OutDir = "resultados-test-ia-v23-4-8"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "V23.4.8 - Validacion de envio real desde BOOKING_PREVIEW" -ForegroundColor Cyan
Write-Host ""
Write-Host "Flujo manual requerido:" -ForegroundColor Yellow
Write-Host "1. Cliente: Hola, quiero reservar limpieza facial profunda jueves a las 10:00 en Providencia"
Write-Host "2. Presionar: Responder con IA"
Write-Host "3. Confirmar preview: 👀 *Vista previa de reserva*"
Write-Host "4. Presionar: Enviar"
Write-Host "5. Confirmar WhatsApp final: ✅ *Reserva temporal creada* + /reservas/confirmar/"
Write-Host ""

$checks = @(
    [pscustomobject]@{
        Id = "T01"
        Nombre = "Preview booking detectado"
        Esperado = "preview-ai genera BOOKING_PREVIEW y no genera enlace real"
        Logs = "TEMPORARY_BOOKING_DRY_RUN; WHATSAPP_MESSAGE_FORMATTED type=BOOKING_PREVIEW"
    },
    [pscustomobject]@{
        Id = "T02"
        Nombre = "Envio real desde preview"
        Esperado = "al presionar Enviar aparece AI_REAL_SEND_STARTED y dryRun=false"
        Logs = "AI_REAL_SEND_STARTED; TRANSACTIONAL_BOOKING_STARTED dryRun=false"
    },
    [pscustomobject]@{
        Id = "T03"
        Nombre = "Reserva temporal creada"
        Esperado = "se crea booking temporal"
        Logs = "TEMPORARY_BOOKING_CREATE_STARTED; TEMPORARY_BOOKING_CREATED"
    },
    [pscustomobject]@{
        Id = "T04"
        Nombre = "Enlace generado"
        Esperado = "se genera enlace de confirmacion"
        Logs = "CONFIRMATION_LINK_CREATED; WHATSAPP_MESSAGE_FORMATTED type=TEMPORARY_BOOKING"
    },
    [pscustomobject]@{
        Id = "T05"
        Nombre = "Envio final correcto"
        Esperado = "WhatsApp recibe mensaje final con enlace, no texto preview"
        Logs = "WHATSAPP_RESPONSE_SEND_RESULT sent=true; containsLink=true"
    }
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$csvPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_8_$timestamp.csv"
$jsonPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_8_$timestamp.json"
$htmlPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_8_$timestamp.html"

$checks | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath
$checks | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 -Path $jsonPath

$htmlRows = ($checks | ForEach-Object { "<tr><td>$($_.Id)</td><td>$($_.Nombre)</td><td>$($_.Esperado)</td><td><code>$($_.Logs)</code></td></tr>" }) -join "`n"
$html = @"
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>Test V23.4.8</title></head>
<body>
<h1>Test V23.4.8 - Envio real desde vista previa</h1>
<p>Este archivo define las verificaciones de aceptacion para confirmar que BOOKING_PREVIEW se convierte en envio real transaccional.</p>
<pre>docker compose -f docker-compose.local.yml logs --tail=3000 backend-java | Select-String -Pattern "AI_REAL_SEND","dryRun=false","TEMPORARY_BOOKING_CREATE_STARTED","TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","WHATSAPP_MESSAGE_FORMATTED","WHATSAPP_RESPONSE_SEND_RESULT","FLOW_ERROR"</pre>
<table border="1" cellspacing="0" cellpadding="6">
<tr><th>Id</th><th>Nombre</th><th>Esperado</th><th>Logs</th></tr>
$htmlRows
</table>
</body>
</html>
"@
$html | Set-Content -Encoding UTF8 -Path $htmlPath

Write-Host "Archivos generados:" -ForegroundColor Green
Write-Host $csvPath
Write-Host $jsonPath
Write-Host $htmlPath
Write-Host ""
Write-Host "Comando recomendado:" -ForegroundColor Yellow
Write-Host 'docker compose -f docker-compose.local.yml logs --tail=3000 backend-java | Select-String -Pattern "AI_REAL_SEND","dryRun=false","TEMPORARY_BOOKING_CREATE_STARTED","TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","WHATSAPP_MESSAGE_FORMATTED","WHATSAPP_RESPONSE_SEND_RESULT","FLOW_ERROR"'
