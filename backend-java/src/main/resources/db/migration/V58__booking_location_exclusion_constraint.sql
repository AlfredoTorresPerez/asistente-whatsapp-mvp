-- V58: Exclusion constraint para booking sin professional ni room.
-- Cubre el gap donde professional_id y room_id son ambos null,
-- usando location_id como unico discriminante de solapamiento.
-- Requiere btree_gist (habilitado en V33).

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'ex_booking_location_no_overlap_active'
    ) then
        alter table booking
            add constraint ex_booking_location_no_overlap_active
            exclude using gist (
                business_id with =,
                location_id with =,
                tstzrange(starts_at, ends_at, '[)') with &&
            )
            where (
                professional_id is null
                and room_id is null
                and status in (
                    'REQUESTED', 'TEMPORARY', 'PENDIENTE_CONFIRMACION', 'CONFIRMED', 'RESCHEDULED', 'REPROGRAMADA',
                    'SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE'
                )
            )
            deferrable initially immediate;
    end if;
end $$;
