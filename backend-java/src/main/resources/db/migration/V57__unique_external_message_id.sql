-- V57: Agrega constraint UNIQUE (business_id, external_message_id) en message
-- para evitar duplicados por redelivery de webhook con diferente deliveryId.
-- Sin CONCURRENTLY porque Flyway corre en exclusión mutua durante el startup.

-- Solo aplica cuando external_message_id no es nulo ni vacio
do $$
begin
    if not exists (
        select 1 from pg_indexes
        where indexname = 'uq_message_business_external_message_id'
    ) then
        create unique index uq_message_business_external_message_id
            on message (business_id, external_message_id)
            where external_message_id is not null and external_message_id != '';
    end if;
end $$;
