# Fix agendar / reservar por WhatsApp: sucursal y datos demo

## Problema observado

El cliente escribia solicitudes como:

- `Hola, quiero una limpieza facial para el Martes 16 de junio 2026 a las 18:00 en Providencia`
- `Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 16:00 en Providencia`

La IA extraia correctamente `sede=Providencia`, pero el flujo terminaba usando la sede heredada de la conversacion: `Centro Estetico Bella - Sede Principal`.

Como el servicio `Limpieza facial profunda` no estaba configurado para esa sede principal, el sistema devolvia:

```text
Limpieza facial profunda no está configurado para Centro Estetico Bella - Sede Principal.
```

Y la vista previa de IA respondia HTTP 500.

## Causa tecnica

1. La resolucion de sucursal podia tomar primero la sede de conversacion o el texto completo antes de priorizar la entidad exacta extraida por la IA.
2. La asignacion automatica de sede de conversacion podia escoger la sede principal cuando el texto contenia `Providencia`, porque la sede principal tenia comuna `Providencia`.
3. En bases ya existentes, algunas migraciones anteriores ya estaban aplicadas, por lo que no se reejecutaban los seeds de sedes, servicios, profesionales, cabinas y horarios.

## Cambios realizados

### Backend Java

- `TransactionalAgendaBookingService.java`
  - Ahora prioriza la sucursal extraida por IA/reglas (`sede=Providencia`) antes del texto completo y antes de la sede heredada de la conversacion.
  - `resolveLocation(...)` ya no devuelve automaticamente la unica sede activa si existe texto explicito que no coincide.

- `ConversationJdbcRepository.java`
  - La asignacion automatica de sede ahora prioriza coincidencia por nombre/codigo antes que comuna.
  - Esto evita asignar `Centro Estetico Bella - Sede Principal` solo porque su comuna sea `Providencia`.

- `WhatsAppMessageFormatter.java`
  - La respuesta de seleccion de sede ya no ofrece `Centro Estético Bella - Sede Principal` como sucursal operativa.

### Base de datos

Se agrego la migracion:

```text
V31__fix_location_resolution_agenda_seed.sql
```

Esta migracion:

- Asegura sedes activas: `Providencia`, `Maipu`, `Santiago Centro`.
- Desactiva la sede legacy `principal` como sucursal operativa demo.
- Asegura que los servicios esten configurados en las sedes operativas.
- Asegura profesionales por sede.
- Asegura horarios de negocio y profesionales.
- Asegura cabinas y relacion cabina-servicio.
- Limpia conversaciones demo asociadas a la sede principal para que la proxima solicitud use la sucursal escrita por el cliente.

## Resultado esperado

Para una solicitud como:

```text
Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 16:00 en Providencia
```

El flujo debe resolver:

```text
locationName=Providencia
serviceName=Limpieza facial profunda
```

Luego debe validar disponibilidad en Providencia, crear la reserva temporal si existe cupo, generar enlace y permitir confirmar la reserva.

## Validacion pendiente

No se pudo compilar localmente dentro de este entorno porque el wrapper de Maven no pudo descargar Apache Maven desde internet:

```text
wget: Failed to fetch https://repo.maven.apache.org/...
```

La validacion completa debe ejecutarse en el entorno Docker del proyecto con:

```powershell
docker compose -f docker-compose.local.yml down --remove-orphans
.\scripts\start_mvp_public_link.ps1
```
