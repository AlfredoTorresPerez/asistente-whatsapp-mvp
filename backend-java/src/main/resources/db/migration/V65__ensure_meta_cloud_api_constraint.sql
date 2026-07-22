do $$
declare
    cloud_channel_id uuid;
    demo_business_id uuid := '11111111-1111-1111-1111-111111111111';
begin
    -- Asegurar que la constraint incluya META_CLOUD_API
    alter table channel_account
        drop constraint if exists chk_channel_account_provider_name;

    alter table channel_account
        add constraint chk_channel_account_provider_name
            check (provider_name in ('WHATSAPP_WEB', 'CLOUD_API', 'META_CLOUD_API'));

    -- Asegurar que exista el canal META_CLOUD_API centralizado para el negocio demo
    if not exists (
        select 1 from channel_account
        where business_id = demo_business_id
          and provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
          and routing_mode = 'CENTRALIZED'
          and active = true
    ) then
        insert into channel_account (
            id, business_id, provider_name, channel_type, session_key, status,
            phone_number, display_phone_number, normalized_phone_number, active,
            registration_status, operational_status, webhook_status, credential_status,
            routing_mode, created_at, updated_at
        ) values (
            gen_random_uuid(), demo_business_id, 'META_CLOUD_API', 'WHATSAPP',
            'META_CLOUD_API_DEMO_' || gen_random_uuid()::text, 'DISCONNECTED',
            '56927305158', '+56 9 2730 5158', '56927305158', true,
            'COMPLETED', 'ACTIVE', 'DISABLED', 'CONFIGURED',
            'CENTRALIZED', current_timestamp, current_timestamp
        );
    end if;
end $$;
