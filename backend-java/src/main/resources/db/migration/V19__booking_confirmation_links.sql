-- Correccion MVP: asistente de negocios por WhatsApp monocanal y centro estetico multisucursal.
-- Agrega reserva temporal, enlace publico de confirmacion, expiracion y liberacion de cupo.

alter table booking
    drop constraint if exists chk_booking_status;

alter table booking
    add constraint chk_booking_status
        check (status in (
            'REQUESTED', 'TEMPORARY', 'CONFIRMED', 'RESCHEDULED', 'CANCELLED',
            'COMPLETED', 'EXPIRED', 'RELEASED', 'NO_SHOW', 'ATTENDED'
        ));

create table if not exists booking_confirmation_link (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    token_hash varchar(64) not null,
    confirmation_url varchar(700) not null,
    status varchar(20) not null default 'GENERATED',
    expires_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    opened_at timestamp with time zone,
    confirmed_at timestamp with time zone,
    expired_at timestamp with time zone,
    invalidated_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_confirmation_link_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_confirmation_link_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint uq_booking_confirmation_link_token_hash unique (token_hash),
    constraint chk_booking_confirmation_link_status
        check (status in ('GENERATED', 'SENT', 'OPENED', 'CONFIRMED', 'EXPIRED', 'INVALIDATED')),
    constraint chk_booking_confirmation_link_expiration check (expires_at > created_at)
);

create index if not exists idx_booking_confirmation_link_booking_status
    on booking_confirmation_link (business_id, booking_id, status);

create index if not exists idx_booking_confirmation_link_expiration
    on booking_confirmation_link (status, expires_at);

create unique index if not exists uq_booking_confirmation_link_active_per_booking
    on booking_confirmation_link (business_id, booking_id)
    where status in ('GENERATED', 'SENT', 'OPENED');

create index if not exists idx_booking_slot_guard
    on booking (business_id, location_id, starts_at, status)
    where status in ('REQUESTED', 'TEMPORARY', 'CONFIRMED', 'RESCHEDULED');
