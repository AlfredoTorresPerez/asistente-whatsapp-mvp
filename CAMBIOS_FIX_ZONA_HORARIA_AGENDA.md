# Cambios - Fix zona horaria y visualizacion de agenda

## Problema detectado

Al crear una reserva desde WhatsApp para una hora local de Chile, la disponibilidad se generaba usando `ZoneOffset.UTC`.

Ejemplo observado:

- Solicitud del cliente: 12/06/2026 14:00 en Providencia.
- Hora persistida en `booking.starts_at`: 12/06/2026 10:00:00-04.

La reserva si se creaba y se confirmaba, pero quedaba desplazada cuatro horas respecto de la hora solicitada.

## Correcciones aplicadas

### 1. Agenda con zona horaria de sucursal

Archivo modificado:

```text
backend-java/src/main/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaService.java
```

Se reemplazo la construccion de horarios con UTC:

```java
request.date().atTime(cursor).atOffset(ZoneOffset.UTC)
```

por construccion con la zona horaria de la sucursal:

```java
request.date().atTime(cursor).atZone(locationZone).toOffsetDateTime()
```

La zona horaria se obtiene desde `business_location.timezone`. Si no existe o es invalida, se usa `America/Santiago` como respaldo controlado.

### 2. Listado de citas por solapamiento de rango

Archivo modificado:

```text
backend-java/src/main/java/com/asistentewhatsapp/bookings/infrastructure/BookingJdbcRepository.java
```

El listado mensual de citas ya no filtra solo por `starts_at between :from and :to`. Ahora usa solapamiento de rango:

```sql
b.starts_at < :to
and coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > :from
```

Esto hace que el listado sea consistente con la agenda diaria y evita perder citas por borde de rango.

## Resultado esperado

Una solicitud como:

```text
viernes 12 de junio 2026 a las 14:00 en Providencia
```

ahora debe persistirse como:

```text
2026-06-12 14:00:00-04
```

y debe visualizarse en la agenda mensual/lista diaria del 12 de junio.

## Nota de prueba

Los registros ya existentes creados antes de esta correccion no se recalculan automaticamente. Para validar la correccion, genera una nueva reserva desde WhatsApp despues de levantar esta version.
