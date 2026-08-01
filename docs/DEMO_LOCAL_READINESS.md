# Demo Local Controlada — Readiness

## Alcance real de la demo

El MVP demuestra un asistente de WhatsApp para centro estetico con:

- **Landing page publica** con servicios por categoria y sucursales.
- **Wizard de reserva publico** en 5 pasos: servicio → sucursal → fecha/hora → datos del cliente → confirmacion.
- **Notificacion por WhatsApp** con enlace de confirmacion (canal simulado embebido en el backend).
- **Notificacion por email** con template HTML premium.
- **Confirmacion, reprogramacion y cancelacion** desde enlace publico.
- **Panel interno de agenda** con vista semanal, filtros y acciones operativas.
- **Pagos simulados** (no se procesa dinero real).

## Que NO prometer

| Aspecto | Motivo |
|---------|--------|
| Alta disponibilidad del canal | El canal local es un proveedor simulado, no productivo |
| Entrega garantizada de mensajes | En local solo se simula; produccion usa WhatsApp Cloud API |
| Respuesta automatica IA | `APP_AI_AGENTS_AUTO_REPLY_ENABLED=false` por defecto |
| WhatsApp Cloud API | Requiere credenciales de Meta Business |
| Certificado SSL / HTTPS en local | Solo accesible via `localhost` |
| Enlaces funcionales fuera de la red local | Los enlaces contienen `localhost` a menos que se use tunel |

## Stack

| Componente | Tecnologia |
|------------|-----------|
| Backend | Java 21 / Spring Boot 3.x / Flyway |
| Frontend | React 18 / TypeScript / Vite / Tailwind |
| Base de datos | PostgreSQL 16 |
| WhatsApp | Canal simulado embebido (Spring), proveedor `SIMULATED` |
| Contenedores | Docker Compose |
| Pagos | SIMULATED (no real) |
| IA agente | Desactivada por defecto en demo |

## Credenciales demo

| Rol | Email | Password |
|-----|-------|----------|
| OWNER | `admin@demo.cl` | `Cambiar123!` |
| ADMIN | `admin2@demo.cl` | `Cambiar123!` |
| AGENT | `agente@demo.cl` | `Cambiar123!` |
| SUPERVISOR | `supervisor@demo.cl` | `Cambiar123!` |

## Checklist previo a la demo

- [ ] `git status` — sin cambios sin commitear ni archivos no rastreados sensibles.
- [ ] `.env.local` no contiene credenciales reales.
- [ ] `docker compose -f docker-compose.local.yml config` — sin errores.
- [ ] `cd backend-java && .\mvnw.cmd -q test` — todos los tests pasan.
- [ ] `cd frontend-react && corepack pnpm lint` — 0 errores.
- [ ] `cd frontend-react && corepack pnpm test` — todos los tests pasan.
- [ ] `cd frontend-react && corepack pnpm build` — build exitoso.
- [ ] `APP_WHATSAPP_CHANNEL_PROVIDER=SIMULATED` en `.env.local` — canal simulado activo.
- [ ] `docker compose -f docker-compose.local.yml up -d` — todos los contenedores healthy.
- [ ] `http://localhost:5173/` — landing page carga.
- [ ] `http://localhost:5173/reservar` — wizard de reserva carga.
- [ ] `POST http://localhost:8080/api/v1/test/whatsapp-inbound` — mensaje entrante simulado procesado.
- [ ] Email SMTP configurado (o simulado) segun `.env.local`.

## Comandos de validacion

```bash
# Backend tests
cd backend-java
.\mvnw.cmd -q test

# Frontend lint
cd frontend-react
corepack pnpm lint

# Frontend tests
corepack pnpm test

# Frontend build
corepack pnpm build

# Canal simulado: mensaje entrante de prueba
curl -i -X POST "http://localhost:8080/api/v1/test/whatsapp-inbound" \
  -H "Content-Type: application/json" \
  -d '{"from":"56911112222","body":"Hola, quiero agendar una reserva"}'

# Docker compose config validation
docker compose -f docker-compose.local.yml config
```

## Comandos de arranque local

```bash
# 1. Preparar entorno
cp .env.local.template .env.local
# Editar .env.local con valores reales si es necesario

# 2. Iniciar todo
docker compose -f docker-compose.local.yml up -d --build

# 3. Ver estado
docker compose -f docker-compose.local.yml ps

# 4. Ver logs
docker compose -f docker-compose.local.yml logs -f

# 5. Opcional: tunel publico
docker compose -f docker-compose.local.yml --profile public-link up -d

# 6. Detener
docker compose -f docker-compose.local.yml down
```

## Como generar ZIP limpio de demo

```powershell
# Windows
powershell -File scripts\package_demo_clean.ps1
```

El script crea una copia temporal excluyendo `.git`, `.idea`, `node_modules`, `target`, `dist`, `.env`, sesiones WhatsApp, caches y artefactos de build. El ZIP se genera en el directorio padre del workspace.

## Riesgos pendientes

1. **Tunel ephemeral**: `trycloudflare.com` cambia cada vez que se reinicia el contenedor. Los enlaces de confirmacion anteriores quedan rotos.
2. **Canal simulado**: en local no hay entrega real de WhatsApp; la simulacion usa `POST /api/v1/test/whatsapp-inbound` con el proveedor `SIMULATED`.
3. **Pagos simulados**: No hay integracion real con Mercado Pago ni otro proveedor.
4. **IA desactivada**: La auto-respuesta del orquestador multiagente esta desactivada por defecto. Habilitar solo para pruebas especificas.
5. **Email SMTP**: Las credenciales de Gmail deben configurarse manualmente en `.env.local`. Sin ellas, el email se simula en consola.
6. **Datos demo**: Las migraciones contienen datos semilla mezclados con esquema. No reescribir migraciones historicas.
7. **Sin SSL**: La comunicacion local no esta cifrada. No usar en redes no confiables.
