-- V47: Transbank Webpay Plus Mall payment support
-- Añade soporte para transacciones Mall con múltiples comercios hijos

alter table booking_payment
    add column if not exists transbank_token_ws varchar(100),
    add column if not exists transbank_buy_order varchar(100),
    add column if not exists transbank_session_id varchar(100),
    add column if not exists transbank_card_detail varchar(50),
    add column if not exists transbank_accounting_date date,
    add column if not exists transbank_transaction_date timestamp with time zone,
    add column if not exists transbank_vci varchar(20),
    add column if not exists transbank_card_last_digits varchar(4),
    add column if not exists transbank_card_expiration_date varchar(4),
    add column if not exists transbank_response_code varchar(2),
    add column if not exists transbank_status varchar(30),
    add column if not exists transbank_raw_response jsonb,
    add column if not exists transbank_committed_at timestamp with time zone;

-- Tabla para detalle de transacciones hijo (Mall)
create table if not exists booking_payment_mall_detail (
    id uuid primary key,
    payment_id uuid not null,
    business_id uuid not null,
    booking_id uuid not null,
    child_commerce_code varchar(12) not null,
    child_buy_order varchar(26) not null,
    amount numeric(12, 2) not null,
    currency varchar(3) not null default 'CLP',
    authorization_code varchar(20),
    payment_type_code varchar(20),
    response_code varchar(10),
    installments_number integer,
    status varchar(30),
    description varchar(255),
    reference varchar(255),
    transbank_raw_response jsonb,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_mall_detail_payment
        foreign key (payment_id) references booking_payment (id) on delete cascade,
    constraint fk_mall_detail_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_mall_detail_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint chk_mall_detail_amount check (amount >= 0),
    constraint chk_mall_detail_currency check (currency = upper(currency) and length(currency) = 3)
);

create index if not exists idx_mall_detail_payment
    on booking_payment_mall_detail (payment_id, created_at desc);

create index if not exists idx_mall_detail_child_buy_order
    on booking_payment_mall_detail (child_buy_order)
    where child_buy_order is not null;

create index if not exists idx_mall_detail_child_commerce
    on booking_payment_mall_detail (child_commerce_code, child_buy_order)
    where child_commerce_code is not null;

-- Índices adicionales en booking_payment para Mall
create index if not exists idx_booking_payment_transbank_token
    on booking_payment (transbank_token_ws)
    where transbank_token_ws is not null;

create index if not exists idx_booking_payment_transbank_buy_order
    on booking_payment (transbank_buy_order)
    where transbank_buy_order is not null;

create index if not exists idx_booking_payment_transbank_session
    on booking_payment (transbank_session_id)
    where transbank_session_id is not null;

-- payment_purpose extendido
alter table booking_payment
    drop constraint if exists chk_booking_payment_purpose;

alter table booking_payment
    add constraint chk_booking_payment_purpose
        check (payment_purpose in ('DEPOSIT', 'FULL', 'MANUAL', 'MALL'));
