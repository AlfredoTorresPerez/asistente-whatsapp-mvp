-- V54: OAuth state table for OAuth2 PKCE-less flow
-- Stores temporary OAuth state parameters for calendar integrations

create table if not exists oauth_state (
    id uuid not null primary key,
    business_id uuid not null,
    provider varchar(20) not null,
    state_hash varchar(64) not null,
    redirect_uri text,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    consumed boolean not null default false
);

create unique index if not exists idx_oauth_state_hash on oauth_state (state_hash);
create index if not exists idx_oauth_state_expires_at on oauth_state (expires_at);
