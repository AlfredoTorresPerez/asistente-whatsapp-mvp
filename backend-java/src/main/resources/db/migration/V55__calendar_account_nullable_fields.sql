-- V55: Make calendar_integration_account fields nullable and add new columns
-- Allows OAuth accounts to be created before token exchange completes

alter table calendar_integration_account alter column email drop not null;
alter table calendar_integration_account alter column access_token_encrypted drop not null;

alter table calendar_integration_account add column if not exists revoked_at timestamptz;
alter table calendar_integration_account add column if not exists last_sync_at timestamptz;
alter table calendar_integration_account add column if not exists requires_reconnect boolean not null default false;
