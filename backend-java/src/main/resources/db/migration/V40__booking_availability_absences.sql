-- V40: booking availability and absences.
-- Agrega tabla de ausencias del profesional, capacidad diaria, anticipacion minima configurable
-- e indice unico parcial para evitar duplicados de reservas activas por cliente.

create table if not exists professional_absence (
    id uuid primary key,
    business_id uuid not null,
    professional_id uuid not null,
    absence_type varchar(30) not null check (absence_type in ('VACATION', 'SICK_LEAVE', 'TRAINING', 'OTHER')),
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    reason text,
    active boolean not null default true,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint fk_professional_absence_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_professional_absence_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete cascade,
    constraint chk_professional_absence_time_range
        check (ends_at > starts_at)
);

create index if not exists idx_professional_absence_professional_active
    on professional_absence (business_id, professional_id, active)
    where active = true;

create index if not exists idx_professional_absence_date_range
    on professional_absence (business_id, professional_id, starts_at, ends_at)
    where active = true;

alter table aesthetic_professional
    add column if not exists max_daily_bookings integer;

alter table aesthetic_professional
    drop constraint if exists chk_aesthetic_professional_max_daily_bookings,
    add constraint chk_aesthetic_professional_max_daily_bookings
        check (max_daily_bookings is null or max_daily_bookings > 0);

alter table agenda_business_hours
    add column if not exists min_advance_notice_minutes integer not null default 1440;

alter table agenda_business_hours
    drop constraint if exists chk_agenda_business_hours_min_advance,
    add constraint chk_agenda_business_hours_min_advance
        check (min_advance_notice_minutes >= 0);

alter table agenda_professional_hours
    add column if not exists min_advance_notice_minutes integer;

alter table agenda_professional_hours
    drop constraint if exists chk_agenda_professional_hours_min_advance,
    add constraint chk_agenda_professional_hours_min_advance
        check (min_advance_notice_minutes is null or min_advance_notice_minutes >= 0);

-- Indice unico parcial: evita que un mismo cliente tenga 2 reservas activas
-- en el mismo horario con el mismo profesional
do $$
begin
    if not exists (
        select 1 from pg_class
        where relname = 'uq_booking_customer_professional_active'
          and relnamespace = (select oid from pg_namespace where nspname = current_schema())
    ) then
        create unique index uq_booking_customer_professional_active
            on booking (customer_id, professional_id, starts_at)
            where professional_id is not null
              and status not in ('CANCELADA', 'EXPIRADA', 'ATENDIDA', 'NO_ASISTE');
    end if;
end $$;
