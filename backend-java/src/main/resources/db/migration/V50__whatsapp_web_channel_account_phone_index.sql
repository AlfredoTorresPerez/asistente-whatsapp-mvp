-- V50__whatsapp_web_channel_account_phone_index.sql
-- Agrega índice en channel_account(phone_number) para lookup rápido por teléfono
-- y constraint unique en (business_id, phone_number) para upsert

create index if not exists idx_channel_account_phone_number
    on channel_account (phone_number)
    where channel_type = 'WHATSAPP';

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'uk_channel_account_business_phone'
          and conrelid = 'channel_account'::regclass
    ) then
        alter table channel_account
            add constraint uk_channel_account_business_phone
            unique (business_id, phone_number);
    end if;
end;
$$;

comment on index idx_channel_account_phone_number is 'Búsqueda rápida de channel_account por phone_number para webhook fallback';
comment on constraint uk_channel_account_business_phone on channel_account is 'Unicidad por negocio y teléfono para upsert en webhook';