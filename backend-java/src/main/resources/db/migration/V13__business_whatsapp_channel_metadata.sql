alter table channel_account
    add column if not exists adapter_mode varchar(60),
    add column if not exists linked_at timestamp with time zone,
    add column if not exists last_error text,
    add column if not exists business_phone_label varchar(120),
    add column if not exists require_dedicated_business_number boolean not null default true;

update channel_account
set adapter_mode = coalesce(adapter_mode, 'WHATSAPP_WEB_LOCAL'),
    linked_at = coalesce(linked_at, connected_at),
    business_phone_label = coalesce(business_phone_label, 'WhatsApp empresa'),
    require_dedicated_business_number = true
where channel_type = 'WHATSAPP';
