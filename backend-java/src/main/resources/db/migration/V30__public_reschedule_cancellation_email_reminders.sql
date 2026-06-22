-- V30: enlaces publicos de reprogramacion/cancelacion, correos simulables,
-- recordatorios multicanal y trazabilidad ampliada de booking.

create extension if not exists pgcrypto;

alter table booking
    drop constraint if exists chk_booking_status;

alter table booking
    alter column status type varchar(30),
    add column if not exists reschedule_count integer not null default 0,
    add column if not exists last_confirmation_sent_at timestamp with time zone,
    add column if not exists last_email_sent_at timestamp with time zone,
    add column if not exists cancellation_requested_at timestamp with time zone,
    add column if not exists cancelled_at timestamp with time zone;

alter table booking
    add constraint chk_booking_status
        check (status in (
            'REQUESTED',
            'PENDIENTE_CONFIRMACION',
            'TEMPORARY',
            'CONFIRMED',
            'RESCHEDULED',
            'REPROGRAMADA',
            'CANCELLED',
            'CANCELADA',
            'COMPLETED',
            'EXPIRADA',
            'EXPIRED',
            'RELEASED',
            'NO_SHOW',
            'ATTENDED'
        ));

create table if not exists booking_reschedule_link (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    token_hash varchar(128) not null,
    reschedule_url text not null,
    proposed_starts_at timestamp with time zone not null,
    proposed_ends_at timestamp with time zone not null,
    proposed_location_id uuid not null,
    proposed_service_id uuid,
    proposed_professional_id uuid,
    proposed_room_id uuid,
    reason text,
    status varchar(20) not null default 'ACTIVE',
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    rejected_at timestamp with time zone,
    created_by_channel varchar(30) not null default 'ADMIN',
    created_by_user_id uuid,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_reschedule_link_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_reschedule_link_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint fk_booking_reschedule_link_location
        foreign key (proposed_location_id) references business_location (id) on delete cascade,
    constraint fk_booking_reschedule_link_service
        foreign key (proposed_service_id) references aesthetic_service (id) on delete set null,
    constraint fk_booking_reschedule_link_professional
        foreign key (proposed_professional_id) references aesthetic_professional (id) on delete set null,
    constraint fk_booking_reschedule_link_room
        foreign key (proposed_room_id) references agenda_room (id) on delete set null,
    constraint fk_booking_reschedule_link_user
        foreign key (created_by_user_id) references user_account (id) on delete set null,
    constraint uq_booking_reschedule_link_token_hash unique (token_hash),
    constraint chk_booking_reschedule_link_status
        check (status in ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED', 'REJECTED')),
    constraint chk_booking_reschedule_link_range
        check (proposed_ends_at > proposed_starts_at),
    constraint chk_booking_reschedule_link_expiration
        check (expires_at > created_at)
);

create index if not exists idx_booking_reschedule_link_booking_status
    on booking_reschedule_link (business_id, booking_id, status);

create index if not exists idx_booking_reschedule_link_expiration
    on booking_reschedule_link (status, expires_at);

create unique index if not exists uq_booking_reschedule_link_active_per_booking
    on booking_reschedule_link (business_id, booking_id)
    where status = 'ACTIVE';

create table if not exists booking_cancellation_link (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    token_hash varchar(128) not null,
    cancellation_url text not null,
    status varchar(20) not null default 'ACTIVE',
    cancellation_reason text,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    created_by_channel varchar(30) not null default 'ADMIN',
    created_by_user_id uuid,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_cancellation_link_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_cancellation_link_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint fk_booking_cancellation_link_user
        foreign key (created_by_user_id) references user_account (id) on delete set null,
    constraint uq_booking_cancellation_link_token_hash unique (token_hash),
    constraint chk_booking_cancellation_link_status
        check (status in ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED')),
    constraint chk_booking_cancellation_link_expiration
        check (expires_at > created_at)
);

create index if not exists idx_booking_cancellation_link_booking_status
    on booking_cancellation_link (business_id, booking_id, status);

create index if not exists idx_booking_cancellation_link_expiration
    on booking_cancellation_link (status, expires_at);

create unique index if not exists uq_booking_cancellation_link_active_per_booking
    on booking_cancellation_link (business_id, booking_id)
    where status = 'ACTIVE';

create table if not exists booking_email_log (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid,
    recipient_email varchar(255) not null,
    subject varchar(240) not null,
    template_key varchar(80) not null,
    status varchar(20) not null,
    simulation boolean not null default true,
    failure_reason text,
    body_preview text,
    sent_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_email_log_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_email_log_booking
        foreign key (booking_id) references booking (id) on delete set null,
    constraint chk_booking_email_log_status
        check (status in ('STARTED', 'SENT', 'FAILED', 'SIMULATED', 'SKIPPED'))
);

create index if not exists idx_booking_email_log_booking_created
    on booking_email_log (business_id, booking_id, created_at desc);

alter table booking_reminder
    drop constraint if exists chk_booking_reminder_status,
    drop constraint if exists chk_booking_reminder_channel,
    drop constraint if exists uq_booking_reminder_type;

alter table booking_reminder
    alter column status type varchar(20),
    add column if not exists failure_reason text,
    add column if not exists template_key varchar(80);

update booking_reminder
set status = 'PENDING'
where status = 'SCHEDULED';

alter table booking_reminder
    add constraint chk_booking_reminder_status
        check (status in ('PENDING', 'SCHEDULED', 'SENT', 'FAILED', 'CANCELLED', 'SKIPPED')),
    add constraint chk_booking_reminder_channel
        check (channel_type in ('WHATSAPP', 'EMAIL')),
    add constraint uq_booking_reminder_type
        unique (business_id, booking_id, reminder_type, channel_type, scheduled_at);

drop index if exists idx_booking_slot_guard_pending_confirmation;

create index if not exists idx_booking_slot_guard_public_flow
    on booking (business_id, location_id, starts_at, status)
    where status in ('REQUESTED', 'PENDIENTE_CONFIRMACION', 'TEMPORARY', 'CONFIRMED', 'RESCHEDULED', 'REPROGRAMADA');

create index if not exists idx_booking_reminder_due_multichannel
    on booking_reminder (status, scheduled_at, channel_type);
