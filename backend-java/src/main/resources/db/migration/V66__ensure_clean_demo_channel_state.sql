do $$
declare
    demo_business_id uuid := '11111111-1111-1111-1111-111111111111';
    v_principal_location_id uuid;
    v_meta_channel_id uuid;
    v_meta_active_count integer;
    v_web_channel_id uuid;
begin
    -- Ensure principal location is active (V31 may have deactivated it)
    select id into v_principal_location_id
    from business_location
    where business_id = demo_business_id
      and code = 'principal';
    if v_principal_location_id is not null then
        update business_location
        set active = true,
            updated_at = current_timestamp
        where id = v_principal_location_id
          and active = false;
    end if;

    -- Remove duplicate META_CLOUD_API channels, keep only the best one
    select count(*) into v_meta_active_count
    from channel_account
    where business_id = demo_business_id
      and provider_name = 'META_CLOUD_API'
      and channel_type = 'WHATSAPP'
      and active = true;

    if v_meta_active_count > 1 then
        -- Keep the one with CENTRALIZED routing and most data
        select id into v_meta_channel_id
        from channel_account
        where business_id = demo_business_id
          and provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
          and active = true
        order by
            case when routing_mode = 'CENTRALIZED' then 0 else 1 end,
            case when phone_number_id is not null then 0 else 1 end,
            updated_at desc
        limit 1;

        update channel_account
        set active = false,
            operational_status = 'DISCONNECTED',
            updated_at = current_timestamp
        where business_id = demo_business_id
          and provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
          and active = true
          and id <> v_meta_channel_id;
    end if;

    -- Ensure exactly one META_CLOUD_API channel exists with CENTRALIZED routing
    select id into v_meta_channel_id
    from channel_account
    where business_id = demo_business_id
      and provider_name = 'META_CLOUD_API'
      and channel_type = 'WHATSAPP'
      and routing_mode = 'CENTRALIZED'
      and active = true;

    if v_meta_channel_id is null then
        -- Check if there's an inactive one we can reactivate
        select id into v_meta_channel_id
        from channel_account
        where business_id = demo_business_id
          and provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
          and routing_mode = 'CENTRALIZED'
        order by updated_at desc
        limit 1;

        if v_meta_channel_id is not null then
            update channel_account
            set active = true,
                operational_status = 'DISCONNECTED',
                updated_at = current_timestamp
            where id = v_meta_channel_id;
        else
            -- Check if there's a non-centralized one we can promote
            select id into v_meta_channel_id
            from channel_account
            where business_id = demo_business_id
              and provider_name = 'META_CLOUD_API'
              and channel_type = 'WHATSAPP'
            order by updated_at desc
            limit 1;

            if v_meta_channel_id is not null then
                update channel_account
                set routing_mode = 'CENTRALIZED',
                    location_id = null,
                    active = true,
                    operational_status = 'DISCONNECTED',
                    updated_at = current_timestamp
                where id = v_meta_channel_id;
            else
                -- No META_CLOUD_API channel exists at all, create one
                insert into channel_account (
                    id, business_id, provider_name, channel_type, session_key, status,
                    phone_number, display_phone_number, normalized_phone_number, active,
                    registration_status, operational_status, webhook_status, credential_status,
                    routing_mode, created_at, updated_at
                ) values (
                    gen_random_uuid(), demo_business_id, 'META_CLOUD_API', 'WHATSAPP',
                    'META_CLOUD_API_DEMO_' || gen_random_uuid()::text, 'DISCONNECTED',
                    '56927305158', '+56 9 2730 5158', '56927305158', true,
                    'PENDING', 'DISCONNECTED', 'DISABLED', 'INVALID',
                    'CENTRALIZED', current_timestamp, current_timestamp
                );
            end if;
        end if;
    end if;

    -- Ensure WHATSAPP_WEB channel exists and has a valid location
    select id into v_web_channel_id
    from channel_account
    where business_id = demo_business_id
      and provider_name = 'WHATSAPP_WEB'
      and channel_type = 'WHATSAPP';

    if v_web_channel_id is not null then
        -- Update WHATSAPP_WEB phone to avoid conflict with META_CLOUD_API
        -- and ensure it's location-specific
        update channel_account
        set location_id = coalesce(location_id, v_principal_location_id),
            routing_mode = 'LOCATION_SPECIFIC',
            updated_at = current_timestamp
        where id = v_web_channel_id
          and (location_id is null or routing_mode = 'CENTRALIZED');
    end if;

    -- Ensure uk_channel_account_business_phone constraint is satisfied:
    -- META_CLOUD_API has phone '56927305158', WHATSAPP_WEB should have null phone
    -- (to avoid duplicate key on same business)
    if v_web_channel_id is not null then
        update channel_account
        set phone_number = null,
            display_phone_number = null,
            normalized_phone_number = null,
            updated_at = current_timestamp
        where id = v_web_channel_id
          and phone_number = '56927305158';
    end if;

    -- Verify the provider_name CHECK constraint includes all expected values
    alter table channel_account
        drop constraint if exists chk_channel_account_provider_name;

    alter table channel_account
        add constraint chk_channel_account_provider_name
            check (provider_name in ('WHATSAPP_WEB', 'CLOUD_API', 'META_CLOUD_API'));

    raise notice 'V66: Demo channel state verified and fixed';
end $$;
