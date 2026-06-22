alter table channel_account
    drop constraint if exists chk_channel_account_provider_name;

alter table channel_account
    add constraint chk_channel_account_provider_name
        check (provider_name in ('WHATSAPP_WEB', 'CLOUD_API'));
