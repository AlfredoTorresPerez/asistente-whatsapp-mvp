-- =============================================================================
-- V80: POLITICAS VERSIONADAS POR NEGOCIO Y SUCURSAL
--
-- Crea el sistema de politicas versionadas para cancelacion, reprogramacion,
-- anticipacion maxima, tolerancia y penalizacion, permitiendo congelar la
-- politica vigente al momento de la reserva (policy_version_id + snapshot).
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. business_policy_version: versiones de politica por negocio
-- ---------------------------------------------------------------------------
create table if not exists business_policy_version (
    id              uuid primary key,
    business_id     uuid not null,
    version         integer not null,
    description     varchar(500),
    effective_from  timestamp with time zone not null,
    effective_until timestamp with time zone,
    created_at      timestamp with time zone not null default current_timestamp,
    created_by      uuid,

    constraint fk_bpv_business foreign key (business_id) references business(id) on delete cascade,
    constraint uq_bpv_business_version unique (business_id, version),
    constraint chk_bpv_version_positive check (version > 0),
    constraint chk_bpv_effective_range check (effective_until is null or effective_until > effective_from)
);

create index idx_bpv_business_effective
    on business_policy_version (business_id, effective_from desc, effective_until);

comment on table business_policy_version is
    'Versiones de politica operativa por negocio. Cada version agrupa un conjunto
     de reglas (cancellation, reschedule, max_advance, tolerance, penalty) que
     entran en vigencia en una fecha determinada.';

-- ---------------------------------------------------------------------------
-- 2. business_policy: reglas puntuales dentro de una version de politica
-- ---------------------------------------------------------------------------
create table if not exists business_policy (
    id              uuid primary key,
    version_id      uuid not null,
    location_id     uuid,  -- null = aplica a todo el negocio
    policy_type     varchar(40) not null,
    policy_key      varchar(80) not null,
    policy_value    jsonb not null,
    priority        integer not null default 0,
    active          boolean not null default true,
    created_at      timestamp with time zone not null default current_timestamp,
    updated_at      timestamp with time zone not null default current_timestamp,

    constraint fk_bp_version foreign key (version_id) references business_policy_version(id) on delete cascade,
    constraint fk_bp_location foreign key (location_id) references business_location(id) on delete set null,
    constraint chk_bp_policy_type check (policy_type in (
        'CANCELLATION', 'RESCHEDULE', 'MAX_ADVANCE', 'MIN_ADVANCE',
        'TOLERANCE', 'PENALTY', 'CONFIRMATION', 'DEPOSIT'
    )),
    constraint uq_bp_version_type_key unique (version_id, location_id, policy_type, policy_key)
);

create index idx_bp_version on business_policy (version_id);
create index idx_bp_location on business_policy (location_id) where location_id is not null;

comment on table business_policy is
    'Reglas individuales dentro de una version de politica.
     policy_type: CANCELLATION, RESCHEDULE, MAX_ADVANCE, MIN_ADVANCE,
                  TOLERANCE, PENALTY, CONFIRMATION, DEPOSIT.
     policy_value: JSONB con los parametros especificos de cada regla.
     location_id null = aplica a todo el negocio; si tiene valor = override por sucursal.';

-- ---------------------------------------------------------------------------
-- 3. Columnas de congelamiento en booking
-- ---------------------------------------------------------------------------
alter table booking
    add column if not exists policy_version_id uuid,
    add column if not exists policy_snapshot jsonb;

alter table booking
    add constraint fk_booking_policy_version
        foreign key (policy_version_id) references business_policy_version(id) on delete set null;

comment on column booking.policy_version_id is
    'Version de politica congelada al momento de crear/confirmar la reserva.';
comment on column booking.policy_snapshot is
    'Snapshot JSONB de las reglas aplicables al momento de la reserva
     (cancellation_window_hours, reschedule_window_hours, max_advance_days,
      min_advance_minutes, tolerance_minutes, penalty_percent, penalty_amount,
      reschedule_max_count, grace_period_minutes).';

-- ---------------------------------------------------------------------------
-- 4. Seed data para negocio demo: version 1 vigente
-- ---------------------------------------------------------------------------
insert into business_policy_version (id, business_id, version, description, effective_from, created_by)
select
    'a1000000-0000-0000-0000-000000000001'::uuid,
    id,
    1,
    'Politica inicial por defecto',
    '2026-01-01 00:00:00-03'::timestamptz,
    null
from business
where code = 'CENTRO-ESTETICO-DEMO'
  and not exists (
      select 1 from business_policy_version bpv
      where bpv.business_id = business.id and bpv.version = 1
  );

-- Reglas default de la version 1
do $$
declare
    v_id uuid;
begin
    select id into v_id from business_policy_version
    where business_id = (select id from business where code = 'CENTRO-ESTETICO-DEMO')
      and version = 1
    limit 1;

    if v_id is not null then
        -- Cancelacion: 24h de anticipacion, sin penalidad
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'CANCELLATION', 'default',
                '{"window_hours": 24, "require_reason": true, "allow_online": true, "notify_professional": true}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;

        -- Reprogramacion: 12h, max 3 veces
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'RESCHEDULE', 'default',
                '{"window_hours": 12, "max_count": 3, "require_reason": true, "allow_online": true}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;

        -- Anticipacion maxima: 60 dias
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'MAX_ADVANCE', 'default',
                '{"max_days": 60}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;

        -- Anticipacion minima: 60 min (default, se override por agenda_business_hours)
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'MIN_ADVANCE', 'default',
                '{"min_minutes": 60}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;

        -- Tolerancia: 15 min de espera
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'TOLERANCE', 'default',
                '{"grace_period_minutes": 15, "auto_expire_minutes": 30}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;

        -- Penalidad: sin penalidad por defecto
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'PENALTY', 'default',
                '{"type": "none", "percent": 0, "fixed_amount": 0, "currency": "CLP"}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;
    end if;
end $$;
