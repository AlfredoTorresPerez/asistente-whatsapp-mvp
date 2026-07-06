-- V37: Refresca fechas demo a Junio/Julio 2026 y agrega usuarios demo por rol
-- Las migraciones V1-V36 no se modifican para no romper checksums de Flyway.

-- 1. Actualizar fechas de entidades demo a fechas recientes
--    Desplazar todo ~34 dias adelante (de mayo a junio/julio 2026)

-- user_account: ultimo login
update user_account
set last_login_at = '2026-06-25T14:00:00Z'
where email = 'admin@demo.cl';

-- conversation: fechas
update conversation
set
    last_message_at = '2026-06-25T16:00:00Z',
    last_message_preview = 'Quiero saber el precio de la limpieza facial.',
    opened_at = '2026-06-25T15:50:00Z'
where id = '64000000-0000-0000-0000-000000000001';

update conversation
set
    last_message_at = '2026-06-24T16:30:00Z',
    opened_at = '2026-06-24T16:00:00Z'
where id = '64000000-0000-0000-0000-000000000002';

-- message: fechas
update message
set
    received_at = '2026-06-25T15:55:00Z'
where id = '65000000-0000-0000-0000-000000000001'
  and received_at is not null;

update message
set
    sent_at = '2026-06-25T16:00:00Z'
where id = '65000000-0000-0000-0000-000000000002';

-- booking: reprogramar a fechas futuras (actualizar ends_at junto con starts_at)
update booking
set
    starts_at = '2026-06-30T14:00:00Z',
    ends_at = '2026-06-30T14:45:00Z',
    completed_at = null
where id = '68000000-0000-0000-0000-000000000001';

update booking
set
    starts_at = '2026-07-02T17:00:00Z',
    ends_at = '2026-07-02T17:45:00Z',
    completed_at = null
where id = '68000000-0000-0000-0000-000000000002';

-- payment: fecha reciente
update payment
set paid_at = '2026-06-25T19:10:00Z'
where id = '69200000-0000-0000-0000-000000000001';

-- automation_execution
update automation_execution
set executed_at = '2026-06-25T19:20:00Z'
where id = '69400000-0000-0000-0000-000000000001';

-- channel_event_log
update channel_event_log
set
    received_at = '2026-06-25T15:55:05Z',
    processed_at = '2026-06-25T15:55:07Z'
where id = '69600000-0000-0000-0000-000000000001';

-- message_delivery_log
update message_delivery_log
set occurred_at = '2026-06-25T16:00:10Z'
where id = '69700000-0000-0000-0000-000000000001';

-- audit_log
update audit_log
set occurred_at = '2026-06-25T15:50:00Z'
where id = '69900000-0000-0000-0000-000000000001';

update audit_log
set occurred_at = '2026-06-25T19:20:01Z'
where id = '69900000-0000-0000-0000-000000000002';

-- 2. Agregar usuarios demo con distintos roles para demostracion
--    Se crean solo si no existen (por email unico por business_id)

-- password hash para 'Cambiar123!' (mismo que admin demo)
-- Nota: en ambiente local se puede cambiar la clave manualmente

do $$
declare
    v_business_id constant uuid := '11111111-1111-1111-1111-111111111111';
    v_password_hash constant text := '$2a$10$n7vTmgWhJDL9XDuOn9e5ve6NAhXH4zP6WtU0b7ib/KcN7/TfIz0Gi';
begin
    -- AGENT: agente de ventas con acceso operativo
    insert into user_account (id, business_id, first_name, last_name, email, phone, password_hash, timezone, status, last_login_at, failed_login_attempts)
    values ('40000000-0000-0000-0000-000000000002', v_business_id, 'Pedro', 'Lopez', 'agente@demo.cl', '+56955550102', v_password_hash, 'America/Santiago', 'ACTIVE', '2026-06-25T12:00:00Z', 0)
    on conflict (business_id, email) do nothing;

    insert into user_role (id, business_id, user_id, role_id)
    values ('50000000-0000-0000-0000-000000000002', v_business_id, '40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003')
    on conflict (user_id, role_id) do nothing;

    -- SUPERVISOR: solo lectura
    insert into user_account (id, business_id, first_name, last_name, email, phone, password_hash, timezone, status, last_login_at, failed_login_attempts)
    values ('40000000-0000-0000-0000-000000000003', v_business_id, 'Laura', 'Garcia', 'supervisor@demo.cl', '+56955550103', v_password_hash, 'America/Santiago', 'ACTIVE', '2026-06-25T10:00:00Z', 0)
    on conflict (business_id, email) do nothing;

    insert into user_role (id, business_id, user_id, role_id)
    values ('50000000-0000-0000-0000-000000000003', v_business_id, '40000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002')
    on conflict (user_id, role_id) do nothing;

    -- ADMIN: administrador operativo
    insert into user_account (id, business_id, first_name, last_name, email, phone, password_hash, timezone, status, last_login_at, failed_login_attempts)
    values ('40000000-0000-0000-0000-000000000004', v_business_id, 'Ana', 'Martinez', 'admin2@demo.cl', '+56955550104', v_password_hash, 'America/Santiago', 'ACTIVE', null, 0)
    on conflict (business_id, email) do nothing;

    insert into user_role (id, business_id, user_id, role_id)
    values ('50000000-0000-0000-0000-000000000004', v_business_id, '40000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002')
    on conflict (user_id, role_id) do nothing;
end $$;
