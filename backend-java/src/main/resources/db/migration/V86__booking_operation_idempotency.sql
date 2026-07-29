create table if not exists agenda_booking_operation_idempotency (
    id uuid primary key,
    business_id uuid not null,
    operation_type varchar(80) not null,
    idempotency_key varchar(255) not null,
    request_hash varchar(80) not null,
    source varchar(80) not null,
    status varchar(40) not null,
    booking_id uuid,
    result jsonb not null default '{}'::jsonb,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    completed_at timestamp with time zone,
    constraint fk_agenda_booking_operation_idempotency_business
        foreign key (business_id) references business(id) on delete cascade,
    constraint fk_agenda_booking_operation_idempotency_booking
        foreign key (booking_id) references booking(id) on delete set null,
    constraint uq_agenda_booking_operation_idempotency_key
        unique (business_id, operation_type, idempotency_key),
    constraint ck_agenda_booking_operation_idempotency_status
        check (status in ('IN_PROGRESS', 'COMPLETED'))
);

create index if not exists idx_agenda_booking_operation_idempotency_booking
    on agenda_booking_operation_idempotency (business_id, booking_id);

comment on table agenda_booking_operation_idempotency is
    'Resultado persistido de operaciones de reserva idempotentes por negocio, operacion y clave funcional.';
