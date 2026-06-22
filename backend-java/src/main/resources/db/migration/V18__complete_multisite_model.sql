create extension if not exists pgcrypto;

-- Modelo multisede completo incremental. No modifica V17 ni elimina columnas legacy.

alter table business_location
    add column if not exists opening_hours jsonb not null default '{}'::jsonb,
    add column if not exists notes text;

create unique index if not exists uq_business_location_id_business
    on business_location (id, business_id);

alter table lead
    add column if not exists location_id uuid;

alter table lead
    add constraint fk_lead_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index if not exists idx_lead_business_location_stage
    on lead (business_id, location_id, stage);

update lead l
set location_id = c.location_id,
    updated_at = current_timestamp
from conversation c
where c.id = l.conversation_id
  and c.business_id = l.business_id
  and l.location_id is null
  and c.location_id is not null;

alter table order_request
    add column if not exists location_id uuid,
    add column if not exists fulfillment_type varchar(30) not null default 'BUSINESS_LOCATION';

alter table order_request
    add constraint fk_order_request_business_location
        foreign key (location_id) references business_location (id) on delete set null;

alter table order_request
    drop constraint if exists chk_order_request_fulfillment_type;

alter table order_request
    add constraint chk_order_request_fulfillment_type
        check (fulfillment_type in ('BUSINESS_LOCATION', 'PICKUP', 'DELIVERY', 'REMOTE'));

create index if not exists idx_order_request_business_location_status
    on order_request (business_id, location_id, status);

update order_request o
set location_id = coalesce(c.location_id, l.location_id),
    updated_at = current_timestamp
from lead l
left join conversation c on c.id = l.conversation_id and c.business_id = l.business_id
where o.lead_id = l.id
  and o.business_id = l.business_id
  and o.location_id is null
  and coalesce(c.location_id, l.location_id) is not null;

create table if not exists product_service_location (
    id uuid primary key,
    business_id uuid not null,
    product_service_id uuid not null,
    location_id uuid not null,
    active boolean not null default true,
    price_override numeric(12, 2),
    duration_override_minutes integer,
    stock_enabled boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_product_service_location_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_product_service_location_product_service
        foreign key (product_service_id) references product_service (id) on delete cascade,
    constraint fk_product_service_location_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_product_service_location
        unique (business_id, product_service_id, location_id),
    constraint chk_product_service_location_price_override
        check (price_override is null or price_override >= 0),
    constraint chk_product_service_location_duration_override
        check (duration_override_minutes is null or duration_override_minutes > 0)
);

create index if not exists idx_product_service_location_business_location_active
    on product_service_location (business_id, location_id, active);

create table if not exists product_location_stock (
    id uuid primary key,
    business_id uuid not null,
    product_service_id uuid not null,
    location_id uuid not null,
    stock_quantity integer not null default 0,
    stock_minimum integer not null default 0,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_product_location_stock_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_product_location_stock_product_service
        foreign key (product_service_id) references product_service (id) on delete cascade,
    constraint fk_product_location_stock_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_product_location_stock
        unique (business_id, product_service_id, location_id),
    constraint chk_product_location_stock_quantity
        check (stock_quantity >= 0),
    constraint chk_product_location_stock_minimum
        check (stock_minimum >= 0)
);

create index if not exists idx_product_location_stock_business_location_active
    on product_location_stock (business_id, location_id, active);

create table if not exists aesthetic_service_location (
    id uuid primary key,
    business_id uuid not null,
    service_id uuid not null,
    location_id uuid not null,
    active boolean not null default true,
    price_override numeric(12, 2),
    duration_override_minutes integer,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_service_location_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_aesthetic_service_location_service
        foreign key (service_id) references aesthetic_service (id) on delete cascade,
    constraint fk_aesthetic_service_location_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_aesthetic_service_location
        unique (business_id, service_id, location_id),
    constraint chk_aesthetic_service_location_price_override
        check (price_override is null or price_override >= 0),
    constraint chk_aesthetic_service_location_duration_override
        check (duration_override_minutes is null or duration_override_minutes > 0)
);

create index if not exists idx_aesthetic_service_location_business_location_active
    on aesthetic_service_location (business_id, location_id, active);

create table if not exists professional_location_schedule (
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
    constraint fk_professional_location_schedule_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_professional_location_schedule_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete cascade,
    constraint fk_professional_location_schedule_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint chk_professional_location_schedule_day
        check (day_of_week between 1 and 7),
    constraint chk_professional_location_schedule_time
        check (end_time > start_time),
    constraint uq_professional_location_schedule
        unique (business_id, professional_id, location_id, day_of_week, start_time, end_time)
);

create index if not exists idx_professional_location_schedule_professional_location
    on professional_location_schedule (business_id, location_id, professional_id, active);

create index if not exists idx_professional_location_schedule_day
    on professional_location_schedule (business_id, location_id, day_of_week, active);

alter table channel_account
    drop constraint if exists uq_channel_account_business_channel;

alter table channel_account
    add column if not exists location_id uuid,
    add column if not exists routing_mode varchar(30) not null default 'CENTRALIZED';

alter table channel_account
    add constraint fk_channel_account_business_location
        foreign key (location_id) references business_location (id) on delete set null;

alter table channel_account
    drop constraint if exists chk_channel_account_routing_mode;

alter table channel_account
    add constraint chk_channel_account_routing_mode
        check (routing_mode in ('CENTRALIZED', 'LOCATION_SPECIFIC'));

create unique index if not exists uq_channel_account_business_global_channel
    on channel_account (business_id, channel_type)
    where location_id is null;

create unique index if not exists uq_channel_account_business_location_channel
    on channel_account (business_id, location_id, channel_type)
    where location_id is not null;

create table if not exists user_location_access (
    id uuid primary key,
    business_id uuid not null,
    user_id uuid not null,
    location_id uuid not null,
    role_scope varchar(30) not null default 'OPERATOR',
    can_view_conversations boolean not null default true,
    can_manage_bookings boolean not null default true,
    can_manage_orders boolean not null default true,
    can_manage_catalog boolean not null default false,
    can_view_reports boolean not null default true,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_user_location_access_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_user_location_access_user
        foreign key (user_id) references user_account (id) on delete cascade,
    constraint fk_user_location_access_location
        foreign key (location_id) references business_location (id) on delete cascade,
    constraint uq_user_location_access
        unique (business_id, user_id, location_id),
    constraint chk_user_location_access_role_scope
        check (role_scope in ('OWNER', 'ADMIN', 'SUPERVISOR', 'OPERATOR', 'VIEWER'))
);

create index if not exists idx_user_location_access_user_active
    on user_location_access (business_id, user_id, active);

alter table automation_rule
    add column if not exists location_id uuid;

alter table automation_rule
    add constraint fk_automation_rule_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index if not exists idx_automation_rule_business_location_active
    on automation_rule (business_id, location_id, active);

alter table response_template
    add column if not exists location_id uuid;

alter table response_template
    add constraint fk_response_template_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index if not exists idx_response_template_business_location_active
    on response_template (business_id, location_id, active);

alter table aesthetic_treatment_history
    add column if not exists location_id uuid;

alter table aesthetic_treatment_history
    add constraint fk_aesthetic_treatment_history_location
        foreign key (location_id) references business_location (id) on delete set null;

create index if not exists idx_aesthetic_treatment_history_business_location
    on aesthetic_treatment_history (business_id, location_id, performed_at desc);

alter table notification
    add column if not exists location_id uuid;

alter table notification
    add constraint fk_notification_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index if not exists idx_notification_business_location_created
    on notification (business_id, location_id, created_at desc);

alter table audit_log
    add column if not exists location_id uuid;

alter table audit_log
    add constraint fk_audit_log_business_location
        foreign key (location_id) references business_location (id) on delete set null;

create index if not exists idx_audit_log_business_location_created
    on audit_log (business_id, location_id, created_at desc);

-- Sedes demo adicionales para Centro Estetico Bella.
insert into business_location (
    id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone, active, opening_hours, notes
)
values
    ('81000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'providencia', 'Providencia', 'Av. Providencia 2450, Santiago', 'Santiago', 'Providencia', '+56955550100', '+56955550100', 'America/Santiago', true, '{"lun-vie":"09:00-19:00","sab":"10:00-14:00"}'::jsonb, 'Sede central para demostracion multisede.'),
    ('81000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'maipu', 'Maipu', 'Av. Pajaritos 3211, Maipu', 'Santiago', 'Maipu', '+56955550200', '+56955550200', 'America/Santiago', true, '{"lun-vie":"10:00-20:00","sab":"10:00-15:00"}'::jsonb, 'Sede demo zona poniente.'),
    ('81000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'santiago-centro', 'Santiago Centro', 'Huerfanos 920, Santiago', 'Santiago', 'Santiago Centro', '+56955550300', '+56955550300', 'America/Santiago', true, '{"lun-vie":"09:30-18:30"}'::jsonb, 'Sede demo centro.')
on conflict (business_id, code) do update
set name = excluded.name,
    address = excluded.address,
    city = excluded.city,
    commune = excluded.commune,
    phone = excluded.phone,
    whatsapp_number = excluded.whatsapp_number,
    timezone = excluded.timezone,
    active = excluded.active,
    opening_hours = excluded.opening_hours,
    notes = excluded.notes,
    updated_at = current_timestamp;

-- Alias principal apunta a Providencia para compatibilidad de datos existentes.
update booking b
set location_id = bl.id,
    location = coalesce(b.location, bl.name),
    updated_at = current_timestamp
from business_location bl
where b.business_id = bl.business_id
  and bl.code = 'providencia'
  and b.business_id = '11111111-1111-1111-1111-111111111111'
  and b.location_id is null;

-- Asociar profesionales demo a sedes.
insert into aesthetic_professional_location (id, business_id, professional_id, location_id, active)
select gen_random_uuid(), ap.business_id, ap.id, bl.id, true
from aesthetic_professional ap
join business_location bl on bl.business_id = ap.business_id
where ap.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, professional_id, location_id) do nothing;

-- Horarios demo por profesional y sede.
insert into professional_location_schedule (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '09:00'::time, '18:00'::time, true
from aesthetic_professional_location apl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where apl.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do nothing;

-- Disponibilidad de servicios esteticos por sede.
insert into aesthetic_service_location (id, business_id, service_id, location_id, active, price_override, duration_override_minutes)
select gen_random_uuid(), s.business_id, s.id, bl.id, true, null, null
from aesthetic_service s
join business_location bl on bl.business_id = s.business_id
where s.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, service_id, location_id) do nothing;

-- Disponibilidad de productos/servicios del catalogo comercial por sede y stock inicial.
insert into product_service_location (id, business_id, product_service_id, location_id, active, price_override, duration_override_minutes, stock_enabled)
select gen_random_uuid(), ps.business_id, ps.id, bl.id, true, null, null, ps.type = 'PRODUCT'
from product_service ps
join business_location bl on bl.business_id = ps.business_id
where ps.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, product_service_id, location_id) do nothing;

insert into product_location_stock (id, business_id, product_service_id, location_id, stock_quantity, stock_minimum, active)
select gen_random_uuid(), ps.business_id, ps.id, bl.id,
       greatest(0, coalesce(ps.stock_quantity, 0) / 3),
       coalesce(ps.stock_minimum, 0),
       true
from product_service ps
join business_location bl on bl.business_id = ps.business_id
where ps.business_id = '11111111-1111-1111-1111-111111111111'
  and ps.type = 'PRODUCT'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, product_service_id, location_id) do nothing;

-- Permisos por sede para usuarios existentes.
insert into user_location_access (
    id, business_id, user_id, location_id, role_scope,
    can_view_conversations, can_manage_bookings, can_manage_orders, can_manage_catalog, can_view_reports, active
)
select gen_random_uuid(), ua.business_id, ua.id, bl.id,
       case when exists (
            select 1 from user_role ur join role r on r.id = ur.role_id where ur.user_id = ua.id and r.code in ('OWNER', 'ADMIN')
       ) then 'ADMIN' else 'OPERATOR' end,
       true, true, true,
       exists (select 1 from user_role ur join role r on r.id = ur.role_id where ur.user_id = ua.id and r.code in ('OWNER', 'ADMIN')),
       true,
       true
from user_account ua
join business_location bl on bl.business_id = ua.business_id
where ua.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, user_id, location_id) do nothing;

-- Propagar sede inicial a prospectos/pedidos demo si siguen vacios.
update lead l
set location_id = bl.id,
    updated_at = current_timestamp
from business_location bl
where l.business_id = bl.business_id
  and bl.code = 'providencia'
  and l.business_id = '11111111-1111-1111-1111-111111111111'
  and l.location_id is null;

update order_request o
set location_id = bl.id,
    updated_at = current_timestamp
from business_location bl
where o.business_id = bl.business_id
  and bl.code = 'providencia'
  and o.business_id = '11111111-1111-1111-1111-111111111111'
  and o.location_id is null;
