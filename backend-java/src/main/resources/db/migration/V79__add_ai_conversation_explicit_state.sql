alter table ai_conversation_context
    add column if not exists conversation_state varchar(80) not null default 'INICIO';

alter table ai_conversation_context
    add column if not exists state_payload jsonb not null default '{}'::jsonb;

alter table ai_conversation_context
    add column if not exists active_options jsonb not null default '[]'::jsonb;

alter table ai_conversation_context
    add column if not exists last_transition_at timestamp with time zone not null default current_timestamp;

create index if not exists idx_ai_conversation_context_state
    on ai_conversation_context (business_id, conversation_state, updated_at desc);
