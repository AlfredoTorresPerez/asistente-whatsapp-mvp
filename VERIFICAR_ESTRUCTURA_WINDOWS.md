# Verificacion de estructura en Windows

Este paquete deja `docker-compose.local.yml`, `frontend-react`, `backend-java` y `whatsapp-web-service` en la misma carpeta.

Antes de ejecutar Docker, validar desde PowerShell:

```powershell
Get-ChildItem
Test-Path .\frontend-react
Test-Path .\backend-java
Test-Path .\whatsapp-web-service
Test-Path .\docker-compose.local.yml
```

Todos los `Test-Path` deben devolver `True`.

Luego ejecutar:

```powershell
docker compose -f .\docker-compose.local.yml up -d --build
```
