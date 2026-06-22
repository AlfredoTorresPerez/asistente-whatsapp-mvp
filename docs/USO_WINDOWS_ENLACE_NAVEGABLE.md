# Uso en Windows para enlace navegable

Este proyecto incluye dos formas de iniciar el enlace navegable:

- `scripts/start_mvp_public_link.sh` para Linux, macOS, Git Bash o WSL.
- `scripts/start_mvp_public_link.ps1` para PowerShell en Windows.

## Ejecutar desde PowerShell

Desde la raiz del proyecto:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\start_mvp_public_link.ps1
```

## Validar URL configurada

```powershell
docker compose -f docker-compose.local.yml exec backend-java printenv APP_BOOKING_CONFIRMATION_PUBLIC_BASE_URL
```

Debe mostrar una URL parecida a:

```text
https://xxxx.trycloudflare.com/reservas/confirmar
```

Los enlaces enviados por WhatsApp deberian quedar con este formato:

```text
https://xxxx.trycloudflare.com/reservas/confirmar/{token}
```

## Ver logs del tunel

```powershell
docker compose -f docker-compose.local.yml logs -f public-tunnel
```
