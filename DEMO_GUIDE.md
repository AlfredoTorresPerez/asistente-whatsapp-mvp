# Guia de demostracion tecnica controlada

## Objetivo

Mostrar el MVP como una demostracion tecnica controlada de un asistente WhatsApp para un centro estetico, con backend Java, frontend React y PostgreSQL. El canal de WhatsApp es nativo del backend: proveedor `SIMULATED` (embebido, default local) o `META_CLOUD_API` (WhatsApp Cloud API de Meta). No existe servicio Node externo, QR ni Chromium.

Este MVP no esta listo para produccion.

## Alcance real del MVP

Incluye login JWT, dashboard, conversaciones, envio manual de mensajes, prospectos, agenda, catalogo, pedidos, estado del canal WhatsApp (simulado o Cloud API) y simulacion local de mensaje entrante.

La arquitectura conserva `CanalWhatsApp` como abstraccion central. En local el proveedor es `SIMULATED` (embebido en Spring Boot); en produccion se usa `META_CLOUD_API` via `WhatsAppCloudApiAdapter`.

## Que si mostrar

- Login con credenciales demo.
- Dashboard con datos semilla.
- Lista y detalle de conversaciones.
- Envio manual de mensaje desde una conversacion.
- Estado del canal WhatsApp (proveedor, conexion, eventos recientes) en `/admin/whatsapp-channel`.
- Simulacion de mensaje entrante via `/admin/whatsapp-simulator` o `POST /api/v1/test/whatsapp-inbound`.
- Creacion de prospecto desde conversacion.
- Creacion de cita o pedido desde conversacion.

## Que no mostrar

- El MVP como solucion productiva final.
- Automatizaciones productivas sin supervision.
- Seguridad avanzada, usuarios/roles administrativos o reportes como modulos completos.
- El canal simulado como integracion productiva.
- Estados de entrega, plantillas, firma de proveedor oficial u observabilidad avanzada como terminados.

## Arranque local

```bash
docker compose --env-file .env.local.example -f docker-compose.local.yml config
docker compose --env-file .env.local.example -f docker-compose.local.yml build
docker compose --env-file .env.local.example -f docker-compose.local.yml up
```

En Windows PowerShell:

```powershell
docker compose --env-file .env.local.example -f docker-compose.local.yml config
docker compose --env-file .env.local.example -f docker-compose.local.yml build
docker compose --env-file .env.local.example -f docker-compose.local.yml up
```

Para reuniones, construir las imagenes previamente.

## URLs principales

- frontend: http://localhost:5173
- backend health: http://localhost:8080/actuator/health
- API docs: http://localhost:8080/swagger-ui/index.html

## Credenciales demo

Todas usan la contrasena `Cambiar123!`:

| Usuario | Rol | Descripcion |
|---|---|---|
| `admin@demo.cl` | OWNER | Acceso total a la plataforma |
| `admin2@demo.cl` | ADMIN | Administracion operativa |
| `supervisor@demo.cl` | SUPERVISOR | Solo lectura/supervision |
| `agente@demo.cl` | AGENT | Operacion diaria (conversaciones, citas, pedidos) |

No usar estas credenciales en produccion.

## Flujo sugerido de 10 a 12 minutos

1. Login en http://localhost:5173 con `admin@demo.cl` / `Cambiar123!`.
2. Revisar Dashboard con datos semilla actualizados a Junio 2026.
3. Ir a Conversaciones y abrir la conversacion con Sofia Rojas.
4. Enviar un mensaje manual corto.
5. Ir a Agenda para ver las reservas de los proximos dias.
6. Crear una nueva reserva desde una conversacion.
7. Generar link de pago para la reserva (modo SIMULATED).
8. Abrir el link de pago publico en `/reservas/pagar/{paymentId}`.
9. Ir a Administracion > Canal de WhatsApp (mostrar estado del proveedor simulado).
10. Simular mensaje entrante (ver abajo) con `POST /api/v1/test/whatsapp-inbound`.
11. Crear prospecto desde la conversacion.
12. Crear pedido desde la conversacion.

## Simulacion de mensaje entrante

Con el proveedor `SIMULATED` (default local), usar el endpoint de simulacion:

```powershell
$body = @{
  sessionKey = "demo-sales"
  from = "+56977778888"
  body = "Hola, quiero consultar por limpieza facial"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/test/whatsapp-inbound" `
  -ContentType "application/json" `
  -Body $body
```

Tambien disponible desde la UI en `/admin/whatsapp-simulator`. El mensaje se procesa con el mismo flujo que un mensaje real entrante (evento de canal, conversacion, IA si esta habilitada).

## Recuperacion de contrasena

El flujo local puede generar enlace/token en log si no existe correo real. El token no debe guardarse completo en auditoria; solo se registra metadata del usuario y, como maximo, los ultimos 4 caracteres.

## Problemas conocidos

- El canal simulado no entrega mensajes fuera del proceso local: sirve para demo y desarrollo.
- `WhatsAppCloudApiAdapter` requiere credenciales Meta reales (token, phone_number_id, webhook verificado) para operar fuera de dry-run.
- La demo depende de conectividad externa para la primera construccion.
- Faltan validacion de webhook oficial, firma del proveedor oficial, estados de entrega completos, plantillas aprobadas y observabilidad avanzada.
- Para reuniones, construir imagenes previamente.

## Validacion de produccion conceptual

```bash
docker compose --env-file .env.production.example -f docker-compose.prod.yml config
```

El compose productivo usa `APP_WHATSAPP_CHANNEL_PROVIDER=META_CLOUD_API` y no incluye servicios externos de canal.
