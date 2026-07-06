# Guia de logs de trazabilidad IA - V23.4.3

## Ver logs en vivo

Desde la raiz del proyecto:

```powershell
docker compose -f docker-compose.local.yml logs -f backend-java
```

## Filtrar trazas IA

En PowerShell:

```powershell
docker compose -f docker-compose.local.yml logs --tail=1000 backend-java | Select-String -Pattern "AI_TRACE"
```

## Exportar logs

```powershell
docker compose -f docker-compose.local.yml logs --tail=1000 backend-java > logs_backend_v23_4_3.txt
```

## Seguir un mensaje por traceId

1. Envia un mensaje, por ejemplo:

```text
Hola, quiero reservar limpieza facial profunda mañana a las 10:00 en Providencia
```

2. Busca el primer log:

```text
[AI_TRACE] step=WHATSAPP_MESSAGE_RECEIVED traceId=WA-xxxxxxxx
```

3. Filtra por ese `traceId`:

```powershell
docker compose -f docker-compose.local.yml logs --tail=1000 backend-java | Select-String -Pattern "WA-xxxxxxxx"
```

## Pasos importantes

- `WHATSAPP_MESSAGE_RECEIVED`: entrada desde WhatsApp.
- `CONVERSATION_CONTEXT_LOADED`: conversacion/contacto/contexto cargado.
- `MESSAGE_NORMALIZED`: texto original y normalizado.
- `INTENT_CANDIDATES`: grupos de intencion evaluados.
- `INTENT_DETECTED`: intencion final y confianza.
- `ENTITIES_EXTRACTED`: servicio, fecha, hora, sucursal y entidades.
- `EFFECTIVE_LOCATION_RESOLVED`: sucursal efectiva y fuente.
- `SERVICE_RESOLVED`: servicio interno resuelto.
- `SERVICE_LOCATION_VALIDATED`: validacion servicio-sucursal.
- `BOOKING_REQUIRED_DATA_CHECK`: datos minimos y siguiente accion.
- `AVAILABILITY_CHECK_STARTED`: inicio de validacion de agenda.
- `AVAILABILITY_CHECK_RESULT`: resultado de disponibilidad.
- `AVAILABILITY_ALTERNATIVES_FOUND`: alternativas si no hay cupo exacto.
- `TEMPORARY_BOOKING_CREATED`: reserva temporal creada.
- `CONFIRMATION_LINK_CREATED`: enlace generado con token enmascarado.
- `AI_FINAL_RESPONSE`: respuesta final antes de enviar.
- `WHATSAPP_RESPONSE_SEND_STARTED`: inicio de envio.
- `WHATSAPP_RESPONSE_SEND_RESULT`: resultado de envio.
- `FLOW_ERROR`: error tecnico con contexto.

## Datos protegidos

- Telefono: `+569****5678`.
- Token: `abcdef...wxyz`.
- URLs de confirmacion se sanitizan en logs extensos.
