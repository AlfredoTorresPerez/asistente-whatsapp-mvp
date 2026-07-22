alter table channel_account
    add column if not exists display_phone_number varchar(30),
    add column if not exists normalized_phone_number varchar(30),
    add column if not exists registration_status varchar(30) not null default 'NOT_CONFIGURED',
    add column if not exists operational_status varchar(30) not null default 'INACTIVE',
    add column if not exists webhook_status varchar(30) not null default 'NOT_CONFIGURED',
    add column if not exists credential_status varchar(30) not null default 'NOT_CONFIGURED',
    add column if not exists encrypted_access_token text,
    add column if not exists encrypted_verify_token varchar(255),
    add column if not exists token_expires_at timestamp with time zone,
    add column if not exists last_health_check_at timestamp with time zone,
    add column if not exists last_message_received_at timestamp with time zone,
    add column if not exists last_message_sent_at timestamp with time zone,
    add column if not exists last_error_code varchar(80),
    add column if not exists version integer not null default 0;

update channel_account
set display_phone_number = phone_number,
    normalized_phone_number = regexp_replace(phone_number, '\D', '', 'g')
where provider_name = 'CLOUD_API'
  and display_phone_number is null;

create index if not exists idx_channel_account_tenant_provider_active
    on channel_account (business_id, provider_name, active);

comment on column channel_account.display_phone_number is 'Numero visible formateado (ej. +56 9 2730 5158)';
comment on column channel_account.normalized_phone_number is 'Numero normalizado (ej. 56927305158)';
comment on column channel_account.registration_status is 'NOT_CONFIGURED, PENDING, REGISTERED, ERROR';
comment on column channel_account.operational_status is 'INACTIVE, CONFIGURING, CONNECTED, DEGRADED, DISCONNECTED, ERROR';
comment on column channel_account.webhook_status is 'NOT_CONFIGURED, PENDING_VALIDATION, VERIFIED, SUBSCRIBED, ERROR';
comment on column channel_account.credential_status is 'NOT_CONFIGURED, CONFIGURED, EXPIRING, EXPIRED, INVALID';
comment on column channel_account.encrypted_access_token is 'Token de acceso cifrado (nunca en texto plano)';
comment on column channel_account.encrypted_verify_token is 'Token de verificacion del webhook cifrado';
comment on column channel_account.token_expires_at is 'Fecha de expiracion del token de acceso';
comment on column channel_account.last_health_check_at is 'Ultima validacion de conectividad con Meta';
comment on column channel_account.last_message_received_at is 'Fecha del ultimo mensaje recibido';
comment on column channel_account.last_message_sent_at is 'Fecha del ultimo mensaje enviado';
comment on column channel_account.last_error_code is 'Codigo del ultimo error de Meta';
comment on column channel_account.version is 'Control de concurrencia optimista';
