param(
    [switch]$UpdateEnv
)

$ErrorActionPreference = "Stop"

Write-Output "=== Detectando URL de ngrok ==="

try {
    $tunnels = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -ErrorAction Stop
    $httpsTunnel = $tunnels.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1

    if (-not $httpsTunnel) {
        Write-Output "ERROR: No se encontro un tunel HTTPS activo en ngrok."
        Write-Output "Asegurate de que ngrok este corriendo: ngrok http 8080"
        exit 1
    }

    $publicUrl = $httpsTunnel.public_url.TrimEnd('/')
    $callbackUrl = "$publicUrl/api/v1/integrations/whatsapp-cloud/webhook"
    $backendUrl = $httpsTunnel.config.addr

    Write-Output "URL publica ngrok:  $publicUrl"
    Write-Output "Reenvia a:          $backendUrl"
    Write-Output "Callback webhook:   $callbackUrl"

    Write-Output ""
    Write-Output "=== Verificando webhook GET ==="
    try {
        $response = Invoke-WebRequest -Uri "$callbackUrl?hub.mode=subscribe&hub.verify_token=test&hub.challenge=verificacion" -UseBasicParsing -ErrorAction Stop
        Write-Output "HTTP $($response.StatusCode) - Endpoint webhook responde"
    } catch {
        if ($_.Exception.Response.StatusCode -eq 403) {
            Write-Output "HTTP 403 - Token invalido (esperado, el challenge usa verify_token real)"
        } else {
            Write-Output "HTTP $($_.Exception.Response.StatusCode) - Error inesperado"
        }
    }

    if ($UpdateEnv) {
        $envPath = Join-Path $PSScriptRoot ".." ".env.local"
        if (Test-Path $envPath) {
            $content = Get-Content $envPath -Raw
            $pattern = 'APP_WHATSAPP_CLOUD_API_WEBHOOK_PUBLIC_URL=.*'
            $replacement = "APP_WHATSAPP_CLOUD_API_WEBHOOK_PUBLIC_URL=$publicUrl"
            if ($content -match $pattern) {
                $content = $content -replace $pattern, $replacement
                Set-Content -Path $envPath -Value $content -NoNewline
                Write-Output ""
                Write-Output ".env.local actualizado con: $publicUrl"
                Write-Output "Ejecuta 'docker compose up -d --force-recreate backend-java' para aplicar cambios."
            } else {
                Write-Output "WARN: No se encontro APP_WHATSAPP_CLOUD_API_WEBHOOK_PUBLIC_URL en .env.local"
            }
        } else {
            Write-Output "ERROR: No se encuentra .env.local en $envPath"
        }
    }

    Write-Output ""
    Write-Output "=== Resumen ==="
    Write-Output "URL pública:     $publicUrl"
    Write-Output "Callback completa: $callbackUrl"
    Write-Output "Estado:          OK"
} catch {
    Write-Output "ERROR: No se pudo conectar con la API de ngrok en http://127.0.0.1:4040"
    Write-Output "Detalle: $($_.Exception.Message)"
    exit 1
}
