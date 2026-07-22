-- Actualizar CHECK constraint de provider_name para incluir META_CLOUD_API
-- Valor antiguo: ('WHATSAPP_WEB', 'CLOUD_API')
-- Nuevo valor:   ('WHATSAPP_WEB', 'CLOUD_API', 'META_CLOUD_API')

alter table channel_account
    drop constraint if exists chk_channel_account_provider_name;

alter table channel_account
    add constraint chk_channel_account_provider_name
        check (provider_name in ('WHATSAPP_WEB', 'CLOUD_API', 'META_CLOUD_API'));

-- Actualizar registros CLOUD_API a META_CLOUD_API
update channel_account
set provider_name = 'META_CLOUD_API',
    updated_at = current_timestamp
where provider_name = 'CLOUD_API';
