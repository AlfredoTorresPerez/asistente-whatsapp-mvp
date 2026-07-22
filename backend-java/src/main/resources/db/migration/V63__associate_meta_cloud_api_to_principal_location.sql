-- V63: Asociar canal META_CLOUD_API a la sede principal con routing LOCATION_SPECIFIC
-- Corrige V61 que dejó el canal sin location_id (centralizado)
-- y V62 que migró el provider_name a META_CLOUD_API pero sin ajustar location/routing.

do $$
declare
    demo_business_id uuid := '11111111-1111-1111-1111-111111111111';
    principal_location_id uuid;
    cloud_channel_id uuid;
    cloud_channel_location uuid;
begin
    -- 1. Obtener la sede principal de la empresa demo (puede estar inactiva por V31)
    select id into principal_location_id
    from business_location
    where business_id = demo_business_id
      and code = 'principal'
      and active = true;

    if principal_location_id is null then
        -- V31 marco la sede principal como inactiva; la reactivamos para asociar el canal
        update business_location
        set active = true,
            updated_at = current_timestamp
        where business_id = demo_business_id
          and code = 'principal';

        select id into principal_location_id
        from business_location
        where business_id = demo_business_id
          and code = 'principal';
    end if;

    -- 2. Obtener el canal META_CLOUD_API existente
    select id, location_id into cloud_channel_id, cloud_channel_location
    from channel_account
    where business_id = demo_business_id
      and provider_name = 'META_CLOUD_API'
      and channel_type = 'WHATSAPP';

    if cloud_channel_id is null then
        raise notice 'No se encontro canal META_CLOUD_API; posiblemente ya fue eliminado. Se creara uno nuevo.';
    end if;

    -- 3. Si el canal existe pero no tiene location_id, actualizarlo
    if cloud_channel_id is not null and cloud_channel_location is null then
        update channel_account
        set location_id = principal_location_id,
            routing_mode = 'LOCATION_SPECIFIC',
            adapter_mode = 'META_CLOUD_API_CLOUD_API',
            registration_status = 'PENDING',
            operational_status = 'DISCONNECTED',
            updated_at = current_timestamp,
            version = version + 1
        where id = cloud_channel_id;

        raise notice 'Canal META_CLOUD_API actualizado a LOCATION_SPECIFIC con sede principal';
    end if;

    -- 4. Si el canal ya tiene location_id, verificar que sea correcta
    if cloud_channel_id is not null and cloud_channel_location is not null then
        if cloud_channel_location <> principal_location_id then
            update channel_account
            set location_id = principal_location_id,
                routing_mode = 'LOCATION_SPECIFIC',
                updated_at = current_timestamp,
                version = version + 1
            where id = cloud_channel_id;

            raise notice 'Canal META_CLOUD_API reasignado a la sede principal correcta';
        else
            -- Asegurar routing_mode aunque location ya sea correcta
            update channel_account
            set routing_mode = 'LOCATION_SPECIFIC',
                updated_at = current_timestamp,
                version = version + 1
            where id = cloud_channel_id
              and routing_mode is distinct from 'LOCATION_SPECIFIC';

            raise notice 'Canal META_CLOUD_API ya estaba asociado a la sede principal; routing_mode actualizado';
        end if;
    end if;

    -- 5. Si el canal no existe, crearlo con la asociacion correcta
    if cloud_channel_id is null then
        insert into channel_account (
            id, business_id, provider_name, channel_type, session_key, status,
            phone_number, display_phone_number, normalized_phone_number,
            location_id, routing_mode, adapter_mode,
            active, registration_status, operational_status, webhook_status, credential_status,
            created_at, updated_at
        ) values (
            gen_random_uuid(), demo_business_id, 'META_CLOUD_API', 'WHATSAPP',
            'META_CLOUD_API_' || gen_random_uuid()::text, 'DISCONNECTED',
            '56927305158', '+56 9 2730 5158', '56927305158',
            principal_location_id, 'LOCATION_SPECIFIC', 'META_CLOUD_API_CLOUD_API',
            true, 'PENDING', 'DISCONNECTED', 'DISABLED', 'INVALID',
            current_timestamp, current_timestamp
        );

        raise notice 'Canal META_CLOUD_API creado con sede principal y LOCATION_SPECIFIC';
    end if;
end $$;
