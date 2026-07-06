# Cambios ejecutados: reservas pendientes visibles en agenda

## Objetivo

Aplicar el flujo productivo para que una reserva generada desde WhatsApp no viva solo como enlace publico, sino como registro operativo visible en la agenda administrativa.

## Cambios funcionales

1. La reserva generada por agenda conversacional se crea como `PENDIENTE_CONFIRMACION`.
2. La reserva pendiente queda visible en la agenda y bloquea temporalmente el cupo.
3. La confirmacion del enlace cambia la reserva a `CONFIRMED` de forma idempotente.
4. La expiracion del enlace cambia la reserva a `EXPIRADA` y libera el cupo.
5. La pantalla publica de confirmacion diferencia reserva expirada, reserva confirmada y error real de disponibilidad.
6. La agenda administrativa muestra el filtro `PENDIENTE_CONFIRMACION`.
7. Se mantiene compatibilidad con datos antiguos `TEMPORARY`, `EXPIRED` y `RELEASED`.

## Cambios tecnicos

### Backend Java

- `BookingConfirmationService`
  - Confirmacion con lectura bloqueante `findByTokenHashForUpdate`.
  - Confirmacion idempotente si la reserva ya esta `CONFIRMED`.
  - Expiracion explicita a `EXPIRADA`.
  - Mensajes de error especificos para reserva cancelada, cerrada o cupo ocupado.
  - Historial de estado al pasar a pendiente, confirmada o expirada.

- `BookingConfirmationJdbcRepository`
  - Nuevo metodo `findByTokenHashForUpdate` con bloqueo pesimista `FOR UPDATE OF l, b`.
  - Estados activos incluyen `PENDIENTE_CONFIRMACION`.
  - Expiracion masiva pasa reservas pendientes a `EXPIRADA`.

- `CompleteAgendaJdbcRepository`
  - La reserva temporal se inserta como `PENDIENTE_CONFIRMACION`.
  - El vencimiento por `temporary_expires_at` cambia la reserva a `EXPIRADA`.
  - Tambien marca el enlace activo como `EXPIRED`.

- `BookingService`
  - Estado por defecto para nuevas citas: `PENDIENTE_CONFIRMACION`.
  - Compatibilidad de entrada: `TEMPORARY` y `TEMPORAL` se normalizan a `PENDIENTE_CONFIRMACION`.
  - Compatibilidad de entrada: `EXPIRED`, `RELEASED` y `LIBERADA` se normalizan a `EXPIRADA`.

### Base de datos

Nueva migracion:

- `backend-java/src/main/resources/db/migration/V29__booking_pending_confirmation_visible_agenda.sql`

La migracion:

- Convierte reservas antiguas `TEMPORARY` a `PENDIENTE_CONFIRMACION`.
- Convierte `EXPIRED` y `RELEASED` a `EXPIRADA`.
- Actualiza el `CHECK` de estados permitidos.
- Agrega indice de proteccion de cupo para reservas pendientes.
- Inserta historial inicial de sincronizacion si corresponde.

### Frontend React

- Filtros de agenda actualizados.
- Etiquetas de estado actualizadas.
- Pagina publica de confirmacion con mensaje especifico desde la API.
- Nuevas citas administrativas se crean por defecto como `PENDIENTE_CONFIRMACION`.

## Flujo resultante

```text
Cliente solicita hora por WhatsApp
-> IA valida servicio, fecha, hora y sucursal
-> Backend crea booking con estado PENDIENTE_CONFIRMACION
-> El cupo queda ocupado temporalmente
-> Agenda administrativa muestra la reserva pendiente
-> Cliente confirma enlace
-> Backend bloquea reserva y enlace en transaccion
-> Si sigue valida, estado pasa a CONFIRMED
-> Si vence, estado pasa a EXPIRADA y el cupo queda liberado
```

## Validacion pendiente

No fue posible ejecutar compilacion Maven ni compilacion frontend porque el entorno no tiene dependencias instaladas y no pudo descargar paquetes externos. Los cambios quedaron aplicados en codigo fuente y migraciones.
