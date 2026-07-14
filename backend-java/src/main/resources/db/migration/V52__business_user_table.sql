-- V52: Tabla business_user para asignar ubicacion por defecto a usuarios
-- Soporta la funcion findUserDefaultLocation() en CompleteAgendaJdbcRepository

create table business_user (
    id          uuid primary key,
    business_id uuid not null references business(id),
    user_id     uuid not null references user_account(id),
    location_id uuid not null references business_location(id),
    active      boolean not null default true,
    created_at  timestamp with time zone not null default now(),
    updated_at  timestamp with time zone not null default now()
);

create index idx_business_user_business on business_user(business_id);
create unique index idx_business_user_active on business_user(business_id, user_id) where active = true;

-- Seed para usuarios demo: asignar a la ubicacion principal de su empresa
do $$
declare
    v_business_id constant uuid := '11111111-1111-1111-1111-111111111111';
    v_location_id uuid;
begin
    select id into v_location_id from business_location
    where business_id = v_business_id and code = 'principal' and active = true
    limit 1;

    if v_location_id is not null then
        -- admin@demo.cl
        insert into business_user (id, business_id, user_id, location_id, active)
        select gen_random_uuid(), v_business_id, u.id, v_location_id, true
        from user_account u
        where u.business_id = v_business_id and u.email in ('admin@demo.cl', 'agente@demo.cl', 'supervisor@demo.cl', 'admin2@demo.cl')
        on conflict do nothing;
    end if;
end $$;
