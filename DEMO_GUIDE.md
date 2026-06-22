# Guia de demostracion tecnica controlada

## Objetivo

Mostrar el MVP como una demostracion tecnica controlada de un asistente WhatsApp para un centro estetico, con backend Java, frontend React, PostgreSQL y un adaptador local de WhatsApp Web desacoplado por `CanalWhatsApp`.

Este MVP no esta listo para produccion.

## Alcance real del MVP

Incluye login JWT, dashboard, conversaciones, envio manual de mensajes, prospectos, agenda, catalogo, pedidos, estado del canal WhatsApp Web local, noVNC y simulacion local de mensaje entrante.

La arquitectura conserva `CanalWhatsApp` como abstraccion central. En local se usa `WhatsAppWebAdapter`; en produccion futura debe usarse `WhatsAppCloudApiAdapter` o un proveedor oficial.

## Que si mostrar

- Login con credenciales demo.
- Dashboard con datos semilla.
- Lista y detalle de conversaciones.
- Envio manual de mensaje desde una conversacion.
- Estado del adaptador WhatsApp Web local.
- noVNC para visualizar Chromium y el QR si aplica.
- Simulacion de mensaje entrante si WhatsApp Web no conecta.
- Creacion de prospecto desde conversacion.
- Creacion de cita o pedido desde conversacion.

## Que no mostrar

- El MVP como solucion productiva final.
- Automatizaciones productivas sin supervision.
- Seguridad avanzada, usuarios/roles administrativos o reportes como modulos completos.
- WhatsApp Web como integracion oficial o recomendada para produccion.
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
- whatsapp health: http://localhost:3001/health
- noVNC: http://localhost:6080/vnc.html?autoconnect=true&resize=scale

## Credenciales demo

Las credenciales salen del seed Flyway `V4__seed_demo_data.sql`:

- usuario: `admin@demo.cl`
- contrasena: `Cambiar123!`

No usar estas credenciales en produccion.

## Flujo sugerido de 8 a 10 minutos

1. Login en http://localhost:5173 con `admin@demo.cl` / `Cambiar123!`.
2. Revisar Dashboard y explicar que usa datos semilla.
3. Ir a Conversaciones.
4. Abrir una conversacion existente.
5. Enviar un mensaje manual corto.
6. Ir a Administracion y abrir Conexion WhatsApp Web.
7. Abrir noVNC para mostrar Chromium/QR si esta disponible.
8. Simular o recibir mensaje entrante.
9. Crear prospecto desde la conversacion.
10. Crear cita o pedido desde la conversacion.

## Simulacion de mensaje entrante

Si WhatsApp Web no conecta, usar el endpoint local interno:

```powershell
$body = @{
  sessionKey = "demo-sales"
  from = "+56977778888"
  body = "Hola, quiero consultar por limpieza facial"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/internal/demo/incoming-message" `
  -Headers @{ "X-Demo-Internal-Token" = "dev-demo-internal-token" } `
  -ContentType "application/json" `
  -Body $body
```

El endpoint requiere `APP_DEMO_INTERNAL_TOKEN`. En produccion no debe habilitarse.

## Recuperacion de contrasena

El flujo local puede generar enlace/token en log si no existe correo real. El token no debe guardarse completo en auditoria; solo se registra metadata del usuario y, como maximo, los ultimos 4 caracteres.

## Problemas conocidos

- `whatsapp-web.js` es una integracion no oficial.
- `WhatsAppCloudApiAdapter` existe como esqueleto tecnico y punto de extension, no como integracion productiva completa.
- La demo depende de conectividad externa para la primera construccion.
- Faltan validacion de webhook oficial, firma del proveedor oficial, estados de entrega completos, plantillas aprobadas y observabilidad avanzada.
- Para reuniones, construir imagenes previamente.

## Validacion de produccion conceptual

```bash
docker compose --env-file .env.production.example -f docker-compose.prod.yml config
```

El compose productivo no debe depender de `whatsapp-web-service`, Chromium, Xvfb ni noVNC.
