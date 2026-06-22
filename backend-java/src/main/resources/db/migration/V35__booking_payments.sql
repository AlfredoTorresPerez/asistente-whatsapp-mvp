-- V35: pagos asociados directamente a reservas.

create table if not exists booking_payment (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    provider varchar(60) not null,
    provider_payment_id varchar(160),
    idempotency_key varchar(160),
    amount numeric(12, 2) not null,
    currency varchar(3) not null default 'CLP',
    status varchar(30) not null,
    raw_payload jsonb not null default '{}'::jsonb,
    metadata jsonb not null default '{}'::jsonb,
    approved_at timestamp with time zone,
    rejected_at timestamp with time zone,
    expired_at timestamp with time zone,
    refunded_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_payment_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_payment_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint chk_booking_payment_amount
        check (amount >= 0),
    constraint chk_booking_payment_currency
        check (currency = upper(currency) and length(currency) = 3),
    constraint chk_booking_payment_status
        check (status in ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'REFUNDED')),
    constraint uq_booking_payment_provider_payment
        unique (business_id, provider, provider_payment_id),
    constraint uq_booking_payment_idempotency
        unique (business_id, idempotency_key)
);

create index if not exists idx_booking_payment_booking_created
    on booking_payment (business_id, booking_id, created_at desc);

create index if not exists idx_booking_payment_status
    on booking_payment (business_id, status, created_at desc);

create index if not exists idx_booking_payment_provider
    on booking_payment (provider, provider_payment_id)
    where provider_payment_id is not null;
