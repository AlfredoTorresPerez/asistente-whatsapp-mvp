-- V39: Calendar integration tables for external calendar sync (Google Calendar, Outlook)
-- Schema: calendar_integration_account stores OAuth tokens per business per provider
-- Schema: booking_calendar_sync tracks sync state per booking per provider

create table if not exists calendar_integration_account (
    id uuid primary key,
    business_id uuid not null references business(id) on delete cascade,
    provider varchar(20) not null check (provider in ('GOOGLE', 'OUTLOOK')),
    email varchar(255) not null,
    access_token_encrypted text not null,
    refresh_token_encrypted text,
    token_expires_at timestamptz,
    calendar_id varchar(255),
    calendar_summary varchar(255),
    active boolean not null default true,
    connected_at timestamptz not null default current_timestamp,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    unique (business_id, provider)
);

create table if not exists booking_calendar_sync (
    id uuid primary key,
    booking_id uuid not null references booking(id) on delete cascade,
    business_id uuid not null references business(id) on delete cascade,
    provider varchar(20) not null check (provider in ('GOOGLE', 'OUTLOOK')),
    external_event_id varchar(255),
    sync_status varchar(20) not null check (sync_status in ('PENDING', 'SYNCED', 'FAILED', 'CANCELLED')),
    sync_action varchar(20) not null check (sync_action in ('CREATE', 'UPDATE', 'DELETE')),
    error_message text,
    retry_count int not null default 0,
    last_sync_attempt_at timestamptz,
    last_successful_sync_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    unique (booking_id, provider)
);

create index if not exists idx_booking_calendar_sync_booking on booking_calendar_sync(booking_id);
create index if not exists idx_booking_calendar_sync_status on booking_calendar_sync(sync_status);
create index if not exists idx_calendar_int_account_business on calendar_integration_account(business_id);
create index if not exists idx_calendar_int_account_provider on calendar_integration_account(provider);
