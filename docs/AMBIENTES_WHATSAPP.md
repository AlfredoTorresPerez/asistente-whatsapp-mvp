# Ambientes WhatsApp

## Objetivo

Guia breve del modelo actual de canal WhatsApp. El canal es nativo del backend Java: no existe servicio externo, ni sesion de dispositivo, ni QR.

## Proveedores

| Proveedor | Uso | Descripcion |
|---|---|---|
| `SIMULATED` | Local (default) | Proveedor simulado embebido en el backend. Permite demos y pruebas sin Meta ni sesion de dispositivo. |
| `META_CLOUD_API` | Produccion | WhatsApp Cloud API de Meta. Webhook firmado `X-Hub-Signature-256` y enrutamiento por `phone_number_id`. |

La seleccion se hace con la variable:

```text
APP_WHATSAPP_CHANNEL_PROVIDER=SIMULATED|META_CLOUD_API
```

Default en Spring: `SIMULATED`. El arranque falla rapido si el proveedor configurado no tiene bean disponible (fail-fast). No existen variables de entorno del antiguo modelo de adaptador web.

## Ambientes

### Local

Servicios locales:

- PostgreSQL: localhost:5433
- Backend: http://localhost:8080
- Frontend: http://localhost:5173
- Mailpit: http://localhost:8025 (SMTP 1025)

Comando recomendado:

```bash
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

No hay servicio en el puerto 3001.

#### Modalidades locales (Fase 3)

El ambiente local tiene dos modalidades intencionales, separadas por perfil de Spring:

| Modalidad | Perfil | Canal | Controles |
|---|---|---|---|
| **Local segura (simulada)** | `local,local-safe` (default compose) | `SIMULATED` | Compuerta de arranque + guard de tráfico: sin Cloud API, OpenAI, espejo Gmail ni calendario Google; pagos `SIMULATED`; correo solo Mailpit |
| **Local Meta controlada** | `local,local-meta-controlled` | `META_CLOUD_API` | Doble confirmación (`APP_LOCAL_META_CONTROLLED_ACKNOWLEDGED=true`), lista permitida de teléfonos (`APP_WHATSAPP_CLOUD_API_ALLOWED_TEST_PHONES`), credenciales completas, firma de webhook obligatoria, dry-run off |

El perfil legacy `local-whatsapp-cloud` se conserva por compatibilidad pero no incluye doble confirmación ni lista permitida.

### Produccion

Comando recomendado:

```bash
cp .env.production.example .env.production
docker compose --env-file .env.production -f docker-compose.prod.yml up --build
```

Variables clave:

```text
APP_WHATSAPP_CHANNEL_PROVIDER=META_CLOUD_API
```

## Webhook entrante Cloud API

- Verificacion (GET): `/api/v1/integrations/whatsapp-cloud/webhook` con `hub.mode`, `hub.verify_token`, `hub.challenge`.
- Recepcion (POST): `/api/v1/integrations/whatsapp-cloud/webhook` con firma `X-Hub-Signature-256` (HMAC-SHA256 del body con App Secret), verificada en tiempo constante.

## Simulador de mensajes entrantes

En local (proveedor `SIMULATED`) los mensajes entrantes se simulan con:

```bash
curl -i -X POST "http://localhost:8080/api/v1/test/whatsapp-inbound" \
  -H "Content-Type: application/json" \
  -d '{"from":"56911112222","body":"Hola, quiero una reserva"}'
```

## Endpoints de administracion del canal

| Metodo | Ruta |
|---|---|
| `GET` | `/api/v1/whatsapp-channel/status` |
| `POST` | `/api/v1/whatsapp-channel/connect` |
| `POST` | `/api/v1/whatsapp-channel/disconnect` |
| `POST` | `/api/v1/whatsapp-channel/test-message` |

Tambien disponibles bajo el alias `/api/channels/whatsapp-channel/...`. No existe `/refresh-qr`.

## Migracion de datos

Las `channel_accounts` historicas con `provider_name = WHATSAPP_WEB` quedan como registros no usados en la BD y no se eliminan. El canal activo se identifica por el proveedor `META_CLOUD_API` o `SIMULATED`; los datos de conversaciones, clientes, mensajes y auditoria se conservan intactos.
