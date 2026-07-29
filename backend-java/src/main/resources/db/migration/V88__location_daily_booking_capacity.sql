alter table business_location
    add column if not exists daily_booking_capacity integer;

alter table business_location
    drop constraint if exists chk_business_location_daily_booking_capacity,
    add constraint chk_business_location_daily_booking_capacity
        check (daily_booking_capacity is null or daily_booking_capacity > 0);

create index if not exists idx_booking_location_daily_capacity_lookup
    on booking (business_id, location_id, starts_at)
    where status in ('REQUESTED', 'TEMPORARY', 'PENDIENTE_CONFIRMACION', 'CONFIRMED', 'RESCHEDULED',
                     'REPROGRAMADA', 'SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE');

comment on column business_location.daily_booking_capacity is
    'Capacidad maxima diaria agregada de reservas activas para la sede. Null significa sin limite agregado.';
