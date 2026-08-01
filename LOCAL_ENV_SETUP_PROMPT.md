# Prompt: Consideraciones para levantar ambiente local

## 1. Prerrequisitos de herramientas

| Herramienta | Versión requerida | Verificar con |
|---|---|---|
| Docker Desktop | Cualquiera reciente | `docker ps` |
| Java JDK | 21+ | `java -version` |
| Maven | 3.9+ | `mvn --version` |
| Node.js | 20+ | `node --version` |
| pnpm | 10+ | `pnpm --version` |

**⚠️ Docker Engine debe estar corriendo** antes de cualquier paso. Si `docker ps` falla, abrir Docker Desktop y esperar a que el engine inicie.

## 2. Secretos en Windows Credential Manager

Los siguientes secretos **no van en archivos**, se almacenan cifrados con DPAPI:

| Secreto | Target en Credential Manager | ¿Obligatorio? |
|---|---|---|
| `JWT_SECRET` | `asistente-local/JWT_SECRET` | ✅ Sí |
| `WHATSAPP_APP_SECRET` | `asistente-local/WHATSAPP_APP_SECRET` | ✅ Sí |
| `WHATSAPP_ACCESS_TOKEN` | `asistente-local/WHATSAPP_ACCESS_TOKEN` | ✅ Sí |
| `GMAIL_PASSWORD` | `asistente-local/GMAIL_PASSWORD` | ⚠️ Sí (email) |
| `OPENAI_API_KEY` | `asistente-local/OPENAI_API_KEY` | ⚠️ Sí (IA) |
| `SENTRY_DSN` | `asistente-local/SENTRY_DSN` | ❌ Opcional |

**Verificar estado:**
```powershell
.\scripts\restore-local-secrets.ps1 -WhatIf
```

**Guardar/actualizar:**
```powershell
.\scripts\store-local-secrets.ps1
```

## 3. Variables de entorno mapeadas en docker-compose.local.yml

El archivo `docker-compose.local.yml` debe declarar **explícitamente** toda variable que el contenedor necesite, aunque el valor venga del shell vía `restore-local-secrets.ps1`.

**Verificar que `environment:` del servicio `backend-java` incluya:**

```yaml
APP_JWT_SECRET: ${APP_JWT_SECRET:-}
APP_OPENAI_API_KEY: ${APP_OPENAI_API_KEY:-}
APP_WHATSAPP_CLOUD_API_APP_SECRET: ${APP_WHATSAPP_CLOUD_API_APP_SECRET:-}
APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN: ${APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN:-}
SPRING_MAIL_PASSWORD: ${SPRING_MAIL_PASSWORD:-}
APP_EMAIL_MIRROR_PASSWORD: ${APP_EMAIL_MIRROR_PASSWORD:-}
```

**Docker Compose NO hereda variables del shell automáticamente.** Si una variable no está listada en `environment:`, el contenedor no la recibe, aunque esté exportada en el shell.

## 4. Puerto libres (ninguno debe estar en uso)

| Servicio | Puerto |
|---|---|
| PostgreSQL | `5433` |
| Backend Java | `8080` |
| Frontend React | `5173` |
| Mailpit UI | `8025` |
| Mailpit SMTP | `1025` |

```powershell
netstat -ano | Select-String ":5433\s|:8080\s|:5173\s|:8025\s|:1025\s"
```

## 5. Orden de arranque

```powershell
cd C:\mvp\asistente_impl_codex\asistente

# 1. Restaurar secretos (los exporta al shell)
.\scripts\restore-local-secrets.ps1

# 2. Build y up
docker compose --env-file .env.local -f docker-compose.local.yml up --build -d

# Alternativa: script unificado (hace ambos pasos)
.\scripts\local-start.ps1 -Build
```

## 6. Verificación post-arranque

```powershell
docker ps
# Debe mostrar: asistente-postgres, asistente-backend, asistente-frontend, asistente-mailpit
# Todos con estado "(healthy)"

# Backend health
curl http://localhost:8080/actuator/health

# Frontend responde
curl -o /dev/null -w "%{http_code}" http://localhost:5173

# OpenAI key presente en el contenedor
docker inspect asistente-backend --format '{{json .Config.Env}}' | python -c "import sys,json; [print(e) for e in json.load(sys.stdin) if 'OPENAI_API_KEY' in e]"
```

## 7. Perfiles adicionales

| Perfil | Comando | Qué agrega |
|---|---|---|
| `public-link` | `--profile public-link` | Cloudflare Tunnel (trycloudflare.com) |
| `monitoring` | `--profile monitoring` | Prometheus + Grafana |
| `https` | `--profile https` | Caddy con HTTPS local autofirmado |

## 8. Problemas conocidos

| Síntoma | Causa | Solución |
|---|---|---|
| Backend no healthy | OpenAI key no llega al contenedor | Agregar `APP_OPENAI_API_KEY: ${APP_OPENAI_API_KEY:-}` a compose y rebuild |
| JWT falla al hacer login | `APP_JWT_SECRET` no está en contenedor | Verificar que compose declare la variable y que exista en Credential Manager |
| AI Agents no responden | `APP_OPENAI_API_KEY` ausente o `APP_AI_AGENTS_AUTO_REPLY_ENABLED=false` | Verificar env en contenedor + perfil |
| Email no llega | Mailpit caído o `APP_EMAIL_ENABLED=false` | Revisar `docker ps` y env vars |
| Frontend blank | Backend no healthy aún | Esperar 60s (start_period del healthcheck) |
| ngrok URL expirada | `APP_FRONTEND_PUBLIC_BASE_URL` apunta a túnel muerto | Ejecutar `.\scripts\start-public-link.ps1` para renovar |

## 9. Resumen de brechas críticas pasadas (no repetir)

- ❌ **Docker Engine caído** → nada funciona
- ❌ **Variable de compose faltante** (`APP_OPENAI_API_KEY`) → IA habilitada pero ciega
- ❌ **Secretos no guardados en Credential Manager** → JWT, OpenAI, WhatsApp sin credenciales
- ❌ **Perfil `local-whatsapp-cloud` exige token** → si no hay token válido, backend rechaza requests
