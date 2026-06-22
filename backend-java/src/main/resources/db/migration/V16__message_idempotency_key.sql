alter table message
    add column if not exists idempotency_key varchar(120);

create unique index if not exists ux_message_outbound_idempotency
    on message (business_id, conversation_id, idempotency_key)
    where direction = 'OUTBOUND'
      and idempotency_key is not null;
