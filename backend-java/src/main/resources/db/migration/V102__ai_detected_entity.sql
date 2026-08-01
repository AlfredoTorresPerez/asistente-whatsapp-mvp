-- =============================================================================
-- FASE 3: Entidades detectadas por analisis (ai_detected_entity)
-- Registro de entidades resueltas en cada analisis de mensaje, con su
-- metodo de resolucion (catalogo, patron, alias) y referencia canónica.
-- =============================================================================

create table if not exists ai_detected_entity (
    id uuid primary key default gen_random_uuid(),
    message_analysis_id uuid not null,
    business_id uuid not null,
    canonical_entity_id uuid,
    entity_type varchar(50) not null,
    entity_key varchar(80) not null,
    entity_value varchar(255) not null,
    resolution_method varchar(30) not null,
    matched_alias text,
    confidence numeric(5,4),
    reference_type varchar(80),
    reference_id uuid,
    created_at timestamptz not null default current_timestamp,
    constraint fk_ai_detected_entity_analysis
        foreign key (message_analysis_id) references ai_message_analysis (id) on delete cascade,
    constraint fk_ai_detected_entity_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_detected_entity_canonical
        foreign key (canonical_entity_id) references ai_canonical_entity (id) on delete set null,
    constraint chk_ai_detected_entity_type check (
        entity_type in ('SERVICE', 'PROFESSIONAL', 'ROOM', 'LOCATION', 'RELATIVE_DATE', 'PREFERENCE',
            'TIME', 'PERSON', 'CONTACT', 'OTHER')
    ),
    constraint chk_ai_detected_entity_method check (
        resolution_method in ('DATABASE', 'PATTERN', 'ALIAS', 'LLM', 'HUMAN')
    ),
    constraint chk_ai_detected_entity_confidence check (confidence between 0 and 1)
);

comment on table ai_detected_entity is 'Entidades detectadas y resueltas en cada analisis de mensaje de IA';
comment on column ai_detected_entity.message_analysis_id is 'Analisis al que pertenece la entidad';
comment on column ai_detected_entity.canonical_entity_id is 'Entidad canonica resuelta (si aplica)';
comment on column ai_detected_entity.entity_key is 'Clave del contexto de entidades (servicio_o_producto, sede, fecha, hora...)';
comment on column ai_detected_entity.entity_value is 'Valor detectado tal como se resolvio';
comment on column ai_detected_entity.resolution_method is 'Metodo: DATABASE (catalogo), PATTERN (regex), ALIAS, LLM o HUMAN';
comment on column ai_detected_entity.matched_alias is 'Alias o texto que disparo la deteccion (si aplica)';
comment on column ai_detected_entity.reference_type is 'Tabla de referencia resuelta (aesthetic_service, business_location...)';
comment on column ai_detected_entity.reference_id is 'Id de la fila real de referencia';

create index if not exists idx_ai_detected_entity_analysis
    on ai_detected_entity (message_analysis_id, entity_key);

create index if not exists idx_ai_detected_entity_lookup
    on ai_detected_entity (business_id, entity_type, created_at desc);

create index if not exists idx_ai_detected_entity_canonical
    on ai_detected_entity (canonical_entity_id)
    where canonical_entity_id is not null;
