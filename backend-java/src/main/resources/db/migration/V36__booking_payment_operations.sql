-- V36: operaciones internas para pagos de reservas.

alter table booking_payment
    add column if not exists checkout_url text,
    add column if not exists checkout_expires_at timestamp with time zone,
    add column if not exists manual boolean not null default false;

create index if not exists idx_booking_payment_checkout_active
    on booking_payment (business_id, booking_id, checkout_expires_at desc)
    where status = 'PENDING' and checkout_url is not null;

