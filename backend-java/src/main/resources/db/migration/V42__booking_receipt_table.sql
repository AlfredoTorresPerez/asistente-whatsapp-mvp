-- V42: Crea tabla booking_receipt para comprobantes de reserva

create table if not exists booking_receipt (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    receipt_number varchar(255) not null,
    status varchar(50) not null,
    generated_at timestamptz not null,
    created_at timestamptz default current_timestamp,
    updated_at timestamptz default current_timestamp
);

create index if not exists idx_booking_receipt_business_id on booking_receipt (business_id);
create index if not exists idx_booking_receipt_booking_id on booking_receipt (booking_id);

alter table booking_receipt
    add constraint fk_booking_receipt_business
    foreign key (business_id) references business(id);

alter table booking_receipt
    add constraint fk_booking_receipt_booking
    foreign key (booking_id) references booking(id);
