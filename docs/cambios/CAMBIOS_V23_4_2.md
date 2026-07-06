# Cambios V23.4.2 - Sucursal efectiva en contactos sin sede

## Problema corregido

En conversaciones donde el contacto aparecia como `Sin sucursal`, el flujo de IA podia ignorar la sucursal escrita por el cliente y terminar pidiendo sucursal nuevamente o devolviendo errores tecnicos 404 cuando intentaba validar agenda.

Ejemplo afectado:

```text
Hola, quiero reservar limpieza facial profunda manana a las 10:00 en Providencia
```

## Correccion aplicada

Se implemento una jerarquia de resolucion de sucursal efectiva:

1. Sucursal explicita en el mensaje del cliente.
2. Sucursal extraida como entidad.
3. Sucursal seleccionada en la conversacion.
4. Sucursal unica del negocio, solo si existe una sola activa.
5. Si no existe ninguna, se pregunta sucursal.

La sucursal escrita por el cliente tiene prioridad sobre cualquier sucursal asociada a la conversacion.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentConversationRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebWebhookService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebChannelJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/application/WhatsAppWebAdministrationService.java`
- `backend-java/src/main/resources/db/migration/V27__fix_contact_location_resolution_and_service_location_seed.sql`
- `database/manual/V27__fix_contact_location_resolution_and_service_location_seed.sql`
- `test_ia_negocio_conversacional_v23_4_2.ps1`

## Manejo de errores

Si el servicio no esta configurado para la sucursal, el backend ya no debe propagar un 404 tecnico al flujo conversacional. Debe responder:

```text
Limpieza facial profunda no esta configurado para Providencia. Puedo revisar otra sucursal disponible o ayudarte con otro servicio.
```

## Validacion pendiente

No se ejecuto compilacion Maven completa en este entorno porque el wrapper no pudo descargar Maven desde repositorios externos. Se realizo una validacion estatica de sintaxis Java con `javac` sin detectar errores de cadena sin cerrar ni errores de sintaxis en los archivos modificados.
