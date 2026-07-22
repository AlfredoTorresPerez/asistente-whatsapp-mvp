do $$
declare
    demo_business_id uuid := '11111111-1111-1111-1111-111111111111';
    cloud_channel_id uuid;
    meta_cloud_exists boolean;
    web_channel_id uuid;
begin
    -- 1. Insertar canal META_CLOUD_API con +56 9 2730 5158 si no existe
    select exists(
        select 1 from channel_account
        where business_id = demo_business_id
          and provider_name in ('CLOUD_API', 'META_CLOUD_API')
    ) into meta_cloud_exists;

    if not meta_cloud_exists then
        -- Usamos CLOUD_API porque V62 renombra CLOUD_API -> META_CLOUD_API
        -- location_id apunta a la sede principal (V17) para evitar colision
        -- con uq_channel_account_business_global_channel (V18) que exige
        -- (business_id, channel_type) unicos donde location_id IS NULL
        insert into channel_account (
            id, business_id, provider_name, channel_type, session_key, status,
            phone_number, display_phone_number, normalized_phone_number, active,
            registration_status, operational_status, webhook_status, credential_status,
            location_id, routing_mode, created_at, updated_at
        ) values (
            gen_random_uuid(), demo_business_id, 'CLOUD_API', 'WHATSAPP',
            'META_CLOUD_API_DEMO_' || gen_random_uuid()::text, 'DISCONNECTED',
            '56927305158', '+56 9 2730 5158', '56927305158', true,
            'PENDING', 'DISCONNECTED', 'DISABLED', 'INVALID',
            '110af553-fa85-2477-a95a-6c4da89d4db5', 'LOCATION_SPECIFIC',
            current_timestamp, current_timestamp
        );
    end if;

    -- 2. Actualizar WHATSAPP_WEB demo: asignar phone_number solo si está vacío
    --    y que NO esté ya ocupado por META_CLOUD_API (misma business + phone)
    update channel_account
    set phone_number = '56927305158',
        display_phone_number = '+56 9 2730 5158',
        normalized_phone_number = '56927305158',
        updated_at = current_timestamp
    where business_id = demo_business_id
      and provider_name = 'WHATSAPP_WEB'
      and (phone_number is null or phone_number = '')
      and not exists (
          select 1 from channel_account ca2
          where ca2.business_id = demo_business_id
            and ca2.phone_number = '56927305158'
            and ca2.provider_name in ('CLOUD_API', 'META_CLOUD_API')
      );

end $$;
