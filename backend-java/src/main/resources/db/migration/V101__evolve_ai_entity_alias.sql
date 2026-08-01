-- =============================================================================
-- FASE 3: Evolucion de ai_entity_alias (columnas semanticas)
-- Se conservan las columnas originales; se agregan columnas para vincular
-- cada alias a su entidad canonica y calificar el alias.
-- =============================================================================

alter table ai_entity_alias
    add column if not exists canonical_entity_id uuid;

alter table ai_entity_alias
    add column if not exists normalized_alias varchar(180);

alter table ai_entity_alias
    add column if not exists language varchar(10) not null default 'es';

alter table ai_entity_alias
    add column if not exists country_code varchar(10) not null default 'CL';

alter table ai_entity_alias
    add column if not exists alias_type varchar(50) not null default 'SYNONYM';

alter table ai_entity_alias
    add column if not exists confidence_base numeric(5,4) not null default 0.85;

alter table ai_entity_alias
    add column if not exists valid_from timestamptz;

alter table ai_entity_alias
    add column if not exists valid_until timestamptz;

alter table ai_entity_alias
    drop constraint if exists fk_ai_entity_alias_canonical_entity;

alter table ai_entity_alias
    add constraint fk_ai_entity_alias_canonical_entity
        foreign key (canonical_entity_id) references ai_canonical_entity (id) on delete set null;

alter table ai_entity_alias
    drop constraint if exists chk_ai_entity_alias_type;

alter table ai_entity_alias
    add constraint chk_ai_entity_alias_type check (
        alias_type in ('SYNONYM', 'ORTHOGRAPHIC_ERROR', 'REGIONALISM', 'PREFERRED', 'CONTEXTUAL')
    );

alter table ai_entity_alias
    drop constraint if exists chk_ai_entity_alias_confidence;

alter table ai_entity_alias
    add constraint chk_ai_entity_alias_confidence check (confidence_base between 0 and 1);

comment on column ai_entity_alias.canonical_entity_id is 'Entidad canonica del catalogo ai_canonical_entity a la que resuelve este alias';
comment on column ai_entity_alias.normalized_alias is 'Alias normalizado (minusculas, sin tildes) usado para el matching';
comment on column ai_entity_alias.alias_type is 'Tipo de alias: SYNONYM, ORTHOGRAPHIC_ERROR, REGIONALISM, PREFERRED, CONTEXTUAL';
comment on column ai_entity_alias.confidence_base is 'Confianza base aportada por el alias (0-1)';
comment on column ai_entity_alias.valid_from is 'Fecha desde la que el alias es valido (NULL = siempre)';
comment on column ai_entity_alias.valid_until is 'Fecha hasta la que el alias es valido (NULL = siempre)';

-- Backfill idempotente: normalizacion y vinculo con entidad canonica
update ai_entity_alias
set normalized_alias = lower(alias)
where normalized_alias is null;

update ai_entity_alias a
set canonical_entity_id = (
    select c.id
    from ai_canonical_entity c
    where c.business_id = a.business_id
      and c.canonical_name = lower(a.entity_value)
    order by c.entity_type
    limit 1
)
where a.canonical_entity_id is null;

create index if not exists idx_ai_entity_alias_canonical
    on ai_entity_alias (canonical_entity_id)
    where canonical_entity_id is not null;

create index if not exists idx_ai_entity_alias_lookup
    on ai_entity_alias (business_id, active, language, country_code, priority desc);
