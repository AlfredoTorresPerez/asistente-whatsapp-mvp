-- =============================================================================
-- V84: TRAZABILIDAD DE DESCARTE DE SLOTS
--
-- Registra por negocio/sucursal/servicio/profesional/sala cada slot evaluado y
-- descartado por el motor de disponibilidad, junto con la razon de descarte.
-- =============================================================================

create table if not exists agenda_slot_discard_trace (
    id uuid primary key,
    trace_key uuid not null,
    business_id uuid not null,
    location_id uuid not null,
    service_id uuid not null,
    professional_id uuid,
    room_id uuid,
    slot_starts_at timestamp with time zone not null,
    slot_ends_at timestamp with time zone not null,
    effective_starts_at timestamp with time zone not null,
    effective_ends_at timestamp with time zone not null,
    reason_code varchar(80) not null,
    source varchar(80) not null,
    occurrence_count integer not null default 1,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,

    constraint fk_asdt_business foreign key (business_id) references business(id) on delete cascade,
    constraint fk_asdt_location foreign key (location_id) references business_location(id) on delete cascade,
    constraint fk_asdt_service foreign key (service_id) references aesthetic_service(id) on delete cascade,
    constraint fk_asdt_professional foreign key (professional_id) references aesthetic_professional(id) on delete set null,
    constraint fk_asdt_room foreign key (room_id) references agenda_room(id) on delete set null,
    constraint uq_asdt_trace_key unique (trace_key),
    constraint chk_asdt_reason_code check (reason_code in ('PAST', 'MIN_ADVANCE', 'CONFLICT', 'BLOCKED')),
    constraint chk_asdt_source_non_empty check (length(trim(source)) > 0),
    constraint chk_asdt_time_range check (slot_ends_at > slot_starts_at and effective_ends_at > effective_starts_at)
);

create index if not exists idx_asdt_business_location_slot
    on agenda_slot_discard_trace (business_id, location_id, slot_starts_at desc);

create index if not exists idx_asdt_reason_source
    on agenda_slot_discard_trace (business_id, reason_code, source, updated_at desc);
