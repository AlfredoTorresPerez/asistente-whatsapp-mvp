# Validación Local MVP — Guía Rápida (3 min)

## Resumen
Script único que valida todo el stack local en < 3 minutos:
- Docker Compose up + build
- Healthchecks (PostgreSQL, Backend, Frontend, WhatsApp Web opcional)
- Auth + API smoke test
- WhatsApp Web status
- Webhook HMAC + channel_event_log
- IA Auto-reply end-to-end

---

## Uso Rápido

```powershell
# Quick (solo health + auth) — ideal pre-push hook
.\scripts\verify_mvp_local.ps1 -Quick

# Full validation (todo)
.\scripts\verify_mvp_local.ps1 -Profile whatsapp

# Con más tiempo si build inicial
.\scripts\verify_mvp_local.ps1 -Profile whatsapp -TimeoutMinutes 5 -NoCleanup
```

```bash
# Bash (Git Bash / WSL / Linux)
./scripts/verify_mvp_local.sh --quick
./scripts/verify_mvp_local.sh --profile whatsapp
./scripts/verify_mvp_local.sh --profile whatsapp --timeout 5 --no-cleanup
```

---

## Parámetros

| Parámetro | PowerShell | Bash | Descripción |
|-----------|------------|------|-------------|
| Quick mode | `-Quick` | `--quick` | Solo health + auth (30s) |
| Perfil WhatsApp | `-Profile whatsapp` | `--profile whatsapp` | Incluye whatsapp-web-service |
| Timeout global | `-TimeoutMinutes 3` | `--timeout 3` | Minutos máx (default 3) |
| No cleanup | `-NoCleanup` | `--no-cleanup` | Deja servicios arriba |

---

## Exit Codes

| Código | Significado | Acción |
|--------|-------------|--------|
| **0** | ✅ TODO PASS | OK |
| **1** | ❌ Docker compose falló | Revisar `docker compose logs` |
| **2** | ❌ Healthchecks timeout | Algún servicio no llegó a `healthy` |
| **3** | ❌ API/Auth falló | Backend down, login inválido, o `/api/v1/company` 404 |
| **4** | ❌ WhatsApp Web falló | Sesión no CONNECTED/QR_PENDING, webhook error |
| **5** | ❌ IA Auto-reply falló | Outbox no procesó, o no hubo MESSAGE_SENT |

---

## Flujo Detallado (Full Mode)

```
1. docker compose up -d --build
       │
       ▼
2. Wait healthchecks (max 3 min)
   ├── postgres: healthy
   ├── backend-java: healthy (/actuator/health UP)
   ├── frontend-react: healthy (HTTP 200)
   └── whatsapp-web-service: healthy (runtimeReady=true) [si --profile whatsapp]
       │
       ▼
3. Health endpoints
   ├── GET http://localhost:8080/actuator/health → UP
   ├── GET http://localhost:5173 → 200
   └── GET http://localhost:3001/health → runtimeReady [whatsapp]
       │
       ▼
4. Auth + API Smoke
   ├── POST /api/v1/auth/login (admin@demo.cl / Cambiar123!) → accessToken
    └── GET /api/v1/company → 200 + id
       │
       ▼
5. WhatsApp Web Status [whatsapp profile]
   └── GET /api/v1/whatsapp-web/status → sessionStatus ∈ {CONNECTED, QR_PENDING}
       │
       ▼
6. Webhook Test
   ├── POST /api/v1/integrations/whatsapp-web/webhook (HMAC válido)
   └── Poll channel_event_log → deliveryId PROCESSED
       │
       ▼
7. IA Auto-reply Test
   ├── POST webhook MESSAGE_RECEIVED (texto agendamiento)
   ├── Poll /api/v1/ai/outbox/stats → pending=0 processing=0 failed=0
   └── channel_event_log → MESSAGE_SENT detectado
       │
       ▼
8. Cleanup (si no -NoCleanup)
   └── docker compose down
```

---

## Prerrequisitos

- Docker Desktop corriendo
- `.env.local` copiado de `.env.local.template`
- Puerto 5433, 8080, 5173, 3001 libres
- Para perfil `whatsapp`: 4GB RAM disponible (shm_size)

---

## Troubleshooting Común

### Exit 1 — Docker Compose Falló
```bash
docker compose -f docker-compose.local.yml logs -f
# Verificar: puertos ocupados, memoria, Docker daemon
```

### Exit 2 — Healthcheck Timeout
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
docker logs asistente-backend
docker logs asistente-frontend
docker logs asistente-whatsapp-web
```

### Exit 3 — Auth/API Falló
```bash
# Verificar backend
curl -v http://localhost:8080/actuator/health
# Verificar login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.cl","password":"Cambiar123!"}'
```

### Exit 4 — WhatsApp Web
```bash
# Ver estado
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/whatsapp-web/status

# Escanear QR si QR_PENDING
# Abrir: http://localhost:5173/admin/whatsapp-web

# Logs WhatsApp Web
docker logs -f asistente-whatsapp-web
```

### Exit 5 — IA Auto-reply
```bash
# Stats outbox
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/ai/outbox/stats

# Logs worker
docker logs asistente-backend | grep -E "AI_OUTBOX|AI_ROUTE|WHATSAPP_RESPONSE"

# Verificar seed
docker exec -it asistente-postgres psql -U assistant -d asistente_whatsapp \
  -c "SELECT * FROM business_location WHERE whatsapp_number IS NOT NULL;"
docker exec -it asistente-postgres psql -U assistant -d asistente_whatsapp \
  -c "SELECT name, requires_room FROM aesthetic_service;"
```

---

## Integración CI (pre-push hook)

```bash
# .husky/pre-push
#!/bin/sh
echo "🔍 Validando MVP local (quick)..."
pwsh -NoProfile -File scripts/verify_mvp_local.ps1 -Quick
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
    echo "❌ Validación local falló (exit $EXIT_CODE). Fix antes de push."
    exit 1
fi
echo "✅ Pre-push validation OK"
```

```json
// package.json (frontend-react)
"husky": {
  "hooks": {
    "pre-push": "pwsh -NoProfile -File scripts/verify_mvp_local.ps1 -Quick"
  }
}
```

---

## Archivos del Suite

| Archivo | Descripción |
|---------|-------------|
| `scripts/verify_mvp_local.ps1` | **Principal** — Orquesta todo (PowerShell) |
| `scripts/verify_mvp_local.sh` | Versión Bash (Git Bash / WSL / Linux) |
| `scripts/test-whatsapp-webhook-local.ps1` | Webhook HMAC unit test |
| `scripts/test-whatsapp-webhook-local.sh` | Webhook HMAC unit test (Bash) |
| `scripts/test-ai-auto-reply-local.ps1` | IA Auto-reply E2E test |
| `scripts/test-ai-auto-reply-local.sh` | IA Auto-reply E2E test (Bash) |

---

## Notas

- **Quick mode** (`-Quick` / `--quick`): ~30s, no requiere WhatsApp Web corriendo
- **Full mode**: ~2-3 min (incluye build inicial), requiere perfil `whatsapp` y sesión CONNECTED
- **`-NoCleanup`**: Deja servicios arriba para debug manual posterior
- Los scripts usan `.env.local` si existe, sino defaults del compose
- HMAC secret por defecto: `dev-whatsapp-web-webhook-secret`