-- V33: guardas transaccionales para reservas.
-- Agrega versionado optimista y restricciones anti-solape en PostgreSQL.

create extension if not exists btree_gist;

alter table booking
    add column if not exists version integer not null default 0;

update booking
set ends_at = starts_at + make_interval(mins => duration_minutes)
where ends_at is null;

alter table booking
    drop constraint if exists chk_booking_version_non_negative,
    drop constraint if exists chk_booking_valid_time_range,
    add constraint chk_booking_version_non_negative
        check (version >= 0),
    add constraint chk_booking_valid_time_range
        check (ends_at is not null and ends_at > starts_at);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'ex_booking_professional_no_overlap_active'
    ) then
        alter table booking
            add constraint ex_booking_professional_no_overlap_active
            exclude using gist (
                business_id with =,
                professional_id with =,
                tstzrange(starts_at, ends_at, '[)') with &&
            )
            where (
                professional_id is not null
                and status in (
                    'REQUESTED', 'TEMPORARY', 'PENDIENTE_CONFIRMACION', 'CONFIRMED', 'RESCHEDULED', 'REPROGRAMADA',
                    'SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE'
                )
            )
            deferrable initially immediate;
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'ex_booking_room_no_overlap_active'
    ) then
        alter table booking
            add constraint ex_booking_room_no_overlap_active
            exclude using gist (
                business_id with =,
                room_id with =,
                tstzrange(starts_at, ends_at, '[)') with &&
            )
            where (
                room_id is not null
                and status in (
                    'REQUESTED', 'TEMPORARY', 'PENDIENTE_CONFIRMACION', 'CONFIRMED', 'RESCHEDULED', 'REPROGRAMADA',
                    'SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE'
                )
            )
            deferrable initially immediate;
    end if;
end $$;
