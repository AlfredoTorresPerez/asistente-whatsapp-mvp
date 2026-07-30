-- =============================================================================
-- FASE 1: Configuracion funcional de IA por empresa
-- Crea business_ai_settings y migra prompts existentes a ai_prompt_template
-- =============================================================================

create table if not exists business_ai_settings (
    id uuid primary key default gen_random_uuid(),
    business_id uuid not null references business (id) on delete cascade,
    active boolean not null default false,
    mode varchar(20) not null default 'suggest',
    tone varchar(20) not null default 'Cercano',
    language varchar(10) not null default 'es',
    escalation_threshold numeric(3,2) not null default 0.30,
    allow_prices boolean not null default false,
    allow_booking boolean not null default true,
    allow_promotions boolean not null default false,
    require_availability_check boolean not null default true,
    allowed_topics jsonb not null default '[]'::jsonb,
    blocked_topics jsonb not null default '[]'::jsonb,
    active_prompt_version integer,
    updated_by uuid references user_account (id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint uq_business_ai_settings_business unique (business_id),
    constraint chk_business_ai_settings_mode check (mode in ('suggest', 'auto')),
    constraint chk_business_ai_settings_escalation check (escalation_threshold between 0.01 and 1.00)
);

comment on table business_ai_settings is 'Configuracion de IA del negocio por empresa';
comment on column business_ai_settings.active is 'Indica si el asistente IA esta activo para la empresa';
comment on column business_ai_settings.mode is 'Modo de operacion: suggest (sugerir) o auto (respuesta automatica)';
comment on column business_ai_settings.tone is 'Tono de las respuestas: Cercano, Profesional o Comercial';
comment on column business_ai_settings.escalation_threshold is 'Umbral de confianza para derivacion humana (0.01-1.00)';
comment on column business_ai_settings.allow_prices is 'Permite al asistente informar precios';
comment on column business_ai_settings.allow_booking is 'Permite al asistente gestionar reservas';
comment on column business_ai_settings.allow_promotions is 'Permite al asistente informar promociones';
comment on column business_ai_settings.require_availability_check is 'Exige validacion de disponibilidad antes de reservar';
comment on column business_ai_settings.allowed_topics is 'Temas permitidos para el asistente';
comment on column business_ai_settings.blocked_topics is 'Temas bloqueados para el asistente';
comment on column business_ai_settings.active_prompt_version is 'Version activa del prompt operativo';

-- Migrar PROMPT_OPERATIVO_IA_NEGOCIO desde aesthetic_business_rule a ai_prompt_template
insert into ai_prompt_template (business_id, codigo, nombre, descripcion, modulo, tipo, contenido, variables, prioridad, activo, version)
select
    abr.business_id,
    'PROMPT_OPERATIVO_IA_NEGOCIO',
    'Prompt operativo de IA del negocio',
    'Instruccion principal que define el comportamiento del asistente IA',
    'AI_AGENT',
    'SYSTEM_PROMPT',
    coalesce(abr.rule_payload ->> 'contenido', abr.description),
    coalesce((abr.rule_payload -> 'variables')::jsonb, '[]'::jsonb),
    1,
    true,
    1
from aesthetic_business_rule abr
where abr.code = 'PROMPT_OPERATIVO_IA_NEGOCIO'
  and abr.active = true
on conflict (business_id, codigo, version) do nothing;

-- Insertar configuración por defecto para empresas que tienen reglas IA
insert into business_ai_settings (business_id, active, mode, tone, language, escalation_threshold, allow_prices, allow_booking, allow_promotions, require_availability_check, active_prompt_version)
select distinct
    abr.business_id,
    true,
    'suggest',
    'Cercano',
    'es',
    0.30,
    false,
    true,
    false,
    true,
    (select max(pt.version) from ai_prompt_template pt where pt.business_id = abr.business_id and pt.codigo = 'PROMPT_OPERATIVO_IA_NEGOCIO')
from aesthetic_business_rule abr
where abr.business_id is not null
  and not exists (select 1 from business_ai_settings bas where bas.business_id = abr.business_id);
