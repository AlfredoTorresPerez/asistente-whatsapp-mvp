param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AnalyzePath = "/api/v1/esthetic/intent/analyze",
    [string]$LoginPath = "/api/v1/auth/login",
    [string]$Email = "admin@demo.cl",
    [string]$Password = "Cambiar123!",
    [string]$Token = "",
    [int]$MinConfidenceDefault = 50
)

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

function Repair-TextEncoding([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return "" }
    $fixed = [string]$Value
    try {
        if ($fixed -match "Ã|Â|â") {
            $fixed = [System.Text.Encoding]::UTF8.GetString([System.Text.Encoding]::GetEncoding("ISO-8859-1").GetBytes($fixed))
        }
    } catch { }
    return $fixed
}

function Normalize-Text([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return "" }
    $Value = Repair-TextEncoding $Value
    $lower = $Value.ToLowerInvariant()
    $formD = $lower.Normalize([Text.NormalizationForm]::FormD)
    $builder = New-Object System.Text.StringBuilder
    foreach ($ch in $formD.ToCharArray()) {
        $cat = [Globalization.CharUnicodeInfo]::GetUnicodeCategory($ch)
        if ($cat -ne [Globalization.UnicodeCategory]::NonSpacingMark) { [void]$builder.Append($ch) }
    }
    $plain = $builder.ToString().Normalize([Text.NormalizationForm]::FormC)
    $plain = $plain.Replace("¿", "").Replace("?", "").Replace("¡", "").Replace("!", "")
    return $plain
}

function Html-Encode([string]$Value) {
    if ($null -eq $Value) { return "" }
    return [System.Net.WebUtility]::HtmlEncode($Value)
}

function Get-Field($Object, [string[]]$Names) {
    foreach ($name in $Names) {
        if ($null -ne $Object.$name) { return [string]$Object.$name }
    }
    return ""
}

function Convert-ConfidenceToPercent($Value) {
    if ($null -eq $Value -or [string]::IsNullOrWhiteSpace([string]$Value)) { return 0 }
    $number = [double]$Value
    if ($number -le 1) { return [int][Math]::Round($number * 100) }
    return [int][Math]::Round($number)
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    $loginUrl = "$BaseUrl$LoginPath"
    Write-Host "Autenticando en $loginUrl ..."
    $loginBody = @{ email = $Email; password = $Password } | ConvertTo-Json -Depth 5
    try {
        $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -ContentType "application/json; charset=utf-8" -Body $loginBody
        $Token = Get-Field $loginResponse @("accessToken", "token", "jwt")
        if ([string]::IsNullOrWhiteSpace($Token)) { throw "No se encontro token en la respuesta de autenticacion." }
    } catch {
        Write-Host "ERROR autenticando: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

$headers = @{ Authorization = "Bearer $Token"; Accept = "application/json" }
$analyzeUrl = "$BaseUrl$AnalyzePath"

$cases = @(
    @{ Id="T01"; Categoria="Contacto sin sucursal con Providencia"; Pregunta="Hola, quiero reservar limpieza facial profunda manana a las 10:00 en Providencia"; Intent="reservar_hora"; Min=70; Must=@("providencia", "limpieza facial profunda", "10:00"); MustAny=@("reservas/confirmar", "no encontre disponibilidad", "no esta configurado"); MustNot=@("si falta sucursal", "que sucursal", "en que sucursal", "404", "api client error") },
    @{ Id="T02"; Categoria="Contacto sin sucursal sin sede"; Pregunta="Quiero reservar limpieza facial profunda manana a las 10:00"; Intent="reservar_hora"; Min=70; Must=@("sucursal"); MustNot=@("reservas/confirmar", "404", "api client error") },
    @{ Id="T03"; Categoria="Mensaje prioriza Providencia sobre respaldo"; Pregunta="Quiero reservar limpieza facial profunda manana a las 10:00 en Providencia"; Intent="reservar_hora"; Min=70; Must=@("providencia", "limpieza facial profunda", "10:00"); MustAny=@("reservas/confirmar", "no encontre disponibilidad", "no esta configurado"); MustNot=@("maipu", "que sucursal", "en que sucursal", "404") },
    @{ Id="T04"; Categoria="Reserva con sede explicita y otro servicio"; Pregunta="Quiero reservar depilacion bozo manana a las 14:00 en Providencia"; Intent="reservar_hora"; Min=70; Must=@("providencia", "depilacion bozo", "14:00"); MustAny=@("reservas/confirmar", "no encontre disponibilidad", "no esta configurado"); MustNot=@("que sucursal", "en que sucursal", "404") },
    @{ Id="T05"; Categoria="Servicio no configurado en sucursal"; Pregunta="Quiero reservar un servicio no disponible manana a las 10:00 en Providencia"; Intent="reservar_hora"; Min=50; MustAny=@("servicio", "no esta configurado", "que servicio quieres"); MustNot=@("404", "api client error") },
    @{ Id="T06"; Categoria="Reenvio enlace"; Pregunta="No me llego el link de confirmacion, me lo puedes reenviar?"; Intent="reenviar_enlace_confirmacion"; Min=70; Must=@("enlace", "reserva"); MustNot=@("que servicio quieres") },
    @{ Id="T07"; Categoria="Cancelacion"; Pregunta="Necesito cancelar mi cita de manana"; Intent="cancelar_reserva"; Min=70; Must=@("cancelar", "reserva"); MustNot=@("que servicio quieres") },
    @{ Id="T08"; Categoria="Caso sensible"; Pregunta="Tuve una reaccion en la piel despues del tratamiento"; Intent="caso_sensible_post_tratamiento"; Min=70; Must=@("derivar", "persona"); MustNot=@("que servicio quieres") }
)
Write-Host ""
Write-Host "Ejecutando test funcional V23.4.2 contra: $analyzeUrl"
Write-Host "Casos: $($cases.Count)"

$results = New-Object System.Collections.Generic.List[object]
foreach ($case in $cases) {
    Write-Host ""
    Write-Host "[$($case.Id)] $($case.Categoria)" -ForegroundColor Cyan
    Write-Host "Cliente: $($case.Pregunta)"
    $body = @{ message = $case.Pregunta } | ConvertTo-Json -Depth 5
    try {
        $response = Invoke-RestMethod -Uri $analyzeUrl -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $body
        $intent = Repair-TextEncoding (Get-Field $response @("intencion", "intent"))
        $confidence = Convert-ConfidenceToPercent (Get-Field $response @("confianza", "confidence"))
        $answer = Repair-TextEncoding (Get-Field $response @("respuestaSugerida", "suggestedResponse", "respuesta"))
        $service = ""; $date = ""; $hour = ""
        if ($null -ne $response.entidades) {
            $service = Repair-TextEncoding (Get-Field $response.entidades @("servicio", "servicio_o_producto"))
            $date = Repair-TextEncoding (Get-Field $response.entidades @("fecha", "fecha_relativa"))
            $hour = Repair-TextEncoding (Get-Field $response.entidades @("hora", "hora_exacta"))
        }

        $normAnswer = Normalize-Text $answer
        $normIntent = Normalize-Text $intent
        $motives = New-Object System.Collections.Generic.List[string]

        if ($case.ContainsKey("Intent")) {
            if ($normIntent -ne (Normalize-Text $case.Intent)) { [void]$motives.Add("Intencion inesperada: $intent") }
        }
        if ($confidence -lt $case.Min) { [void]$motives.Add("Confianza baja: $confidence% menor a $($case.Min)%") }
        foreach ($expected in $case.Must) {
            if ($normAnswer -notlike "*$(Normalize-Text $expected)*") { [void]$motives.Add("No contiene: $expected") }
        }
        if ($case.ContainsKey("MustAny")) {
            $okAny = $false
            foreach ($expectedAny in $case.MustAny) {
                if ($normAnswer -like "*$(Normalize-Text $expectedAny)*") { $okAny = $true }
            }
            if (-not $okAny) { [void]$motives.Add("No contiene ninguna alternativa esperada: $($case.MustAny -join ', ')") }
        }
        foreach ($forbidden in $case.MustNot) {
            if ($normAnswer -like "*$(Normalize-Text $forbidden)*") { [void]$motives.Add("Contiene texto prohibido: $forbidden") }
        }
        $evaluation = if ($motives.Count -eq 0) { "BIEN" } else { "MAL" }
        $motiveText = if ($motives.Count -eq 0) { "OK" } else { $motives -join " | " }

        Write-Host "Resultado: $evaluation - Confianza $confidence%"
        Write-Host "Intencion: $intent"
        Write-Host "Respuesta IA: $answer"

        [void]$results.Add([pscustomobject]@{
            Caso=$case.Id; Categoria=$case.Categoria; PreguntaCliente=$case.Pregunta; IntencionIA=$intent; PorcentajeIA="$confidence%"; Evaluacion=$evaluation; Motivos=$motiveText; ServicioDetectado=$service; FechaDetectada=$date; HoraDetectada=$hour; RespuestaIA=$answer
        })
    } catch {
        [void]$results.Add([pscustomobject]@{ Caso=$case.Id; Categoria=$case.Categoria; PreguntaCliente=$case.Pregunta; IntencionIA="ERROR"; PorcentajeIA="0%"; Evaluacion="MAL"; Motivos=$_.Exception.Message; ServicioDetectado=""; FechaDetectada=""; HoraDetectada=""; RespuestaIA="" })
    }
}

$outDir = Join-Path (Get-Location) "resultados-test-ia-v23-4-2"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$csvPath = Join-Path $outDir "resultado_test_ia_negocio_v23_4_2_$timestamp.csv"
$jsonPath = Join-Path $outDir "resultado_test_ia_negocio_v23_4_2_$timestamp.json"
$htmlPath = Join-Path $outDir "resultado_test_ia_negocio_v23_4_2_$timestamp.html"

$results | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath
$results | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 -Path $jsonPath

$rows = foreach ($r in $results) {
    $class = if ($r.Evaluacion -eq "BIEN") { "ok" } else { "bad" }
    "<tr class='$class'><td>$(Html-Encode $r.Caso)</td><td>$(Html-Encode $r.Categoria)</td><td><pre>$(Html-Encode $r.PreguntaCliente)</pre></td><td>$(Html-Encode $r.IntencionIA)</td><td>$(Html-Encode $r.PorcentajeIA)</td><td>$(Html-Encode $r.Evaluacion)</td><td><pre>$(Html-Encode $r.Motivos)</pre></td><td>$(Html-Encode $r.ServicioDetectado)</td><td>$(Html-Encode $r.FechaDetectada)</td><td>$(Html-Encode $r.HoraDetectada)</td><td><pre>$(Html-Encode $r.RespuestaIA)</pre></td></tr>"
}
$html = @"
<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8" />
<title>Resultado test IA negocio V23.4.2</title>
<style>
body { font-family: Arial, sans-serif; margin: 24px; color: #172033; }
table { border-collapse: collapse; width: 100%; font-size: 13px; }
th, td { border: 1px solid #d8dee9; padding: 8px; vertical-align: top; }
th { background: #eef3ff; text-align: left; }
tr.ok { background: #ecfdf3; }
tr.bad { background: #fff1f2; }
pre { white-space: pre-wrap; margin: 0; font-family: Arial, sans-serif; }
</style>
</head>
<body>
<h1>Resultado test IA negocio V23.4.2</h1>
<p>Fecha: $(Html-Encode (Get-Date))</p>
<table>
<thead><tr><th>Caso</th><th>Categoria</th><th>Pregunta cliente</th><th>Intencion IA</th><th>Porcentaje IA</th><th>Evaluacion</th><th>Motivos</th><th>Servicio</th><th>Fecha</th><th>Hora</th><th>Respuesta IA</th></tr></thead>
<tbody>
$($rows -join "`n")
</tbody>
</table>
</body>
</html>
"@
Set-Content -Encoding UTF8 -Path $htmlPath -Value $html

$ok = ($results | Where-Object { $_.Evaluacion -eq "BIEN" }).Count
$bad = ($results | Where-Object { $_.Evaluacion -eq "MAL" }).Count
Write-Host ""
Write-Host "Resumen: BIEN=$ok MAL=$bad"
Write-Host "CSV:  $csvPath"
Write-Host "JSON: $jsonPath"
Write-Host "HTML: $htmlPath"
