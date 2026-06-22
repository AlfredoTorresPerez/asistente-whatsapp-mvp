create table business_location (
    id uuid primary key,
    business_id uuid not null,
    code varchar(50) not null,
    name varchar(150) not null,
    address varchar(255),
    city varchar(120),
    commune varchar(120),
    phone varchar(30),
    whatsapp_number varchar(30),
    timezone varchar(60) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_business_location_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_business_location_business_code
        unique (business_id, code)
);

create index idx_business_location_business_active
    on business_location (business_id, active);

create index idx_business_location_business_name
    on business_location (business_id, name);

insert into business_location (
    id,
    business_id,
    code,
    name,
    address,
    city,
    commune,
    phone,
    whatsapp_number,
    timezone,
    active
)
select
    (
        substring(location_hash from 1 for 8) || '-' ||
        substring(location_hash from 9 for 4) || '-' ||
        substring(location_hash from 13 for 4) || '-' ||
        substring(location_hash from 17 for 4) || '-' ||
        substring(location_hash from 21 for 12)
    )::uuid,
    id,
    'principal',
    business_name || ' Principal',
    address,
    'Santiago',
    null,
    support_phone,
    support_phone,
    timezone,
    true
from (
    select b.*, md5(b.id::text || ':default-location') as location_hash
    from business b
) seeded_business
on conflict (business_id, code) do nothing;

update business_location
set name = 'Centro Estetico Bella - Sede Principal',
    address = 'Av. Providencia 2450, Santiago',
    city = 'Santiago',
    commune = 'Providencia',
    phone = '+56955550100',
    whatsapp_number = '+56955550100',
    timezone = 'America/Santiago',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'principal';

alter table booking
    add column location_id uuid;

alter table booking
    add constraint fk_booking_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index idx_booking_business_location_starts_at
    on booking (business_id, location_id, starts_at);

update booking b
set location_id = bl.id,
    location = coalesce(b.location, bl.name),
    updated_at = current_timestamp
from business_location bl
where bl.business_id = b.business_id
  and bl.code = 'principal'
  and b.location_id is null;

alter table conversation
    add column location_id uuid;

alter table conversation
    add constraint fk_conversation_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index idx_conversation_business_location_last_message
    on conversation (business_id, location_id, last_message_at desc);

create table aesthetic_professional_location (
    id uuid primary key,
    business_id uuid not null,
    professional_id uuid not null,
    location_id uuid not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_professional_location_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_aesthetic_professional_location_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete cascade,
    constraint fk_aesthetic_professional_location_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_aesthetic_professional_location
        unique (business_id, professional_id, location_id)
);

create index idx_aesthetic_professional_location_business_location
    on aesthetic_professional_location (business_id, location_id, active);

insert into aesthetic_professional_location (
    id,
    business_id,
    professional_id,
    location_id,
    active
)
select
    (
        substring(link_hash from 1 for 8) || '-' ||
        substring(link_hash from 9 for 4) || '-' ||
        substring(link_hash from 13 for 4) || '-' ||
        substring(link_hash from 17 for 4) || '-' ||
        substring(link_hash from 21 for 12)
    )::uuid,
    business_id,
    professional_id,
    location_id,
    true
from (
    select
        ap.business_id,
        ap.id as professional_id,
        bl.id as location_id,
        md5(ap.id::text || ':' || bl.id::text) as link_hash
    from aesthetic_professional ap
    join business_location bl
      on bl.business_id = ap.business_id
     and bl.code = 'principal'
) default_professional_location
on conflict (business_id, professional_id, location_id) do nothing;
