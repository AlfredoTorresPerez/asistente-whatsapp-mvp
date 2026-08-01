-- =============================================================================
-- FASE 1: Catalogo normalizado de intenciones (ai_intent)
-- Capa semantica de IA conversacional. No reemplaza la arquitectura de agenda.
-- =============================================================================

create table if not exists ai_intent (
    id uuid primary key default gen_random_uuid(),
    business_id uuid,
    code varchar(100) not null,
    name varchar(150) not null,
    description text,
    domain varchar(100),
    requires_confirmation boolean not null default false,
    requires_human boolean not null default false,
    minimum_confidence numeric(5,4) not null default 0.60,
    priority integer not null default 100,
    active boolean not null default true,
    version integer not null default 1,
    created_by uuid,
    updated_by uuid,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint fk_ai_intent_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_intent_created_by
        foreign key (created_by) references user_account (id) on delete set null,
    constraint fk_ai_intent_updated_by
        foreign key (updated_by) references user_account (id) on delete set null,
    constraint chk_ai_intent_code_uppercase check (code = upper(code)),
    constraint chk_ai_intent_minimum_confidence check (minimum_confidence between 0 and 1),
    constraint chk_ai_intent_priority check (priority between 1 and 999)
);

comment on table ai_intent is 'Catalogo normalizado de intenciones conversacionales de la IA';
comment on column ai_intent.business_id is 'Negocio dueno de la intencion; NULL cuando es una intencion global';
comment on column ai_intent.code is 'Codigo canonico de la intencion en mayusculas (ej: BOOKING_CREATE)';
comment on column ai_intent.name is 'Nombre legible de la intencion';
comment on column ai_intent.domain is 'Dominio funcional (BOOKING, CATALOG, BUSINESS, PAYMENT, SOCIAL, SUPPORT, KNOWLEDGE, FOLLOW_UP, FALLBACK)';
comment on column ai_intent.requires_confirmation is 'Indica si la intencion exige confirmacion antes de ejecutar una operacion sensible';
comment on column ai_intent.requires_human is 'Indica si la intencion debe derivarse a un humano';
comment on column ai_intent.minimum_confidence is 'Confianza minima para aceptar la intencion (0-1)';
comment on column ai_intent.priority is 'Prioridad de la intencion ante conflictos (1-999)';
comment on column ai_intent.version is 'Version del catalogo de la intencion';

-- Unica por business_id y code (business_id NULL = global)
create unique index if not exists uq_ai_intent_global_code
    on ai_intent (code)
    where business_id is null;

create unique index if not exists uq_ai_intent_business_code
    on ai_intent (business_id, code)
    where business_id is not null;

create index if not exists idx_ai_intent_lookup
    on ai_intent (business_id, active, domain, priority desc);

-- Intenciones globales iniciales (16 requeridas + extras para cobertura 1:1 con AgentIntent)
insert into ai_intent (code, name, description, domain, requires_confirmation, requires_human, minimum_confidence, priority)
values
    ('BOOKING_CREATE',       'Crear reserva',          'El cliente quiere agendar, reservar o tomar una hora', 'BOOKING',  false, false, 0.55, 100),
    ('BOOKING_RESCHEDULE',   'Reprogramar reserva',    'El cliente quiere cambiar fecha u hora de su reserva', 'BOOKING', true,  false, 0.60, 100),
    ('BOOKING_CANCEL',       'Cancelar reserva',       'El cliente quiere cancelar o anular su reserva', 'BOOKING', true,  false, 0.60, 100),
    ('BOOKING_AVAILABILITY', 'Consultar disponibilidad', 'El cliente pregunta por horarios o cupos disponibles', 'BOOKING', false, false, 0.55, 100),
    ('BOOKING_STATUS',       'Estado de reserva',      'El cliente consulta el estado de su reserva', 'BOOKING', false, false, 0.60, 100),
    ('SERVICE_INFORMATION',  'Informacion de servicio', 'El cliente pregunta que incluye, duracion o cuidados de un servicio', 'CATALOG', false, false, 0.50, 100),
    ('SERVICE_PRICE',        'Precio de servicio',     'El cliente pregunta cuanto cuesta un servicio', 'CATALOG', false, false, 0.55, 100),
    ('BUSINESS_HOURS',       'Horario de atencion',    'El cliente pregunta horarios de apertura o atencion', 'BUSINESS', false, false, 0.50, 100),
    ('BUSINESS_LOCATION',    'Ubicacion del negocio',  'El cliente pregunta donde queda o como llegar', 'BUSINESS', false, false, 0.50, 100),
    ('PAYMENT_INFORMATION',  'Informacion de pago',    'El cliente pregunta formas de pago, señal o enlace de pago', 'PAYMENT', false, false, 0.55, 100),
    ('PAYMENT_STATUS',       'Estado de pago',         'El cliente consulta si su pago llego o su saldo', 'PAYMENT', false, false, 0.60, 100),
    ('GREETING',             'Saludo',                 'El cliente saluda', 'SOCIAL', false, false, 0.40, 100),
    ('THANKS',               'Agradecimiento',         'El cliente agradece', 'SOCIAL', false, false, 0.40, 100),
    ('GOODBYE',              'Despedida',              'El cliente se despide', 'SOCIAL', false, false, 0.40, 100),
    ('HUMAN_REQUEST',        'Solicitar humano',       'El cliente pide hablar con una persona', 'SUPPORT', false, true, 0.60, 100),
    ('UNKNOWN',              'Intencion desconocida',  'No se pudo determinar la intencion del mensaje', 'FALLBACK', false, false, 0.00, 100),
    -- Extras: cobertura 1:1 con el enum AgentIntent existente (trazabilidad)
    ('COMMERCIAL_INQUIRY',       'Consulta comercial',          'El cliente pregunta por productos, planes o promociones', 'SALES', false, false, 0.50, 100),
    ('SERVICE_RECOMMENDATION',   'Recomendacion de servicio',   'El cliente pide recomendacion de tratamiento', 'CATALOG', false, false, 0.50, 100),
    ('PROFESSIONAL_QUERY',       'Consulta profesional',        'El cliente pregunta por profesionales o quien atiende', 'CATALOG', false, false, 0.50, 100),
    ('QUOTE_REQUEST',            'Solicitud de cotizacion',     'El cliente pide cotizacion o presupuesto', 'SALES', false, false, 0.55, 100),
    ('PAYMENT_PROBLEM',          'Problema de pago',            'El cliente reporta un problema con un pago', 'PAYMENT', false, false, 0.60, 100),
    ('SUPPORT_GENERAL',          'Soporte general',             'El cliente pide ayuda general', 'SUPPORT', false, false, 0.50, 100),
    ('TECHNICAL_MESSAGE',        'Mensaje tecnico',             'Mensaje con contenido tecnico o de sistema', 'SUPPORT', false, false, 0.60, 100),
    ('KNOWLEDGE_QUERY',          'Consulta de conocimiento',    'El cliente pregunta politicas, penalizaciones o FAQ', 'KNOWLEDGE', false, false, 0.50, 100),
    ('FOLLOW_UP',                'Seguimiento',                 'El cliente retoma una cotizacion o seguimiento previo', 'FOLLOW_UP', false, false, 0.50, 100),
    ('COMPLAINT',                'Reclamo',                     'El cliente presenta un reclamo', 'SUPPORT', false, true, 0.55, 100),
    ('WAITLIST_QUERY',           'Lista de espera',             'El cliente consulta o acepta lista de espera', 'BOOKING', false, false, 0.55, 100)
on conflict (code) where business_id is null do nothing;
