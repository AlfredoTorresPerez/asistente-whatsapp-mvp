# Test V23.4.9 - CTA local para enlace de confirmacion WhatsApp
# Ejecutar desde la raiz del proyecto:
# powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_9.ps1

$ErrorActionPreference = "Stop"
$OutDir = "resultados-test-ia-v23-4-9"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "V23.4.9 - Validacion de CTA para enlace localhost en WhatsApp" -ForegroundColor Cyan
Write-Host ""
Write-Host "Flujo manual recomendado:" -ForegroundColor Yellow
Write-Host "1. Cliente: Hola, quiero reservar limpieza facial profunda viernes a las 12:00 en Providencia"
Write-Host "2. Presionar: Responder con IA"
Write-Host "3. Presionar: Enviar"
Write-Host "4. Confirmar WhatsApp final: ✅ *Reserva temporal creada* + CTA 'Toca o copia este enlace' + localhost"
Write-Host ""

$checks = @(
    [pscustomobject]@{
        Id = "T01"
        Nombre = "Reserva temporal real creada"
        Esperado = "se crea booking temporal y enlace"
        Logs = "TEMPORARY_BOOKING_CREATED; CONFIRMATION_LINK_CREATED"
    },
    [pscustomobject]@{
        Id = "T02"
        Nombre = "Formato destacado"
        Esperado = "mensaje contiene ✅ *Reserva temporal creada*"
        Logs = "WHATSAPP_MESSAGE_FORMATTED type=TEMPORARY_BOOKING"
    },
    [pscustomobject]@{
        Id = "T03"
        Nombre = "CTA local claro"
        Esperado = "mensaje contiene 'Toca o copia este enlace para confirmar tu reserva'"
        Logs = "responseText=Toca o copia este enlace para confirmar tu reserva"
    },
    [pscustomobject]@{
        Id = "T04"
        Nombre = "URL localhost conservada"
        Esperado = "mensaje mantiene http://localhost:5173/reservas/confirmar/"
        Logs = "http://localhost:5173/reservas/confirmar/"
    },
    [pscustomobject]@{
        Id = "T05"
        Nombre = "Instruccion alternativa"
        Esperado = "mensaje indica copiar y pegar si WhatsApp no abre el enlace"
        Logs = "Si WhatsApp no lo abre al tocarlo, copia el enlace"
    }
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$csvPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_9_$timestamp.csv"
$jsonPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_9_$timestamp.json"
$htmlPath = Join-Path $OutDir "resultado_test_ia_negocio_v23_4_9_$timestamp.html"

$checks | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath
$checks | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 -Path $jsonPath

$htmlRows = ($checks | ForEach-Object { "<tr><td>$($_.Id)</td><td>$($_.Nombre)</td><td>$($_.Esperado)</td><td><code>$($_.Logs)</code></td></tr>" }) -join "`n"
$html = @"
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>Test V23.4.9</title></head>
<body>
<h1>Test V23.4.9 - CTA local para enlace WhatsApp</h1>
<p>Este archivo define las verificaciones de aceptacion para confirmar que el mensaje final explica que el cliente debe tocar o copiar el enlace localhost.</p>
<pre>docker compose -f docker-compose.local.yml logs --tail=3000 backend-java | Select-String -Pattern "TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","WHATSAPP_MESSAGE_FORMATTED","Toca o copia","localhost","WHATSAPP_RESPONSE_SEND_RESULT","FLOW_ERROR"</pre>
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
Write-Host 'docker compose -f docker-compose.local.yml logs --tail=3000 backend-java | Select-String -Pattern "TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","WHATSAPP_MESSAGE_FORMATTED","Toca o copia","localhost","WHATSAPP_RESPONSE_SEND_RESULT","FLOW_ERROR"'
