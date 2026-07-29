-- =============================================================================
-- V85: ENTRADA, RESULTADO Y TIEMPO DE EVALUACION EN DESCARTE DE SLOTS
--
-- Complementa V84 con campos explicitos exigidos por la matriz: entrada de
-- regla, resultado y tiempo de evaluacion.
-- =============================================================================

alter table agenda_slot_discard_trace
    add column if not exists rule_input jsonb not null default '{}'::jsonb,
    add column if not exists result varchar(40) not null default 'DISCARDED',
    add column if not exists evaluation_ms integer not null default 0;

alter table agenda_slot_discard_trace
    drop constraint if exists chk_asdt_result,
    drop constraint if exists chk_asdt_evaluation_ms,
    add constraint chk_asdt_result check (result in ('DISCARDED')),
    add constraint chk_asdt_evaluation_ms check (evaluation_ms >= 0);
