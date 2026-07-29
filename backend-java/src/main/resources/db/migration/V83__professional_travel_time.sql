-- =============================================================================
-- V83: TRASLADO PROFESIONAL ENTRE SUCURSALES
--
-- Agrega coordenadas opcionales a sucursales y una matriz configurable de
-- tiempos de traslado entre sucursales por negocio.
-- =============================================================================

alter table business_location
    add column if not exists latitude numeric(9,6),
    add column if not exists longitude numeric(9,6);

create table if not exists business_location_travel_time (
    id uuid primary key,
    business_id uuid not null,
    from_location_id uuid not null,
    to_location_id uuid not null,
    travel_minutes integer not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,

    constraint fk_bltt_business foreign key (business_id) references business(id) on delete cascade,
    constraint fk_bltt_from_location foreign key (from_location_id) references business_location(id) on delete cascade,
    constraint fk_bltt_to_location foreign key (to_location_id) references business_location(id) on delete cascade,
    constraint chk_bltt_travel_minutes check (travel_minutes >= 0),
    constraint chk_bltt_different_locations check (from_location_id <> to_location_id),
    constraint uq_bltt_business_locations unique (business_id, from_location_id, to_location_id)
);

create index if not exists idx_bltt_business_from_to_active
    on business_location_travel_time (business_id, from_location_id, to_location_id, active);

create index if not exists idx_booking_professional_location_time
    on booking (business_id, professional_id, location_id, starts_at, ends_at)
    where professional_id is not null
      and status in (
          'REQUESTED', 'TEMPORARY', 'PENDIENTE_CONFIRMACION', 'CONFIRMED', 'RESCHEDULED', 'REPROGRAMADA',
          'SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE'
      );

comment on table business_location_travel_time is
    'Matriz de minutos de traslado entre sucursales usada para validar agenda de profesionales multi-sucursal.';
