# Estado tecnico del MVP para demo controlada

## Estado real

El MVP queda orientado a una demo controlada de centro estetico. La operacion diaria incluye conversaciones, prospectos, agenda, catalogo, pedidos y pagos. Las rutas administrativas quedan reservadas por rol en servidor y ocultas en la interfaz para usuarios sin permisos.

## Listo para demostracion

- Inicio de sesion con JWT y rutas privadas.
- Operacion diaria para usuarios `AGENT`.
- Lectura/supervision limitada para `SUPERVISOR`.
- Administracion operativa para `ADMIN` y acceso total para `OWNER`.
- Envio por WhatsApp con proveedor configurado explicitamente.
- Simulaciones trazables con estado `SIMULATED` o `DRY_RUN`, nunca como `DELIVERED`.
- Envio manual persistido como `PENDING` antes del despacho, con `idempotencyKey` opcional para evitar duplicados por reintento del cliente.
- Frontend productivo servido como archivos estaticos por Nginx.

## No prometer a cliente

- Alta disponibilidad de WhatsApp Web.
- Entrega garantizada usando el adaptador experimental de WhatsApp Web.
- Integracion productiva completa con WhatsApp Cloud API si faltan credenciales reales.
- Automatizaciones o respuestas de IA sin supervision humana.

## WhatsApp Web vs Cloud API

`WEB` usa el adaptador experimental basado en WhatsApp Web. Debe limitarse a local, demo o validacion temprana.

`CLOUD_API` es la ruta productiva esperada. Requiere `APP_WHATSAPP_CLOUD_API_PHONE_NUMBER_ID` y `APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN`.

`DISABLED` desactiva el canal WhatsApp y hace fallar el despacho con un error explicito.

## Variables obligatorias por ambiente

- `APP_ENVIRONMENT`: `local`, `demo`, `test`, `dev` o `production`.
- `APP_JWT_SECRET`: obligatorio y robusto fuera de ambientes locales.
- `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`.
- `APP_WHATSAPP_CHANNEL_PROVIDER`: `WEB`, `CLOUD_API` o `DISABLED`.
- Para Cloud API: `APP_WHATSAPP_CLOUD_API_PHONE_NUMBER_ID` y `APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN`.
- Para Web local: `APP_WHATSAPP_WEB_ENABLED=true`, `APP_WHATSAPP_WEB_BASE_URL`, `APP_WHATSAPP_WEB_API_KEY`.

## Matriz minima de roles

- `OWNER`: acceso total.
- `ADMIN`: administracion operativa, empresa, usuarios, seguridad, configuracion y WhatsApp.
- `SUPERVISOR`: lectura de configuracion/supervision, sin cambios criticos.
- `AGENT`: conversaciones, prospectos, citas, catalogo, pedidos, pagos y operacion diaria.

## Datos demo y migraciones

Las migraciones versionadas existentes no se reescriben para no romper el historial de Flyway. Los datos insertados por migraciones historicas deben tratarse como semilla de demo/reference del MVP actual.

Para nuevos datos demo, usar scripts separados por ambiente y documentar si son:

- Referencia: datos necesarios para que el dominio funcione.
- Demo: datos comerciales ficticios para presentaciones.

No cargar datos demo en produccion salvo que el ambiente haya sido creado explicitamente para demostracion.

## Ejecucion local

1. Configurar `.env.local` desde `.env.local.example`.
2. Levantar servicios con `docker compose -f docker-compose.local.yml up --build`.
3. Ejecutar backend con `./mvnw test` antes de una demo tecnica.
4. Ejecutar frontend con `pnpm install`, `pnpm test` y `pnpm build`.

## Despliegue productivo

1. Definir secretos reales en el entorno.
2. Usar `APP_ENVIRONMENT=production`.
3. Usar `APP_WHATSAPP_CHANNEL_PROVIDER=CLOUD_API` o `DISABLED`.
4. Mantener `APP_WHATSAPP_CLOUD_API_DRY_RUN_ENABLED=false` salvo pruebas controladas.
5. Validar `docker compose -f docker-compose.prod.yml config`.

## Riesgos pendientes

- El patron outbox completo aun no esta implementado como cola asincrona con reintentos.
- La reconciliacion actual es basica: conserva el mensaje `PENDING` si falla la actualizacion posterior al despacho, pero no ejecuta worker automatico de reintentos.
- WhatsApp Cloud API requiere pruebas con credenciales reales.
- Las migraciones historicas contienen datos demo mezclados con esquema; se documenta la estrategia sin reescribir historial.
- Faltan pruebas end-to-end reales contra navegador y contenedores.
