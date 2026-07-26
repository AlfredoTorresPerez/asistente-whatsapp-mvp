-- =============================================================================
-- MEJORA: booking_reminder para procesamiento concurrente, reintentos y
-- recuperacion de registros abandonados
-- =============================================================================
-- 1. Agrega columnas para procesamiento concurrente y reintentos
-- 2. Agrega estado PROCESSING
-- 3. Agrega restriccion unica con appointment_revision
-- 4. Agrega indices para busqueda con FOR UPDATE SKIP LOCKED
-- =============================================================================

alter table booking_reminder
    add column if not exists appointment_revision integer not null default 0,
    add column if not exists processing_started_at timestamp with time zone,
    add column if not exists processing_instance varchar(100),
    add column if not exists attempt_count integer not null default 0,
    add column if not exists next_attempt_at timestamp with time zone,
    add column if not exists provider_message_id varchar(255),
    add column if not exists last_error_code varchar(50);

-- Actualizar estado check constraint para incluir PROCESSING
alter table booking_reminder
    drop constraint if exists chk_booking_reminder_status;

alter table booking_reminder
    add constraint chk_booking_reminder_status
        check (status in ('PENDING', 'SCHEDULED', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED', 'SKIPPED'));

-- Reemplazar unique constraint: usar appointment_revision en vez de scheduled_at
-- para permitir reprogramacion sin duplicados
alter table booking_reminder
    drop constraint if exists uq_booking_reminder_type;

alter table booking_reminder
    add constraint uq_booking_reminder_type
        unique (business_id, booking_id, reminder_type, channel_type, appointment_revision);

-- Indices para FOR UPDATE SKIP LOCKED
create index if not exists idx_booking_reminder_dispatch
    on booking_reminder (status, scheduled_at, next_attempt_at)
    where status in ('PENDING', 'RETRY');

create index if not exists idx_booking_reminder_processing_recovery
    on booking_reminder (processing_started_at)
    where status = 'PROCESSING';

-- Indice para busqueda por cita (cancelacion, reprogramacion)
create index if not exists idx_booking_reminder_booking
    on booking_reminder (business_id, booking_id);

-- Actualizar registros existentes con revision 0
update booking_reminder set appointment_revision = 0 where appointment_revision is null;
