# Correccion de failed_at en inserts y updates de mensajes

## Problema

PostgreSQL reportaba errores al registrar mensajes salientes:

```text
column "failed_at" is of type timestamp with time zone but expression is of type text
```

La causa era el uso de parametros JDBC dentro de expresiones `case when` sin casteo explicito a `timestamptz`.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/conversations/infrastructure/ConversationJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebChannelJdbcRepository.java`

## Cambios aplicados

Se agrego casteo explicito `cast(... as timestamptz)` en las expresiones SQL que asignan columnas temporales:

- `failed_at`
- `sent_at`
- `closed_at`
- `connected_at`
- `disconnected_at`

## Validacion

No se pudo ejecutar Maven en este entorno porque el wrapper intento descargar Maven desde internet y la descarga no esta disponible en el contenedor de trabajo. La correccion esta orientada al error exacto reportado por PostgreSQL.

## Comandos sugeridos

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.local.yml logs -f --tail=300 backend-java postgres whatsapp-web-service
```
