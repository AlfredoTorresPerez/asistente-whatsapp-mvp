# Resumen de implementacion Centro Estetico

## Cambios realizados

- Se agrego migracion Flyway `V7__aesthetic_center_module.sql`.
- Se agregaron tablas de servicios, productos, categorias, reglas, promociones, profesionales, historial e intenciones.
- Se agrego modulo Java `com.asistentewhatsapp.aesthetic`.
- Se agregaron endpoints REST bajo `/api/v1/esthetic`.
- Se agrego cliente OpenAI configurable y deshabilitado por defecto.
- Se agrego clasificador deterministico de respaldo para operar sin clave externa.
- Se integro el analisis de intencion al webhook del canal WhatsApp despues de persistir mensajes entrantes.
- Se agregaron tipos y cliente de API en frontend React para consumir el modulo estetico.
- Se agregaron pruebas de controlador para listado de servicios y analisis de intencion.

## Archivos nuevos principales

- `backend-java/src/main/resources/db/migration/V7__aesthetic_center_module.sql`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/api/AestheticCenterController.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/infrastructure/AestheticCenterJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/infrastructure/openai/OpenAiIntentClient.java`
- `frontend-react/src/services/api/aestheticApi.ts`
- `docs/CENTRO_ESTETICO_MODULO.md`

## Archivos modificados principales

- `backend-java/src/main/resources/application.yml`
- `backend-java/.env.example`
- `docker-compose.yml`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/whatsappweb/WhatsAppWebWebhookService.java`
- `frontend-react/src/services/api/types.ts`
- `README.md`

## Validacion realizada

No fue posible ejecutar `mvn test` porque el contenedor no tiene Maven instalado y el wrapper no pudo descargar Maven desde `repo.maven.apache.org`. Se valido la sintaxis YAML de `application.yml` y `docker-compose.yml` con analizador local.

## Configuracion

```bash
APP_OPENAI_ENABLED=false
APP_OPENAI_BASE_URL=https://api.openai.com/v1/responses
APP_OPENAI_API_KEY=
APP_OPENAI_MODEL=gpt-5.4-mini
APP_OPENAI_TIMEOUT_SECONDS=30
```

## Riesgos

- El identificador `gpt-5.4-mini` queda parametrizado porque depende de disponibilidad real en la cuenta/proveedor.
- La respuesta sugerida no se envia automaticamente al cliente; queda registrada como salida controlada para evitar respuestas operativas sin validacion adicional.
- La disponibilidad real por profesional requiere una etapa posterior de calendario profesional granular.
