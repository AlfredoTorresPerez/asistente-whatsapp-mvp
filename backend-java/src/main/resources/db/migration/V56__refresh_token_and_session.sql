-- =============================================================================
-- REFRESH TOKEN Y GESTION DE SESIONES
-- =============================================================================
-- Proposito: Permitir revocacion de sesiones, rotacion de refresh tokens
-- y deteccion de reutilizacion.
-- =============================================================================

create table if not exists user_session (
    id                  uuid primary key,
    business_id         uuid not null references business(id),
    user_id             uuid not null references user_account(id),
    refresh_token_hash  varchar(64) not null unique,
    device_info         varchar(500),
    ip_address          varchar(45),
    created_at          timestamp with time zone not null default current_timestamp,
    last_used_at        timestamp with time zone not null default current_timestamp,
    expires_at          timestamp with time zone not null,
    revoked_at          timestamp with time zone,
    revoked_by          uuid references user_account(id)
);

create index if not exists idx_user_session_user on user_session(business_id, user_id);
create index if not exists idx_user_session_hash on user_session(refresh_token_hash);
create index if not exists idx_user_session_expires on user_session(expires_at) where revoked_at is null;

comment on table user_session is 'Sesiones activas con refresh tokens. Solo se almacena hash del token.';
comment on column user_session.refresh_token_hash is 'SHA-256 del refresh token original';
comment on column user_session.revoked_at is 'Si no es null, la sesion fue revocada';
