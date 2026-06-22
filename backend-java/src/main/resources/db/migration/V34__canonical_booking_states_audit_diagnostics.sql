-- V34: estados canonicos, auditoria forense y diagnostico de solapes.

alter table booking
    drop constraint if exists chk_booking_status;

update booking
set status = case status
    when 'REQUESTED' then 'SOLICITADA'
    when 'TEMPORARY' then 'PENDIENTE_CONFIRMACION'
    when 'CONFIRMED' then 'CONFIRMADA'
    when 'RESCHEDULED' then 'REPROGRAMADA'
    when 'CANCELLED' then 'CANCELADA'
    when 'COMPLETED' then 'ATENDIDA'
    when 'ATTENDED' then 'ATENDIDA'
    when 'NO_SHOW' then 'NO_ASISTE'
    when 'EXPIRED' then 'EXPIRADA'
    when 'RELEASED' then 'EXPIRADA'
    else status
end
where status in (
    'REQUESTED',
    'TEMPORARY',
    'CONFIRMED',
    'RESCHEDULED',
    'CANCELLED',
    'COMPLETED',
    'ATTENDED',
    'NO_SHOW',
    'EXPIRED',
    'RELEASED'
);

alter table booking
    add constraint chk_booking_status
        check (status in (
            'SOLICITADA',
            'PENDIENTE_CONFIRMACION',
            'PENDIENTE_PAGO',
            'CONFIRMADA',
            'REPROGRAMACION_PENDIENTE',
            'REPROGRAMADA',
            'CANCELADA',
            'EXPIRADA',
            'ATENDIDA',
            'NO_ASISTE'
        ));

alter table booking_status_history
    add column if not exists correlation_id varchar(80),
    add column if not exists message_id uuid,
    add column if not exists link_id uuid,
    add column if not exists ip_address varchar(80),
    add column if not exists user_agent varchar(500),
    add column if not exists version_before integer,
    add column if not exists version_after integer,
    add column if not exists metadata jsonb not null default '{}'::jsonb;

create index if not exists idx_booking_status_history_correlation
    on booking_status_history (business_id, correlation_id, created_at desc)
    where correlation_id is not null;

create or replace view booking_active_overlap_diagnostics as
select
    a.business_id,
    a.id as booking_id,
    b.id as conflicting_booking_id,
    case
        when a.professional_id is not null and a.professional_id = b.professional_id then 'PROFESSIONAL'
        when a.room_id is not null and a.room_id = b.room_id then 'ROOM'
        else 'UNKNOWN'
    end as conflict_type,
    a.professional_id,
    a.room_id,
    a.starts_at,
    coalesce(a.ends_at, a.starts_at + make_interval(mins => a.duration_minutes)) as ends_at,
    b.starts_at as conflicting_starts_at,
    coalesce(b.ends_at, b.starts_at + make_interval(mins => b.duration_minutes)) as conflicting_ends_at,
    a.status,
    b.status as conflicting_status
from booking a
join booking b
  on b.business_id = a.business_id
 and b.id > a.id
 and b.status in ('SOLICITADA', 'PENDIENTE_CONFIRMACION', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE', 'REPROGRAMADA')
 and tstzrange(b.starts_at, coalesce(b.ends_at, b.starts_at + make_interval(mins => b.duration_minutes)), '[)')
     && tstzrange(a.starts_at, coalesce(a.ends_at, a.starts_at + make_interval(mins => a.duration_minutes)), '[)')
 and (
    (a.professional_id is not null and a.professional_id = b.professional_id)
    or (a.room_id is not null and a.room_id = b.room_id)
 )
where a.status in ('SOLICITADA', 'PENDIENTE_CONFIRMACION', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE', 'REPROGRAMADA');
