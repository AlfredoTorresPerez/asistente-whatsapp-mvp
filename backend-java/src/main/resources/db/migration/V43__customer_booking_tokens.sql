create table if not exists customer_bookings_token (
    id uuid primary key,
    business_id uuid not null,
    token_hash varchar(64) not null,
    phone_digits varchar(20) not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_customer_bookings_token_business
        foreign key (business_id) references business(id) on delete cascade,
    constraint uq_customer_bookings_token_hash
        unique (token_hash)
);

create index idx_customer_bookings_token_expires
    on customer_bookings_token (expires_at)
    where used_at is null;
