-- V45: extiende booking_payment con columnas de proveedor real,
-- normaliza referencia externa y proposito de pago.

alter table booking_payment
    add column if not exists provider_preference_id varchar(160),
    add column if not exists provider_external_reference varchar(160),
    add column if not exists provider_status_detail varchar(160),
    add column if not exists provider_raw_status varchar(80),
    add column if not exists provider_payment_method varchar(80),
    add column if not exists provider_installments integer,
    add column if not exists payer_email varchar(255),
    add column if not exists payment_purpose varchar(30) not null default 'DEPOSIT',
    add column if not exists reconciled_at timestamp with time zone,
    add column if not exists webhook_raw_body text,
    add column if not exists webhook_received_at timestamp with time zone,
    add constraint chk_booking_payment_purpose
        check (payment_purpose in ('DEPOSIT', 'FULL', 'MANUAL'));

-- índice único: business + provider + provider_payment_id (ya existe en V35)
-- drop index if exists we keep the existing uq_booking_payment_provider_payment

-- índice único parcial: business_id + provider + provider_external_reference
create unique index if not exists uq_booking_payment_external_ref
    on booking_payment (business_id, provider, provider_external_reference)
    where provider_external_reference is not null;

-- índice único parcial: business_id + provider_preference_id
create unique index if not exists uq_booking_payment_preference
    on booking_payment (business_id, provider_preference_id)
    where provider_preference_id is not null;

-- índice para búsqueda por provider_preference_id
create index if not exists idx_booking_payment_preference_lookup
    on booking_payment (provider, provider_preference_id)
    where provider_preference_id is not null;

-- índice para reconciliación por provider real payment id
create index if not exists idx_booking_payment_provider_payment_lookup
    on booking_payment (provider, provider_payment_id)
    where provider_payment_id is not null;