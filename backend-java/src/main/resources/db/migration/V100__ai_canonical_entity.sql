-- =============================================================================
-- FASE 3: Entidades canonicas (ai_canonical_entity)
-- Catalogo normalizado de entidades del negocio (servicios, profesionales,
-- salas, sedes, fechas relativas, preferencias) con referencia opcional a
-- tablas transaccionales reales.
-- =============================================================================

create table if not exists ai_canonical_entity (
    id uuid primary key default gen_random_uuid(),
    business_id uuid not null,
    entity_type varchar(50) not null,
    reference_type varchar(80),
    reference_id uuid,
    canonical_name varchar(180) not null,
    display_name varchar(180),
    language varchar(10) not null default 'es',
    country_code varchar(10) not null default 'CL',
    active boolean not null default true,
    priority integer not null default 100,
    version integer not null default 1,
    created_by uuid,
    updated_by uuid,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint fk_ai_canonical_entity_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_canonical_entity_created_by
        foreign key (created_by) references user_account (id) on delete set null,
    constraint fk_ai_canonical_entity_updated_by
        foreign key (updated_by) references user_account (id) on delete set null,
    constraint chk_ai_canonical_entity_type check (
        entity_type in ('SERVICE', 'PROFESSIONAL', 'ROOM', 'LOCATION', 'RELATIVE_DATE', 'PREFERENCE', 'OTHER')
    ),
    constraint chk_ai_canonical_entity_priority check (priority between 1 and 999),
    constraint chk_ai_canonical_entity_reference check (
        (reference_type is null and reference_id is null)
        or (reference_type is not null and reference_id is not null)
    )
);

comment on table ai_canonical_entity is 'Catalogo normalizado de entidades del negocio para la capa semantica de IA';
comment on column ai_canonical_entity.entity_type is 'Tipo de entidad: SERVICE, PROFESSIONAL, ROOM, LOCATION, RELATIVE_DATE, PREFERENCE, OTHER';
comment on column ai_canonical_entity.reference_type is 'Tabla de referencia (aesthetic_service, aesthetic_professional, agenda_room, business_location)';
comment on column ai_canonical_entity.reference_id is 'Id de la fila real en la tabla de referencia';
comment on column ai_canonical_entity.canonical_name is 'Nombre canonico normalizado (minusculas) usado para el matching';
comment on column ai_canonical_entity.display_name is 'Nombre legible para mostrar al cliente';
comment on column ai_canonical_entity.priority is 'Prioridad ante multiples entidades candidatas';

create unique index if not exists uq_ai_canonical_entity_scope
    on ai_canonical_entity (business_id, entity_type, canonical_name);

create index if not exists idx_ai_canonical_entity_reference
    on ai_canonical_entity (reference_type, reference_id)
    where reference_type is not null;

create index if not exists idx_ai_canonical_entity_lookup
    on ai_canonical_entity (business_id, active, entity_type, priority desc);

-- =============================================================================
-- Migracion progresiva desde ai_entity_alias (legado intacto).
-- Solo se canonicalizan los alias con entidad real (servicios, profesionales,
-- sedes) o de semantica conocida (fechas relativas, preferencias).
-- =============================================================================

insert into ai_canonical_entity
    (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority)
select a.business_id, 'SERVICE', 'aesthetic_service', s.id, lower(s.name), s.name, a.priority
from ai_entity_alias a
join aesthetic_service s
  on s.business_id = a.business_id
 and s.active = true
 and lower(s.name) = lower(a.entity_value)
where a.active = true
  and a.entity_key in ('servicio_o_producto', 'categoria_servicio')
on conflict do nothing;

insert into ai_canonical_entity
    (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority)
select a.business_id, 'PROFESSIONAL', 'aesthetic_professional', p.id, lower(p.full_name), p.full_name, a.priority
from ai_entity_alias a
join aesthetic_professional p
  on p.business_id = a.business_id
 and p.active = true
 and lower(p.full_name) = lower(a.entity_value)
where a.active = true
  and a.entity_key = 'profesional'
on conflict do nothing;

insert into ai_canonical_entity
    (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority)
select a.business_id, 'LOCATION', 'business_location', l.id, lower(l.name), l.name, a.priority
from ai_entity_alias a
join business_location l
  on l.business_id = a.business_id
 and l.active = true
 and lower(l.name) = lower(a.entity_value)
where a.active = true
  and a.entity_key = 'sede'
on conflict do nothing;

insert into ai_canonical_entity
    (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority)
select distinct a.business_id, 'RELATIVE_DATE', null::varchar(80), null::uuid, lower(a.entity_value),
       a.entity_value, a.priority
from ai_entity_alias a
where a.active = true
  and a.entity_key = 'fecha_relativa'
on conflict do nothing;

insert into ai_canonical_entity
    (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority)
select distinct a.business_id, 'PREFERENCE', null::varchar(80), null::uuid, lower(a.entity_value),
       a.entity_value, a.priority
from ai_entity_alias a
where a.active = true
  and a.entity_key = 'preferencia_horaria'
on conflict do nothing;
