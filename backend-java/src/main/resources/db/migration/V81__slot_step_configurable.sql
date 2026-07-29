-- =============================================================================
-- V81: SLOT_STEP_MINUTES CONFIGURABLE POR POLITICA
--
-- Agrega SLOT_CONFIG como tipo de politica valido en business_policy y registra
-- slot_step_minutes como regla configurable por negocio/sucursal.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Extender check constraint para incluir SLOT_CONFIG
-- ---------------------------------------------------------------------------
alter table business_policy
    drop constraint if exists chk_bp_policy_type;

alter table business_policy
    add constraint chk_bp_policy_type check (policy_type in (
        'CANCELLATION', 'RESCHEDULE', 'MAX_ADVANCE', 'MIN_ADVANCE',
        'TOLERANCE', 'PENALTY', 'CONFIRMATION', 'DEPOSIT', 'SLOT_CONFIG'
    ));

-- ---------------------------------------------------------------------------
-- 2. Seed: slot_step_minutes = 15 para el negocio demo (version 1)
-- ---------------------------------------------------------------------------
do $$
declare
    v_id uuid;
begin
    select id into v_id from business_policy_version
    where business_id = (select id from business where code = 'CENTRO-ESTETICO-DEMO')
      and version = 1
    limit 1;

    if v_id is not null then
        insert into business_policy (id, version_id, policy_type, policy_key, policy_value, priority)
        values (gen_random_uuid(), v_id, 'SLOT_CONFIG', 'default',
                '{"slot_step_minutes": 15, "min_slot_duration_minutes": 15}'::jsonb, 0)
        on conflict (version_id, location_id, policy_type, policy_key) do nothing;
    end if;
end $$;

comment on column booking.policy_snapshot is
    'Snapshot JSONB de las reglas aplicables al momento de la reserva
     (cancellation_window_hours, reschedule_window_hours, max_advance_days,
      min_advance_minutes, tolerance_minutes, penalty_percent, penalty_amount,
      reschedule_max_count, grace_period_minutes, slot_step_minutes).';
