-- =============================================================================
-- FASE 2: Intenciones detectadas por analisis (ai_detected_intent)
-- Ranking de intenciones candidatas de cada ai_message_analysis.
-- =============================================================================

create table if not exists ai_detected_intent (
    id uuid primary key default gen_random_uuid(),
    message_analysis_id uuid not null,
    business_id uuid not null,
    intent_id uuid not null,
    rank integer not null,
    is_primary boolean not null default false,
    confidence numeric(5,4) not null,
    method_source varchar(30) not null,
    matched_expression text,
    created_at timestamptz not null default current_timestamp,
    constraint fk_ai_detected_intent_analysis
        foreign key (message_analysis_id) references ai_message_analysis (id) on delete cascade,
    constraint fk_ai_detected_intent_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_detected_intent_intent
        foreign key (intent_id) references ai_intent (id) on delete restrict,
    constraint chk_ai_detected_intent_rank check (rank >= 1),
    constraint chk_ai_detected_intent_confidence check (confidence between 0 and 1),
    constraint chk_ai_detected_intent_method check (
        method_source in ('DATABASE', 'JAVA_FALLBACK', 'AI_MODEL', 'HUMAN_VALIDATION')
    )
);

comment on table ai_detected_intent is 'Intenciones detectadas (ranking) para cada analisis de mensaje';
comment on column ai_detected_intent.message_analysis_id is 'Analisis al que pertenece la deteccion';
comment on column ai_detected_intent.intent_id is 'Intencion canonica del catalogo ai_intent';
comment on column ai_detected_intent.rank is 'Posicion en el ranking de candidatos (1 = primaria)';
comment on column ai_detected_intent.is_primary is 'Indica si es la intencion primaria ejecutada';
comment on column ai_detected_intent.confidence is 'Confianza de la deteccion (0-1)';
comment on column ai_detected_intent.method_source is 'Fuente de la deteccion: DATABASE, JAVA_FALLBACK, AI_MODEL o HUMAN_VALIDATION';
comment on column ai_detected_intent.matched_expression is 'Expresion del catalogo que coincidio (si aplica)';

-- Un (rank) por analisis
create unique index if not exists uq_ai_detected_intent_analysis_rank
    on ai_detected_intent (message_analysis_id, rank);

create index if not exists idx_ai_detected_intent_lookup
    on ai_detected_intent (business_id, intent_id, created_at desc);

create index if not exists idx_ai_detected_intent_primary
    on ai_detected_intent (message_analysis_id, is_primary) where is_primary = true;
