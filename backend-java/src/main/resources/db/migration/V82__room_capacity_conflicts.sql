-- =============================================================================
-- V82: CAPACIDAD REAL POR SALA
--
-- El constraint ex_booking_room_no_overlap_active modelaba cada sala como si
-- tuviera capacidad 1. La capacidad real se valida transaccionalmente en la
-- aplicacion bloqueando agenda_room y contando reservas activas solapadas.
-- =============================================================================

alter table booking
    drop constraint if exists ex_booking_room_no_overlap_active;

create index if not exists idx_booking_room_active_overlap
    on booking (business_id, room_id, starts_at, ends_at)
    where room_id is not null
      and status in (
          'REQUESTED', 'TEMPORARY', 'PENDIENTE_CONFIRMACION', 'CONFIRMED', 'RESCHEDULED', 'REPROGRAMADA',
          'SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE'
      );
