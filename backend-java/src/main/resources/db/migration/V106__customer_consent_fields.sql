-- =============================================================================
-- FASE 2: Campos de consentimiento para customer
-- Agrega consent_sms, consent_marketing y consent_whatsapp para gestion
-- explicita de permisos de comunicacion. Parte de la regla de aislamiento
-- y separacion de datos de prueba.
-- =============================================================================

alter table customer
    add column if not exists consent_sms boolean not null default false,
    add column if not exists consent_marketing boolean not null default false,
    add column if not exists consent_whatsapp boolean not null default false,
    add column if not exists consent_updated_at timestamptz;

comment on column customer.consent_sms is 'Cliente dio consentimiento para notificaciones SMS';
comment on column customer.consent_marketing is 'Cliente dio consentimiento para comunicaciones de marketing';
comment on column customer.consent_whatsapp is 'Cliente dio consentimiento para notificaciones por WhatsApp';
comment on column customer.consent_updated_at is 'Ultima actualizacion de permisos de consentimiento';
