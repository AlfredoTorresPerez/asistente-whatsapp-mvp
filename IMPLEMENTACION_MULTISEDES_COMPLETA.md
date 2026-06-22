# Implementacion multisede completa

## Resumen ejecutivo

Se aplico una evolucion incremental para convertir el MVP en una base multisede mas completa sin modificar migraciones ya aplicadas. La entidad central sigue siendo `business_location`, mientras `business` mantiene el significado de negocio.

## Diagnostico inicial

### Ya estaba multisede

- `business_location` como entidad de sede.
- `booking.location_id` para citas.
- `conversation.location_id` opcional para conversaciones.
- `aesthetic_professional_location` para profesionales por sede.
- Pantalla basica `Sedes del negocio`.

### Estaba parcialmente multisede

- Agenda: seleccion de sede, pero sin todo el modelo de disponibilidad por sede.
- Conversaciones: sede visible, pero sin permisos ni reglas completas.
- Profesionales: relacion por sede, pero sin horario estructurado por sede.

### No estaba multisede completo

- Prospectos sin `location_id` directo.
- Pedidos sin `location_id`.
- Catalogo sin disponibilidad por sede.
- Productos sin stock por sede.
- WhatsApp sin canal por sede.
- Usuarios sin permisos por sede.
- Reportes y auditoria sin `location_id` estructurado.

## Cambios PostgreSQL

Migracion nueva:

```text
backend-java/src/main/resources/db/migration/V18__complete_multisite_model.sql
```

No se modifico `V17`.

### Columnas nuevas

- `lead.location_id`
- `order_request.location_id`
- `order_request.fulfillment_type`
- `channel_account.location_id`
- `channel_account.routing_mode`
- `automation_rule.location_id`
- `response_template.location_id`
- `aesthetic_treatment_history.location_id`
- `notification.location_id`
- `audit_log.location_id`
- `business_location.opening_hours`
- `business_location.notes`

### Tablas nuevas

- `product_service_location`
- `product_location_stock`
- `aesthetic_service_location`
- `professional_location_schedule`
- `user_location_access`

### Datos semilla

Se agregaron sedes demo para Centro Estetico Bella:

- Providencia.
- Maipu.
- Santiago Centro.

Tambien se asociaron profesionales, servicios, catalogo, stock inicial, horarios y permisos a las sedes demo.

## Backend Java

Modulo nuevo:

```text
backend-java/src/main/java/com/asistentewhatsapp/multisite
```

### Endpoints nuevos

```text
GET  /api/v1/multisite/summary
GET  /api/v1/multisite/catalog-availability
PUT  /api/v1/multisite/catalog-availability
GET  /api/v1/multisite/professionals
GET  /api/v1/multisite/professional-schedules
POST /api/v1/multisite/professional-schedules
GET  /api/v1/multisite/user-access
PUT  /api/v1/multisite/user-access
GET  /api/v1/multisite/channels
PUT  /api/v1/multisite/channels/{channelId}/location
```

### Validaciones aplicadas en backend

- Validar que la sede pertenezca al negocio autenticado.
- Validar que el producto o servicio pertenezca al negocio.
- Validar que el profesional pertenezca al negocio.
- Validar que el usuario pertenezca al negocio.
- Crear/actualizar disponibilidad y stock por sede.
- Crear horario por profesional y sede.
- Crear/actualizar permisos por usuario y sede.
- Configurar canal WhatsApp centralizado o por sede.

## Frontend React

Modulo nuevo:

```text
frontend-react/src/modules/multisite
```

API nueva:

```text
frontend-react/src/services/api/multisiteApi.ts
```

Tipos agregados:

```text
frontend-react/src/services/api/types.ts
```

Ruta nueva:

```text
/admin/multisite
```

Menu lateral:

```text
Operacion multisede
```

### Pantalla nueva: Operacion multisede completa

Incluye pestanas para:

- Resumen por sede.
- Catalogo y stock.
- Profesionales y horarios.
- Permisos por sede.
- WhatsApp por sede.
- Checklist de validacion operacional.

## Reglas operativas cubiertas

- La sede es eje de agenda, catalogo, stock, profesionales, permisos y canal.
- Una conversacion puede seguir sin sede hasta que requiera operacion fisica.
- El catalogo puede estar disponible por sede.
- Los productos tienen stock por sede.
- Los profesionales tienen horarios por sede.
- Los usuarios tienen permisos por sede.
- WhatsApp puede operar centralizado o asociado a sede.

## Pendientes controlados

Para cerrar multisede productivo total aun se recomienda:

1. Integrar validacion de `user_location_access` en todos los repositorios operativos existentes.
2. Aplicar filtros `locationId` en todos los endpoints existentes de dashboard, prospectos, pedidos, catalogo y reportes.
3. Extender el orquestador de IA para persistir `location_id` cuando detecte sede en una conversacion.
4. Validar disponibilidad real de agenda contra `professional_location_schedule` antes de confirmar.
5. Agregar pruebas automatizadas de integracion con PostgreSQL.

## Comandos de validacion

```powershell
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d --build
```

Si Flyway reporta `checksum mismatch`, no modificar migraciones antiguas. En entorno local, limpiar volumen con `down -v`. En entorno con datos reales, aplicar `flyway repair` solo tras revision tecnica.
