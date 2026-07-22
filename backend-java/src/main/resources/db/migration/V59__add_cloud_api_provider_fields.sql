-- V59: Agrega columnas para asociar channel_account con cuentas de WhatsApp Cloud API
-- provider_account_id: WABA ID o business account ID del proveedor
-- phone_number_id: Phone Number ID de Meta para identificacion multitenant

alter table channel_account
    add column if not exists provider_account_id varchar(120),
    add column if not exists phone_number_id varchar(120);

-- Indice para lookup por phone_number_id (unico dentro de Cloud API)
do $$
begin
    if not exists (
        select 1 from pg_indexes
        where indexname = 'idx_channel_account_phone_number_id'
    ) then
        create unique index idx_channel_account_phone_number_id
            on channel_account (phone_number_id)
            where provider_name = 'CLOUD_API'
              and phone_number_id is not null
              and phone_number_id != '';
    end if;
end;
$$;

-- Indice para lookup por provider_account_id (WABA ID)
do $$
begin
    if not exists (
        select 1 from pg_indexes
        where indexname = 'idx_channel_account_provider_account_id'
    ) then
        create index idx_channel_account_provider_account_id
            on channel_account (provider_account_id)
            where provider_name = 'CLOUD_API'
              and provider_account_id is not null
              and provider_account_id != '';
    end if;
end;
$$;

comment on column channel_account.provider_account_id is 'WABA ID o Business Account ID del proveedor (Cloud API)';
comment on column channel_account.phone_number_id is 'Phone Number ID de Meta para resolucion multitenant en webhooks';
comment on index idx_channel_account_phone_number_id is 'Unicidad de phone_number_id dentro de CLOUD_API para webhook resolution';
comment on index idx_channel_account_provider_account_id is 'Busqueda de channel_account por WABA ID';
