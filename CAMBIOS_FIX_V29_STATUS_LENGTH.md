# Correccion V29: longitud de estado de reserva

## Problema detectado

Al iniciar el backend, Flyway fallaba en la migracion `V29__booking_pending_confirmation_visible_agenda.sql` con:

```text
ERROR: value too long for type character varying(20)
```

La causa era que la migracion intentaba guardar el estado `PENDIENTE_CONFIRMACION`, que tiene 23 caracteres, en la columna `booking.status`, definida originalmente como `varchar(20)`.

## Correccion aplicada

Se actualizo la migracion V29 para ampliar la columna antes de ejecutar los `update` de normalizacion:

```sql
alter table booking
    alter column status type varchar(30);
```

Luego se mantienen las reglas de negocio agregadas previamente:

- `TEMPORARY` se normaliza a `PENDIENTE_CONFIRMACION`.
- `EXPIRED` y `RELEASED` se normalizan a `EXPIRADA`.
- La agenda puede listar reservas pendientes visibles.
- Se mantiene el indice parcial para proteger cupos pendientes o confirmados.

## Impacto

La aplicacion ya no deberia caer durante el arranque por la migracion V29 en bases nuevas o bases donde V29 no alcanzo a aplicarse.
