-- V51__add_last_qr_at_to_channel_account.sql
-- Agrega columna last_qr_at para tracking de cuándo se generó el último QR

alter table channel_account
    add column if not exists last_qr_at timestamptz;

comment on column channel_account.last_qr_at is 'Momento en que se generó el último código QR para escaneo';
