create table if not exists ai_reply_outbox (
    id uuid primary key,
    business_id uuid not null,
    channel_account_id uuid not null,
    conversation_id uuid not null,
    customer_id uuid not null,
    inbound_message_id uuid not null,
    recipient_phone varchar(64) not null,
    customer_display_name varchar(255),
    message_body text not null,
    location_id uuid,
    location_name varchar(255),
    trace_id varchar(80) not null,
    status varchar(30) not null default 'PENDING',
    attempts integer not null default 0,
    max_attempts integer not null default 5,
    next_attempt_at timestamptz not null default current_timestamp,
    locked_at timestamptz,
    processed_at timestamptz,
    last_error_code varchar(160),
    last_error_message text,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint fk_ai_reply_outbox_business foreign key (business_id) references business(id),
    constraint fk_ai_reply_outbox_channel_account foreign key (channel_account_id) references channel_account(id),
    constraint fk_ai_reply_outbox_conversation foreign key (conversation_id) references conversation(id),
    constraint fk_ai_reply_outbox_customer foreign key (customer_id) references customer(id),
    constraint fk_ai_reply_outbox_inbound_message foreign key (inbound_message_id) references message(id),
    constraint uq_ai_reply_outbox_inbound_message unique (business_id, inbound_message_id),
    constraint chk_ai_reply_outbox_status check (status in ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'SKIPPED')),
    constraint chk_ai_reply_outbox_attempts check (attempts >= 0 and max_attempts > 0)
);

create index if not exists idx_ai_reply_outbox_pending
    on ai_reply_outbox (status, next_attempt_at, created_at);

create index if not exists idx_ai_reply_outbox_business_conversation
    on ai_reply_outbox (business_id, conversation_id, created_at desc);
