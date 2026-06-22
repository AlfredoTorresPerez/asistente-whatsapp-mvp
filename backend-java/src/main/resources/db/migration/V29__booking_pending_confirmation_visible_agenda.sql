-- V29: reservas pendientes visibles y confirmacion transaccional clara.
-- Convierte el estado tecnico TEMPORARY en PENDIENTE_CONFIRMACION y EXPIRADA.

alter table booking
    drop constraint if exists chk_booking_status;

-- PENDIENTE_CONFIRMACION tiene 23 caracteres. La columna original venia como varchar(20).
-- Se amplia antes de normalizar estados para evitar SQL State 22001.
alter table booking
    alter column status type varchar(30);

update booking
set status = 'PENDIENTE_CONFIRMACION', updated_at = current_timestamp
where status = 'TEMPORARY';

update booking
set status = 'EXPIRADA', updated_at = current_timestamp
where status in ('EXPIRED', 'RELEASED');

alter table booking
    add constraint chk_booking_status
        check (status in (
            'REQUESTED',
            'PENDIENTE_CONFIRMACION',
            'TEMPORARY',
            'CONFIRMED',
            'RESCHEDULED',
            'CANCELLED',
            'COMPLETED',
            'EXPIRADA',
            'EXPIRED',
            'RELEASED',
            'NO_SHOW',
            'ATTENDED'
        ));

create index if not exists idx_booking_slot_guard_pending_confirmation
    on booking (business_id, location_id, starts_at, status)
    where status in ('REQUESTED', 'PENDIENTE_CONFIRMACION', 'TEMPORARY', 'CONFIRMED', 'RESCHEDULED');

insert into booking_status_history (id, business_id, booking_id, previous_status, new_status, reason, source)
select gen_random_uuid(), b.business_id, b.id, null, b.status,
       'Estado sincronizado para reservas pendientes visibles en agenda.', 'SYSTEM'
from booking b
where b.status in ('PENDIENTE_CONFIRMACION', 'EXPIRADA')
  and not exists (
      select 1
      from booking_status_history h
      where h.business_id = b.business_id
        and h.booking_id = b.id
        and h.new_status = b.status
  );
