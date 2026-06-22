create extension if not exists pgcrypto;

create table if not exists ai_conversation_context (
    id uuid primary key,
    business_id uuid not null,
    conversation_id uuid not null,
    customer_id uuid,
    active_agent varchar(40) not null,
    primary_intent varchar(80) not null,
    secondary_intent varchar(80),
    urgency varchar(20) not null,
    requires_human boolean not null default false,
    extracted_data jsonb not null default '{}'::jsonb,
    missing_data jsonb not null default '[]'::jsonb,
    summary text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_ai_conversation_context_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_conversation_context_conversation
        foreign key (conversation_id) references conversation (id) on delete cascade,
    constraint fk_ai_conversation_context_customer
        foreign key (customer_id) references customer (id) on delete set null,
    constraint uq_ai_conversation_context_business_conversation
        unique (business_id, conversation_id),
    constraint chk_ai_conversation_context_urgency
        check (urgency in ('bajo', 'medio', 'alto'))
);

create table if not exists ai_agent_decision_log (
    id uuid primary key,
    business_id uuid not null,
    conversation_id uuid not null,
    customer_id uuid,
    primary_intent varchar(80) not null,
    secondary_intent varchar(80),
    agent_type varchar(40) not null,
    confidence numeric(5,4) not null,
    urgency varchar(20) not null,
    requires_human boolean not null default false,
    handoff_reason varchar(255),
    extracted_data jsonb not null default '{}'::jsonb,
    missing_data jsonb not null default '[]'::jsonb,
    response_to_customer text,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_ai_agent_decision_log_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_agent_decision_log_conversation
        foreign key (conversation_id) references conversation (id) on delete cascade,
    constraint fk_ai_agent_decision_log_customer
        foreign key (customer_id) references customer (id) on delete set null,
    constraint chk_ai_agent_decision_log_urgency
        check (urgency in ('bajo', 'medio', 'alto')),
    constraint chk_ai_agent_decision_log_confidence
        check (confidence between 0 and 1)
);

create table if not exists human_handoff_request (
    id uuid primary key,
    business_id uuid not null,
    conversation_id uuid not null,
    customer_id uuid,
    urgency varchar(20) not null,
    reason varchar(255),
    summary text not null,
    status varchar(20) not null default 'OPEN',
    assigned_to_user_id uuid,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_human_handoff_request_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_human_handoff_request_conversation
        foreign key (conversation_id) references conversation (id) on delete cascade,
    constraint fk_human_handoff_request_customer
        foreign key (customer_id) references customer (id) on delete set null,
    constraint fk_human_handoff_request_assigned_user
        foreign key (assigned_to_user_id) references user_account (id) on delete set null,
    constraint chk_human_handoff_request_urgency
        check (urgency in ('bajo', 'medio', 'alto')),
    constraint chk_human_handoff_request_status
        check (status in ('OPEN', 'ASSIGNED', 'RESOLVED', 'CANCELLED'))
);

create table if not exists ai_agent_metric_daily (
    id uuid primary key,
    business_id uuid not null,
    metric_date date not null,
    agent_type varchar(40) not null,
    primary_intent varchar(80) not null,
    total_messages integer not null default 0,
    total_handoffs integer not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_ai_agent_metric_daily_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_ai_agent_metric_daily_scope
        unique (business_id, metric_date, agent_type, primary_intent),
    constraint chk_ai_agent_metric_daily_messages
        check (total_messages >= 0),
    constraint chk_ai_agent_metric_daily_handoffs
        check (total_handoffs >= 0)
);

create index if not exists idx_ai_conversation_context_conversation
    on ai_conversation_context (business_id, conversation_id);

create index if not exists idx_ai_agent_decision_log_conversation_created
    on ai_agent_decision_log (business_id, conversation_id, created_at desc);

create index if not exists idx_human_handoff_request_status
    on human_handoff_request (business_id, status, urgency, created_at desc);

create index if not exists idx_ai_agent_metric_daily_business_date
    on ai_agent_metric_daily (business_id, metric_date desc);
