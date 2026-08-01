-- =============================================================================
-- FASE 2: Analisis canonico por mensaje (ai_message_analysis)
-- Registro 1:1 por turno procesado por el motor de IA. No reemplaza
-- aesthetic_intent_log (log LLM paralelo).
-- =============================================================================

create table if not exists ai_message_analysis (
    id uuid primary key default gen_random_uuid(),
    business_id uuid not null,
    conversation_id uuid not null,
    customer_id uuid,
    channel_account_id uuid,
    message_id uuid,
    detector_type varchar(30) not null,
    language varchar(10) not null default 'es',
    country_code varchar(10) not null default 'CL',
    message_normalized text,
    message_tokens integer not null default 0,
    ambiguity_score numeric(5,4),
    payload jsonb,
    created_by uuid,
    created_at timestamptz not null default current_timestamp,
    constraint fk_ai_message_analysis_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_message_analysis_conversation
        foreign key (conversation_id) references conversation (id) on delete cascade,
    constraint fk_ai_message_analysis_customer
        foreign key (customer_id) references customer (id) on delete set null,
    constraint fk_ai_message_analysis_created_by
        foreign key (created_by) references user_account (id) on delete set null,
    constraint chk_ai_message_analysis_detector_type check (
        detector_type in ('DATABASE', 'JAVA_FALLBACK', 'AI_MODEL', 'HUMAN_VALIDATION')
    ),
    constraint chk_ai_message_analysis_ambiguity check (ambiguity_score between 0 and 1),
    constraint chk_ai_message_analysis_tokens check (message_tokens >= 0)
);

comment on table ai_message_analysis is 'Analisis canonico del motor de IA por mensaje entrante procesado';
comment on column ai_message_analysis.business_id is 'Negocio propietario del analisis';
comment on column ai_message_analysis.conversation_id is 'Conversacion a la que pertenece el mensaje';
comment on column ai_message_analysis.message_id is 'Mensaje persistido relacionado (nullable: el analisis es por turno de IA, no por fila de mensaje)';
comment on column ai_message_analysis.detector_type is 'Fuente de la deteccion: DATABASE, JAVA_FALLBACK, AI_MODEL o HUMAN_VALIDATION';
comment on column ai_message_analysis.language is 'Idioma detectado (ISO 639-1, ej: es)';
comment on column ai_message_analysis.country_code is 'Pais (ISO 3166-1, ej: CL)';
comment on column ai_message_analysis.message_normalized is 'Mensaje normalizado analizado por el detector';
comment on column ai_message_analysis.message_tokens is 'Cantidad de tokens aproximada (palabras) del mensaje normalizado';
comment on column ai_message_analysis.ambiguity_score is 'Nivel de ambiguedad del mensaje (1 - confianza de la intencion primaria)';
comment on column ai_message_analysis.payload is 'Detalle del analisis (entidades extraidas, traza, fuente)';

create index if not exists idx_ai_message_analysis_lookup
    on ai_message_analysis (business_id, created_at desc);

create index if not exists idx_ai_message_analysis_conversation
    on ai_message_analysis (conversation_id, created_at desc);

create index if not exists idx_ai_message_analysis_detector
    on ai_message_analysis (detector_type, created_at desc);
