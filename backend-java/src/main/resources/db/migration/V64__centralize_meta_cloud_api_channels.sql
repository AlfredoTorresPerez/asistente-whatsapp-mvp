-- V64: Centralizar canales META_CLOUD_API (routing_mode=CENTRALIZED, location_id=null)
-- Revierte el cambio de V63 que asoció el canal a la sede principal.
-- Sin eliminar datos ni credenciales existentes.
-- Idempotente.

-- 1. Eliminar registros huérfanos de META_CLOUD_API sin business_id válido (limpieza previa)
delete from channel_account
where provider_name = 'META_CLOUD_API'
  and business_id is not null
  and not exists (select 1 from business where id = channel_account.business_id);

-- 2. Resolver duplicados: si una empresa tiene más de un canal META_CLOUD_API,
--    conservar el que tenga más datos (credenciales, phone_number_id, etc.)
--    y desactivar los demás.
do $$
declare
    dup_rec record;
    keep_id uuid;
begin
    for dup_rec in (
        select business_id, count(*) as cnt
        from channel_account
        where provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
          and active = true
        group by business_id
        having count(*) > 1
    ) loop
        -- Elegir el que tenga phone_number_id no nulo (preferido) o el más reciente
        select id into keep_id
        from channel_account
        where business_id = dup_rec.business_id
          and provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
        order by
            case when phone_number_id is not null then 0 else 1 end,
            updated_at desc
        limit 1;

        -- Desactivar los demás duplicados (conservando datos)
        update channel_account
        set active = false,
            operational_status = 'DISCONNECTED',
            updated_at = current_timestamp
        where business_id = dup_rec.business_id
          and provider_name = 'META_CLOUD_API'
          and channel_type = 'WHATSAPP'
          and id <> keep_id;
    end loop;
end $$;

-- 3. Reemplazar constraint uq_channel_account_business_global_channel (V18)
--    que impedía tener WHATSAPP_WEB y META_CLOUD_API simultáneamente.
--    La nueva semántica: solo META_CLOUD_API central tiene unicidad por negocio.
--    (Debe hacerse ANTES de centralizar para evitar colisión al setear location_id=null)
drop index if exists uq_channel_account_business_global_channel;

-- 4. Centralizar todos los canales META_CLOUD_API activos
--    routing_mode = CENTRALIZED, location_id = null
update channel_account
set routing_mode = 'CENTRALIZED',
    location_id = null,
    updated_at = current_timestamp
where provider_name = 'META_CLOUD_API'
  and channel_type = 'WHATSAPP'
  and (
      routing_mode is distinct from 'CENTRALIZED'
      or location_id is not null
  );

-- 5. Unique index: máximo un META_CLOUD_API central activo por empresa
do $$
begin
    if not exists (
        select 1 from pg_indexes
        where indexname = 'uq_channel_account_business_central_meta'
    ) then
        create unique index uq_channel_account_business_central_meta
            on channel_account (business_id)
            where provider_name = 'META_CLOUD_API'
              and routing_mode = 'CENTRALIZED'
              and active = true;
    end if;
end $$;

-- 6. Reemplazar el índice único parcial de phone_number_id (V59)
--    para que sea global a todos los proveedores, no solo CLOUD_API.
drop index if exists idx_channel_account_phone_number_id;

-- 6a. Antes de crear el índice global, resolver posibles duplicados de phone_number_id
--     entre diferentes proveedores (ej. un CLOUD_API legacy y un META_CLOUD_API nuevo).
do $$
declare
    dup_phone record;
    keep_id uuid;
begin
    for dup_phone in (
        select phone_number_id, count(*) as cnt
        from channel_account
        where phone_number_id is not null
          and phone_number_id != ''
        group by phone_number_id
        having count(*) > 1
    ) loop
        -- Conservar el META_CLOUD_API activo; si no, el más reciente
        select id into keep_id
        from channel_account
        where phone_number_id = dup_phone.phone_number_id
        order by
            case when provider_name = 'META_CLOUD_API' and active = true then 0 else 1 end,
            updated_at desc
        limit 1;

        -- Limpiar phone_number_id de los demás para poder crear el índice único
        update channel_account
        set phone_number_id = null,
            updated_at = current_timestamp
        where phone_number_id = dup_phone.phone_number_id
          and id <> keep_id;
    end loop;
end $$;

-- 6b. Crear índice único global de phone_number_id
do $$
begin
    if not exists (
        select 1 from pg_indexes
        where indexname = 'uq_channel_account_phone_number_id'
    ) then
        create unique index uq_channel_account_phone_number_id
            on channel_account (phone_number_id)
            where phone_number_id is not null
              and phone_number_id != '';
    end if;
end $$;

-- 7. Asegurar consistencia: CENTRALIZED => location_id IS NULL
alter table channel_account
    drop constraint if exists chk_channel_account_routing_consistency;

alter table channel_account
    add constraint chk_channel_account_routing_consistency
        check (
            (routing_mode = 'CENTRALIZED' and location_id is null)
            or (routing_mode = 'LOCATION_SPECIFIC' and location_id is not null)
            or (routing_mode is null)
        );

-- 8. Agregar CHECK para que META_CLOUD_API siempre tenga routing_mode definido
alter table channel_account
    drop constraint if exists chk_channel_account_meta_routing;

alter table channel_account
    add constraint chk_channel_account_meta_routing
        check (
            provider_name != 'META_CLOUD_API'
            or (routing_mode in ('CENTRALIZED', 'LOCATION_SPECIFIC'))
        );

-- 9. Actualizar el provider_account_id index para incluir META_CLOUD_API
drop index if exists idx_channel_account_provider_account_id;

create index if not exists idx_channel_account_provider_account_id
    on channel_account (provider_account_id)
    where provider_name in ('CLOUD_API', 'META_CLOUD_API')
      and provider_account_id is not null
      and provider_account_id != '';

comment on index uq_channel_account_business_central_meta is 'Maximo un canal META_CLOUD_API central activo por empresa';
comment on index uq_channel_account_phone_number_id is 'Phone Number ID globalmente unico entre todos los proveedores';
comment on constraint chk_channel_account_routing_consistency on channel_account is 'CENTRALIZED requiere location_id null; LOCATION_SPECIFIC requiere location_id no nulo';
comment on constraint chk_channel_account_meta_routing on channel_account is 'META_CLOUD_API siempre debe tener routing_mode definido';
