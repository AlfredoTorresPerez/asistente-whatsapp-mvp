-- =============================================================================
-- FASE 1: Expresiones de intencion (ai_intent_expression)
-- Reemplaza semanticamente a intencion_expresion (que se conserva como legado).
-- Migracion progresiva: los datos existentes se copian sin eliminar el origen.
-- =============================================================================

create table if not exists ai_intent_expression (
    id uuid primary key default gen_random_uuid(),
    intent_id uuid not null,
    business_id uuid,
    expression_original varchar(500) not null,
    expression_normalized varchar(500) not null,
    expression_type varchar(50) not null default 'COMPLETE_PHRASE',
    priority integer not null default 100,
    confidence_base numeric(5,4) not null default 0.85,
    language varchar(10) not null default 'es',
    country_code varchar(10) not null default 'CL',
    active boolean not null default true,
    valid_from timestamptz,
    valid_until timestamptz,
    created_by uuid,
    updated_by uuid,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint fk_ai_intent_expression_intent
        foreign key (intent_id) references ai_intent (id) on delete cascade,
    constraint fk_ai_intent_expression_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_intent_expression_created_by
        foreign key (created_by) references user_account (id) on delete set null,
    constraint fk_ai_intent_expression_updated_by
        foreign key (updated_by) references user_account (id) on delete set null,
    constraint chk_ai_intent_expression_type check (
        expression_type in ('SYNONYM', 'COMPLETE_PHRASE', 'ORTHOGRAPHIC_ERROR', 'REGIONALISM', 'NEGATION', 'EXAMPLE', 'CONTEXTUAL_PATTERN')
    ),
    constraint chk_ai_intent_expression_confidence_base check (confidence_base between 0 and 1),
    constraint chk_ai_intent_expression_priority check (priority between 1 and 999)
);

comment on table ai_intent_expression is 'Expresiones naturales asociadas a intenciones del catalogo ai_intent';
comment on column ai_intent_expression.intent_id is 'Intencion canonica a la que pertenece la expresion';
comment on column ai_intent_expression.business_id is 'Negocio dueno de la expresion; NULL cuando es global';
comment on column ai_intent_expression.expression_original is 'Expresion tal como la escribio el cliente';
comment on column ai_intent_expression.expression_normalized is 'Expresion normalizada (minusculas, sin tildes) usada para el matching';
comment on column ai_intent_expression.expression_type is 'Tipo de expresion: SYNONYM, COMPLETE_PHRASE, ORTHOGRAPHIC_ERROR, REGIONALISM, NEGATION, EXAMPLE, CONTEXTUAL_PATTERN';
comment on column ai_intent_expression.priority is 'Prioridad de la expresion ante multiples coincidencias';
comment on column ai_intent_expression.confidence_base is 'Confianza base aportada por la expresion (0-1)';
comment on column ai_intent_expression.language is 'Idioma de la expresion (ISO 639-1, ej: es)';
comment on column ai_intent_expression.country_code is 'Pais de la expresion (ISO 3166-1, ej: CL)';
comment on column ai_intent_expression.valid_from is 'Fecha desde la que la expresion es valida (NULL = siempre)';
comment on column ai_intent_expression.valid_until is 'Fecha hasta la que la expresion es valida (NULL = siempre)';

-- Unica por (intent_id, business_id, expression_normalized)
create unique index if not exists uq_ai_intent_expression_global
    on ai_intent_expression (intent_id, expression_normalized)
    where business_id is null;

create unique index if not exists uq_ai_intent_expression_business
    on ai_intent_expression (intent_id, business_id, expression_normalized)
    where business_id is not null;

-- Indices de lookup
create index if not exists idx_ai_intent_expression_lookup
    on ai_intent_expression (business_id, language, country_code, active, priority desc);

create index if not exists idx_ai_intent_expression_normalized
    on ai_intent_expression (expression_normalized, active);

-- =============================================================================
-- Migracion progresiva desde intencion_expresion (tabla de legado, V48).
-- El origen se conserva intacto; solo se copian las expresiones de intenciones
-- mapeadas. Re-ejecutable gracias a los indices unicos parciales.
-- =============================================================================

insert into ai_intent_expression
    (intent_id, business_id, expression_original, expression_normalized, expression_type, priority, confidence_base, language, country_code, active)
select distinct
    ai_intent.id,
    ie.business_id,
    ie.expresion,
    lower(btrim(ie.expresion)),
    case
        when lower(btrim(ie.expresion)) in (
            'reserbar', 'recervar', 'resarvar', 'ajendar', 'agndar',
            'pedir ora', 'sacar hroa', 'reserva r', 'reservaar',
            'repogramar', 'reagendar hora', 'cancelar mi cita por favorr'
        ) then 'ORTHOGRAPHIC_ERROR'
        else 'COMPLETE_PHRASE'
    end,
    100,
    0.85,
    'es',
    'CL',
    true
from intencion_expresion ie
join ai_intent on ai_intent.code = case ie.intencion_canonica
    when 'reservar'             then 'BOOKING_CREATE'
    when 'reprogramar_reserva'  then 'BOOKING_RESCHEDULE'
    when 'cancelar_reserva'     then 'BOOKING_CANCEL'
    when 'consultar_reservas'   then 'BOOKING_STATUS'
    else null
end
where ai_intent.id is not null
  and lower(btrim(ie.expresion)) is not null
on conflict do nothing;

-- =============================================================================
-- Seeds de expresiones chilenas para el nuevo catalogo (intenciones globales).
-- =============================================================================

insert into ai_intent_expression
    (intent_id, business_id, expression_original, expression_normalized, expression_type, priority, confidence_base, language, country_code, active)
select id, null, expr.expression_original, lower(expr.expression_original), expr.expression_type, expr.priority, expr.confidence_base, 'es', 'CL', true
from (values
    -- BOOKING_CREATE: pedir/agendar una hora
    ('BOOKING_CREATE', 'quiero agendar una hora',        'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_CREATE', 'quiero agendar',                 'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_CREATE', 'quiero reservar',                'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_CREATE', 'quiero pedir una hora',          'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_CREATE', 'necesito tomar una hora',        'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_CREATE', 'me gustaria tomar una hora',     'COMPLETE_PHRASE',  99, 0.85),
    ('BOOKING_CREATE', 'quiero una cita',                'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_CREATE', 'puedo reservar',                 'COMPLETE_PHRASE', 100, 0.85),
    ('BOOKING_CREATE', 'tendran hora',                   'REGIONALISM',     100, 0.85),
    ('BOOKING_CREATE', 'hay cupo',                       'REGIONALISM',     100, 0.85),
    ('BOOKING_CREATE', 'ajendar una hora',               'ORTHOGRAPHIC_ERROR', 100, 0.85),
    ('BOOKING_CREATE', 'quiero que me agenden',          'COMPLETE_PHRASE',  99, 0.85),
    -- BOOKING_RESCHEDULE: mover/cambiar la hora
    ('BOOKING_RESCHEDULE', 'quiero cambiar mi hora',     'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_RESCHEDULE', 'necesito mover la cita',     'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_RESCHEDULE', 'quiero reagendar',           'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_RESCHEDULE', 'quiero reprogramar mi cita', 'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_RESCHEDULE', 'puedo cambiar la fecha',     'COMPLETE_PHRASE', 100, 0.85),
    ('BOOKING_RESCHEDULE', 'muevela para manana',        'COMPLETE_PHRASE',  99, 0.85),
    ('BOOKING_RESCHEDULE', 'quiero mover la hora',       'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_RESCHEDULE', 'cambiar la hora de mi cita', 'COMPLETE_PHRASE', 100, 0.85),
    -- BOOKING_CANCEL: anular/suspender la hora
    ('BOOKING_CANCEL', 'quiero cancelar',                'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_CANCEL', 'quiero cancelar mi reserva',     'COMPLETE_PHRASE', 100, 0.90),
    ('BOOKING_CANCEL', 'quiero anular la hora',          'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_CANCEL', 'quiero anular mi cita',          'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_CANCEL', 'necesito suspender la cita',     'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_CANCEL', 'no podre asistir',               'COMPLETE_PHRASE',  99, 0.85),
    ('BOOKING_CANCEL', 'elimina mi reserva',             'COMPLETE_PHRASE', 100, 0.88),
    ('BOOKING_CANCEL', 'dar de baja la hora',            'REGIONALISM',     100, 0.85)
) as expr(code, expression_original, expression_type, priority, confidence_base)
join ai_intent on ai_intent.code = expr.code and ai_intent.business_id is null
on conflict do nothing;
