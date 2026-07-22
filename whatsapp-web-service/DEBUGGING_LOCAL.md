# 🔧 Cómo Debuggear WhatsApp Web Local

Guía práctica para diagnosticar y resolver problemas del adaptador `whatsapp-web.js` en entorno local con Docker.

---

## 📋 Arquitectura Rápida

```
┌─────────────────┐     HTTP/JSON      ┌──────────────────────┐
│  Backend Java   │ ◄─────────────────► │  whatsapp-web-service │
│  (puerto 8080)  │   Webhook + API    │  (puerto 3001)       │
└─────────────────┘                    └──────────────────────┘
                                                │
                                                ▼
                                         ┌──────────────────┐
                                         │  Chromium +      │
                                         │  whatsapp-web.js │
                                         └──────────────────┘
```

---

## 🚀 Levantar el Entorno

### Opción A: Solo servicios core (sin WhatsApp Web)
```bash
docker compose -f docker-compose.local.yml up -d --build
# Servicios: postgres, backend-java, frontend-react
```

### Opción B: Con WhatsApp Web (headless, recomendado)
```bash
cp whatsapp-web-service/.env.local.template .env.local
docker compose -f docker-compose.local.yml --profile whatsapp up -d --build whatsapp-web-service
# O si ya están arriba los otros:
docker compose -f docker-compose.local.yml --profile whatsapp up -d whatsapp-web-service
```

### Opción C: Con VNC visual (debug visual)
```bash
docker compose -f docker-compose.local.yml --profile whatsapp up -d whatsapp-web-service
# Luego abre: http://localhost:6080/vnc.html?autoconnect=true&resize=scale
```

---

## ✅ Verificar Salud

### 1. Healthcheck del adaptador
```bash
curl -s http://localhost:3001/health | jq .
```
**Respuesta esperada (conectado):**
```json
{
  "status": "UP",
  "service": "whatsapp-webjs-service",
  "connectionStatus": "CONNECTED",
  "runtimeReady": true,
  "timestamp": "2026-07-15T..."
}
```

**Respuesta esperada (esperando QR):**
```json
{
  "status": "UP",
  "connectionStatus": "QR_PENDING",
  "runtimeReady": false,
  "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg..."
}
```

### 2. Ver logs en tiempo real
```bash
# Solo adaptador
docker compose -f docker-compose.local.yml logs -f whatsapp-web-service --tail=100

# Todos los servicios
docker compose -f docker-compose.local.yml logs -f --tail=50
```

### 3. Ver estado de sesión via API
```bash
curl -s -H "X-API-Key: dev-whatsapp-web-key" http://localhost:3001/api/v1/session/status | jq .
```

---

## 🔄 Flujo QR → Conexión

### 1. Obtener QR
```bash
curl -s -H "X-API-Key: dev-whatsapp-web-key" http://localhost:3001/api/v1/session/qr | jq -r .qrCode
# Copia el data URL y ábrelo en navegador, o usa: qrencode -t UTF8 <(echo "...")
```

### 2. Escanear con WhatsApp móvil
- Abre WhatsApp → Configuración → Dispositivos vinculados → Vincular dispositivo
- Escanea el código QR

### 3. Verificar conexión
```bash
# Poll hasta que pase a CONNECTED
watch -n 5 'curl -s -H "X-API-Key: dev-whatsapp-web-key" http://localhost:3001/api/v1/session/status | jq .connectionStatus'
```

---

## 🐛 Problemas Comunes y Soluciones

### 1. **"Profile in use" / SingletonLock**
**Síntoma:** Chromium no inicia, error "Profile in use"
```bash
# Solución: El entrypoint ya limpia locks, pero si persiste:
docker compose -f docker-compose.local.yml exec whatsapp-web-service \
  find /app/.wwebjs_auth -name "Singleton*" -delete
docker compose -f docker-compose.local.yml restart whatsapp-web-service
```

### 2. **Chromium OOM / Crashes**
**Síntoma:** Contenedor se reinicia, logs "Killed" o "Out of memory"
```bash
# Verificar shm_size (debe ser 4gb)
docker inspect asistente-whatsapp-whatsapp-web | grep -A2 ShmSize

# Aumentar memoria Docker Desktop (Settings → Resources → Memory > 6GB)
# Verificar logs de Chromium:
docker compose -f docker-compose.local.yml logs whatsapp-web-service | grep -i chromium
```

### 3. **Healthcheck no pasa (start_period 60s)**
**Síntoma:** Servicio `unhealthy` tras 60s
```bash
# Ver qué devuelve /health
curl -v http://localhost:3001/health

# Si runtimeReady=false: esperar QR o revisar logs
docker compose -f docker-compose.local.yml logs whatsapp-web-service --tail=200 | grep -E "(connectionStatus|runtimeReady|lastError|ERROR)"
```

### 4. **QR expira / no aparece**
**Síntoma:** `qrCode: null` y `connectionStatus: "SYNCING"` o `"ERROR"`
```bash
# Forzar regenerar QR
curl -X POST -H "X-API-Key: dev-whatsapp-web-key" http://localhost:3001/api/v1/session/refresh-qr

# Ver logs de generación QR
docker compose -f docker-compose.local.yml logs whatsapp-web-service | grep -i "qr"
```

### 5. **Webhook no llega al backend**
**Síntoma:** Mensaje en WhatsApp pero no en backend
```bash
# 1. Verificar que backend está sano
curl http://localhost:8080/actuator/health

# 2. Verificar conectividad red (desde contenedor whatsapp-web-service)
docker compose -f docker-compose.local.yml exec whatsapp-web-service \
  wget -qO- http://backend-java:8080/actuator/health

# 3. Ver logs de envío webhook
docker compose -f docker-compose.local.yml logs whatsapp-web-service | grep -i "webhook"
```

### 6. **Sesión no persiste tras `docker compose down/up`**
**Síntoma:** Pide QR cada vez que reinicias
```bash
# Verificar volumen
docker volume inspect asistente_whatsapp-webjs-session-data

# Verificar ownership (debe ser node:node = 1000:1000)
docker compose -f docker-compose.local.yml exec whatsapp-web-service ls -la /app/.wwebjs_auth/

# Si ownership incorrecto:
docker compose -f docker-compose.local.yml down
docker run --rm -v asistente_whatsapp-webjs-session-data:/data alpine chown -R 1000:1000 /data
docker compose -f docker-compose.local.yml up -d
```

---

## 📊 Logs Estructurados (JSON)

Desde `LOG_JSON=true`, cada línea es JSON parseable:

```bash
# Filtrar por nivel
docker compose -f docker-compose.local.yml logs whatsapp-web-service | jq 'select(.level=="error")'

# Filtrar por correlationId
docker compose -f docker-compose.local.yml logs whatsapp-web-service | jq 'select(.correlationId=="abc-123")'

# Ver métricas de rendimiento
docker compose -f docker-compose.local.yml logs whatsapp-web-service | jq 'select(.message=="whatsapp-webjs-service listening")'
```

**Campos clave:**
- `timestamp`: ISO 8601 UTC
- `level`: debug/info/warn/error
- `correlationId`: UUID por request (webhook, send, etc.)
- `connectionStatus`: DISCONNECTED|SYNCING|QR_PENDING|AUTHENTICATED|CONNECTED|ERROR
- `runtimeReady`: boolean (true = listo para enviar/recibir)
- `lastError`: último error si hay

---

## 🧪 Tests Manuales Rápidos

### Enviar mensaje de prueba
```bash
curl -X POST -H "X-API-Key: dev-whatsapp-web-key" -H "Content-Type: application/json" \
  -d '{"businessId":"00000000-0000-0000-0000-000000000001","to":"56950954580","body":"Test desde local"}' \
  http://localhost:3001/api/v1/messages/send | jq .
```

### Simular mensaje entrante (prueba webhook)
```bash
curl -X POST -H "X-API-Key: dev-whatsapp-web-key" -H "Content-Type: application/json" \
  -d '{"from":"56950954580","to":"56927305158","body":"Hola, ¿cómo están?"}' \
  http://localhost:3001/api/v1/messages/simulate-inbound | jq .
```

### Verificar outbox IA procesó
```bash
# En backend-java (si tienes acceso BD)
# SELECT * FROM ai_reply_outbox ORDER BY created_at DESC LIMIT 5;
```

---

## 🔧 Variables de Entorno Críticas

| Variable | Valor Local Recomendado | Descripción |
|----------|------------------------|-------------|
| `WHATSAPP_WEB_HEADLESS` | `true` | Sin GUI, estable |
| `WHATSAPP_WEB_VISUAL_MODE` | `false` | Sin VNC, ahorra RAM |
| `WHATSAPP_WEB_AUTO_CONNECT` | `true` | Reconecta solo |
| `WHATSAPP_WEB_DEMO_FALLBACK_ENABLED` | `false` | **Ver errores reales** |
| `WHATSAPP_WEB_INIT_RETRIES` | `5` | Más intentos |
| `WHATSAPP_WEB_INIT_RETRY_DELAY_MS` | `10000` | 10s entre reintentos |
| `shm_size` | `4gb` | En docker-compose |

---

## 📁 Archivos Relevantes

```
whatsapp-web-service/
├── Dockerfile                 # Imagen optimizada (node:20-slim, dumb-init, user node)
├── docker-entrypoint.sh       # Limpia locks, mata chromium huérfano
├── src/server.js              # Servidor Express + whatsapp-web.js
├── .env.local.template        # Variables recomendadas (copia a .env.local)
└── package.json
```

---

## 🚨 Checklist de "Todo Verde"

- [ ] `docker compose ps` → todos `healthy` o `running`
- [ ] `curl localhost:3001/health` → `runtimeReady: true`
- [ ] `curl localhost:8080/actuator/health` → `UP`
- [ ] `curl localhost:5173` → HTML frontend
- [ ] QR escaneado → status `CONNECTED`
- [ ] Mensaje de prueba → llega a backend → IA responde → sale por WhatsApp
- [ ] `docker compose down && docker compose up -d` → sesión persiste (no pide QR)

---

## 🆘 Si Nada Funciona

```bash
# Reset nuclear (pierdes sesión WhatsApp)
docker compose -f docker-compose.local.yml down -v
docker volume rm asistente_whatsapp-webjs-session-data asistente_whatsapp-webjs-cache-data
docker compose -f docker-compose.local.yml up -d --build

# Luego escanea QR de nuevo
```

---

## 📚 Referencias

- [whatsapp-web.js Docs](https://wwebjs.dev/)
- [Puppeteer Troubleshooting](https://github.com/puppeteer/puppeteer/blob/main/docs/troubleshooting.md)
- [Chromium en Docker](https://github.com/puppeteer/puppeteer/blob/main/docs/troubleshooting.md#running-in-docker)