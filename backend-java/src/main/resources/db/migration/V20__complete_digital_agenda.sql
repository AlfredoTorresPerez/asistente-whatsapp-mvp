-- Agenda digital completa para Asistente de Negocios por WhatsApp monocanal y centro estetico multisucursal.
-- Extiende la agenda MVP con sucursal, servicio, profesional, cabina, horarios, bloqueos,
-- disponibilidad calculada, reserva temporal, recordatorios e historial de estados.

create extension if not exists pgcrypto;

alter table aesthetic_service
    add column if not exists requires_room boolean not null default true,
    add column if not exists requires_deposit boolean not null default false,
    add column if not exists deposit_amount numeric(12, 2),
    add column if not exists preparation_minutes integer not null default 0,
    add column if not exists cleanup_minutes integer not null default 0;

alter table aesthetic_service
    drop constraint if exists chk_aesthetic_service_deposit_amount,
    add constraint chk_aesthetic_service_deposit_amount
        check (deposit_amount is null or deposit_amount >= 0),
    drop constraint if exists chk_aesthetic_service_buffers,
    add constraint chk_aesthetic_service_buffers
        check (preparation_minutes >= 0 and cleanup_minutes >= 0);

create table if not exists agenda_room (
    id uuid primary key,
    business_id uuid not null,
    location_id uuid not null,
    code varchar(60) not null,
    name varchar(140) not null,
    room_type varchar(80) not null,
    capacity integer not null default 1,
    active boolean not null default true,
    notes text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_room_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_room_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_agenda_room_business_location_code
        unique (business_id, location_id, code),
    constraint chk_agenda_room_capacity
        check (capacity > 0)
);

create index if not exists idx_agenda_room_location_active
    on agenda_room (business_id, location_id, active);

create table if not exists agenda_room_service (
    id uuid primary key,
    business_id uuid not null,
    room_id uuid not null,
    service_id uuid not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_room_service_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_room_service_room
        foreign key (room_id) references agenda_room (id) on delete cascade,
    constraint fk_agenda_room_service_service
        foreign key (service_id) references aesthetic_service (id) on delete cascade,
    constraint uq_agenda_room_service
        unique (business_id, room_id, service_id)
);

create index if not exists idx_agenda_room_service_service_active
    on agenda_room_service (business_id, service_id, active);

create table if not exists agenda_professional_service (
    id uuid primary key,
    business_id uuid not null,
    professional_id uuid not null,
    service_id uuid not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_professional_service_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_professional_service_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete cascade,
    constraint fk_agenda_professional_service_service
        foreign key (service_id) references aesthetic_service (id) on delete cascade,
    constraint uq_agenda_professional_service
        unique (business_id, professional_id, service_id)
);

create index if not exists idx_agenda_professional_service_service_active
    on agenda_professional_service (business_id, service_id, active);

create table if not exists agenda_business_hours (
    id uuid primary key,
    business_id uuid not null,
    location_id uuid not null,
    day_of_week integer not null,
    start_time time not null,
    end_time time not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_business_hours_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_business_hours_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint chk_agenda_business_hours_day
        check (day_of_week between 1 and 7),
    constraint chk_agenda_business_hours_range
        check (end_time > start_time),
    constraint uq_agenda_business_hours
        unique (business_id, location_id, day_of_week, start_time, end_time)
);

create index if not exists idx_agenda_business_hours_lookup
    on agenda_business_hours (business_id, location_id, day_of_week, active);

create table if not exists agenda_professional_hours (
    id uuid primary key,
    business_id uuid not null,
    professional_id uuid not null,
    location_id uuid not null,
    day_of_week integer not null,
    start_time time not null,
    end_time time not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_professional_hours_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_professional_hours_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete cascade,
    constraint fk_agenda_professional_hours_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint chk_agenda_professional_hours_day
        check (day_of_week between 1 and 7),
    constraint chk_agenda_professional_hours_range
        check (end_time > start_time),
    constraint uq_agenda_professional_hours
        unique (business_id, professional_id, location_id, day_of_week, start_time, end_time)
);

create index if not exists idx_agenda_professional_hours_lookup
    on agenda_professional_hours (business_id, location_id, professional_id, day_of_week, active);

create table if not exists agenda_holiday (
    id uuid primary key,
    business_id uuid not null,
    location_id uuid,
    holiday_date date not null,
    name varchar(160) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_holiday_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_holiday_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_agenda_holiday_scope
        unique (business_id, location_id, holiday_date, name)
);

create index if not exists idx_agenda_holiday_lookup
    on agenda_holiday (business_id, location_id, holiday_date, active);

create table if not exists agenda_block (
    id uuid primary key,
    business_id uuid not null,
    location_id uuid,
    professional_id uuid,
    room_id uuid,
    starts_at timestamp with time zone not null,
    ends_at timestamp with time zone not null,
    reason varchar(240) not null,
    active boolean not null default true,
    created_by_user_id uuid,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_agenda_block_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_agenda_block_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint fk_agenda_block_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete cascade,
    constraint fk_agenda_block_room
        foreign key (room_id) references agenda_room (id) on delete cascade,
    constraint fk_agenda_block_created_by
        foreign key (created_by_user_id) references user_account (id) on delete set null,
    constraint chk_agenda_block_range
        check (ends_at > starts_at)
);

create index if not exists idx_agenda_block_lookup
    on agenda_block (business_id, location_id, professional_id, room_id, starts_at, ends_at, active);

alter table booking
    add column if not exists service_id uuid,
    add column if not exists professional_id uuid,
    add column if not exists room_id uuid,
    add column if not exists ends_at timestamp with time zone,
    add column if not exists temporary_expires_at timestamp with time zone,
    add column if not exists source_channel varchar(30) not null default 'WHATSAPP',
    add column if not exists cancellation_reason text,
    add column if not exists reschedule_reason text,
    add column if not exists requires_deposit boolean not null default false,
    add column if not exists deposit_amount numeric(12, 2),
    add column if not exists payment_status varchar(30) not null default 'NOT_REQUIRED';

update booking
set ends_at = starts_at + (duration_minutes || ' minutes')::interval
where ends_at is null;

alter table booking
    drop constraint if exists fk_booking_aesthetic_service,
    add constraint fk_booking_aesthetic_service
        foreign key (service_id) references aesthetic_service (id) on delete set null,
    drop constraint if exists fk_booking_aesthetic_professional,
    add constraint fk_booking_aesthetic_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete set null,
    drop constraint if exists fk_booking_agenda_room,
    add constraint fk_booking_agenda_room
        foreign key (room_id) references agenda_room (id) on delete set null,
    drop constraint if exists chk_booking_complete_range,
    add constraint chk_booking_complete_range
        check (ends_at is null or ends_at > starts_at),
    drop constraint if exists chk_booking_payment_status,
    add constraint chk_booking_payment_status
        check (payment_status in ('NOT_REQUIRED', 'PENDING', 'PARTIAL', 'PAID', 'FAILED', 'REFUNDED')),
    drop constraint if exists chk_booking_source_channel,
    add constraint chk_booking_source_channel
        check (source_channel in ('WHATSAPP', 'ADMIN', 'SYSTEM'));

create index if not exists idx_booking_complete_agenda_lookup
    on booking (business_id, location_id, service_id, professional_id, room_id, starts_at, ends_at, status);

create table if not exists booking_status_history (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    previous_status varchar(30),
    new_status varchar(30) not null,
    reason text,
    actor_user_id uuid,
    source varchar(30) not null default 'SYSTEM',
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_status_history_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_status_history_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint fk_booking_status_history_actor
        foreign key (actor_user_id) references user_account (id) on delete set null
);

create index if not exists idx_booking_status_history_booking_created
    on booking_status_history (business_id, booking_id, created_at desc);

create table if not exists booking_reminder (
    id uuid primary key,
    business_id uuid not null,
    booking_id uuid not null,
    reminder_type varchar(40) not null,
    channel_type varchar(30) not null default 'WHATSAPP',
    scheduled_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    status varchar(20) not null default 'SCHEDULED',
    error_message text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_reminder_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_reminder_booking
        foreign key (booking_id) references booking (id) on delete cascade,
    constraint chk_booking_reminder_status
        check (status in ('SCHEDULED', 'SENT', 'FAILED', 'CANCELLED')),
    constraint chk_booking_reminder_channel
        check (channel_type in ('WHATSAPP')),
    constraint uq_booking_reminder_type
        unique (business_id, booking_id, reminder_type, scheduled_at)
);

create index if not exists idx_booking_reminder_due
    on booking_reminder (business_id, status, scheduled_at);

-- Datos demo seguros para la agenda completa.
insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes)
select gen_random_uuid(), bl.business_id, bl.id, lower(bl.code || '-cabina-1'), 'Cabina 1 - ' || bl.name, 'FACIAL_CORPORAL', 1, true, 'Cabina demo para servicios esteticos.'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, location_id, code) do nothing;

insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes)
select gen_random_uuid(), bl.business_id, bl.id, lower(bl.code || '-cabina-2'), 'Cabina 2 - ' || bl.name, 'DEPILACION_MANOS', 1, true, 'Cabina demo para servicios rapidos.'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, location_id, code) do nothing;

insert into agenda_room_service (id, business_id, room_id, service_id, active)
select gen_random_uuid(), r.business_id, r.id, s.id, true
from agenda_room r
join aesthetic_service s on s.business_id = r.business_id
where r.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, room_id, service_id) do nothing;

insert into agenda_professional_service (id, business_id, professional_id, service_id, active)
select gen_random_uuid(), p.business_id, p.id, s.id, true
from aesthetic_professional p
join aesthetic_service s on s.business_id = p.business_id
where p.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, professional_id, service_id) do nothing;

insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, d.day_of_week, '09:00'::time, '19:00'::time, true
from business_location bl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where bl.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, location_id, day_of_week, start_time, end_time) do nothing;

insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, 6, '10:00'::time, '14:00'::time, true
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, location_id, day_of_week, start_time, end_time) do nothing;

insert into agenda_professional_hours (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '09:00'::time, '18:00'::time, true
from aesthetic_professional_location apl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where apl.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do nothing;

insert into booking_status_history (id, business_id, booking_id, previous_status, new_status, reason, source)
select gen_random_uuid(), business_id, id, null, status, 'Historial inicial creado por agenda digital completa.', 'SYSTEM'
from booking b
where not exists (
    select 1 from booking_status_history h where h.business_id = b.business_id and h.booking_id = b.id
);
