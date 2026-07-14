-- V48: Booking sync engine + intent expressions + IA booking facts

-- ============================================================
-- 1. Booking sync event outbox
-- ============================================================

create table if not exists booking_sync_event (
    id uuid primary key,
    booking_id uuid not null,
    business_id uuid not null,
    event_type varchar(30) not null check (event_type in ('RESERVA_CREADA', 'RESERVA_CANCELADA', 'RESERVA_REPROGRAMADA')),
    event_version int not null default 1,
    event_body jsonb not null,
    idempotency_key varchar(100) not null,
    status varchar(20) not null default 'PENDING' check (status in ('PENDING', 'PROCESSING', 'SYNCED', 'FAILED', 'SKIPPED')),
    attempts int not null default 0,
    max_attempts int not null default 5,
    next_attempt_at timestamp with time zone not null default current_timestamp,
    locked_at timestamp with time zone,
    last_error_code varchar(100),
    last_error_message text,
    trace_id varchar(100),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index if not exists idx_booking_sync_event_status
    on booking_sync_event (status, next_attempt_at)
    where status in ('PENDING', 'PROCESSING');

create unique index if not exists uq_booking_sync_event_idempotency
    on booking_sync_event (idempotency_key);

create index if not exists idx_booking_sync_event_booking
    on booking_sync_event (booking_id, event_type);

-- ============================================================
-- 2. IA booking facts table
-- ============================================================

create table if not exists ia_hecho_reserva (
    booking_id uuid primary key,
    business_id uuid not null,
    customer_phone varchar(30) not null,
    customer_name varchar(160),
    customer_management_id varchar(200),
    service_name varchar(200),
    location_name varchar(200),
    professional_name varchar(200),
    booking_date date,
    booking_time time,
    booking_status varchar(30) not null,
    conversation_id uuid,
    channel_origin varchar(30),
    origin_intent varchar(30) default 'reservar',
    tiene_reserva_activa boolean not null default true,
    booking_created_at timestamp with time zone,
    sync_status varchar(20) not null default 'PENDING' check (sync_status in ('PENDING', 'SYNCED', 'ERROR')),
    synced_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_ia_hecho_reserva_business
        foreign key (business_id) references business (id) on delete cascade
);

create index if not exists idx_ia_hecho_reserva_phone
    on ia_hecho_reserva (business_id, customer_phone);

create index if not exists idx_ia_hecho_reserva_active
    on ia_hecho_reserva (business_id, tiene_reserva_activa, booking_date)
    where tiene_reserva_activa = true;

-- ============================================================
-- 3. Add sync columns to booking
-- ============================================================

alter table booking
    add column if not exists sync_status varchar(20) not null default 'PENDING'
        check (sync_status in ('PENDING', 'SYNCED', 'ERROR'));

alter table booking
    add column if not exists conversation_id uuid;

alter table booking
    add column if not exists channel_origin varchar(30);

alter table booking
    add column if not exists origin_intent varchar(30) default 'reservar';

create index if not exists idx_booking_sync_status
    on booking (id)
    where sync_status is not null;

-- ============================================================
-- 4. Intent expressions (data-driven intent detection)
-- ============================================================

create table if not exists intencion_expresion (
    id uuid primary key,
    business_id uuid not null,
    intencion_canonica varchar(50) not null,
    expresion varchar(500) not null,
    ejemplo text,
    accion varchar(100) not null default 'MOSTRAR_FORMULARIO_RESERVA',
    prioridad int not null default 100,
    activo boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_intencion_expresion_business
        foreign key (business_id) references business (id) on delete cascade
);

create index if not exists idx_intencion_expresion_lookup
    on intencion_expresion (business_id, intencion_canonica, activo);

insert into intencion_expresion (id, business_id, intencion_canonica, expresion, ejemplo, accion, prioridad)
values
    ('a1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'cancelar', 'Quiero cancelar mi hora', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'cancela', 'Cancela mi cita porfa', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'cancelacion', 'Necesito cancelacion de hora', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'cancelación', 'Necesito cancelación de hora', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'anular', 'Anula mi reserva', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'anule', 'Anule la hora porfa', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'anula', 'Anula mi cita', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'cancelada', 'Mi reserva esta cancelada', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'cancelar_reserva', 'cancelado', 'Quedó cancelado?', 'GESTIONAR_CANCELACION', 10),
    ('a1000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'reprogramar', 'Necesito reprogramar mi cita', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'reagendar', 'Quiero reagendar mi hora', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'reprogramacion', 'Solicito reprogramacion', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'reprogramación', 'Solicito reprogramación', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar hora', 'Quiero cambiar mi hora', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000015', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar mi hora', 'Necesito cambiar mi hora', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar cita', 'Cambiar cita porfa', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000017', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar mi cita', 'Cambiar mi cita de horario', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000018', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambio de hora', 'Necesito un cambio de hora', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000019', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambio la hora', 'Se puede cambiar la hora?', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000020', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'modificar cita', 'Modificar cita existente', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000021', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'modificar mi cita', 'Modificar mi cita por favor', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000022', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'mover', 'Mover mi hora', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000023', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'mover mi hora', 'Mover mi hora de atencion', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000024', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambio de fecha', 'Necesito cambio de fecha', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000025', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar de fecha', 'Cambiar de fecha la reserva', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000026', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiarme', 'Quiero cambiarme de horario', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000027', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar la hora', 'Cambiar la hora de atencion', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000028', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar de hora', 'Cambiar de hora porfa', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000029', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar mi reserva', 'Cambiar mi reserva de horario', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000030', '11111111-1111-1111-1111-111111111111', 'reprogramar_reserva', 'cambiar reserva', 'Cambiar reserva existente', 'GESTIONAR_REPROGRAMACION', 20),
    ('a1000000-0000-0000-0000-000000000031', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'tengo agendado', 'Tengo algo agendado?', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000032', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'tengo agendada', 'Tengo agendada una hora?', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000033', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'tengo reserva', 'Tengo una reserva?', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000034', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'mi reserva', 'Quiero ver mi reserva', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000035', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'mis reservas', 'Ver mis reservas activas', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000036', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'revisar agenda', 'Revisar mi agenda', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000037', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'tengo cita', 'Tengo cita agendada?', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000038', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'tengo una cita', 'Tengo una cita?', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000039', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'estado reserva', 'Cual es el estado de mi reserva', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000040', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'ver mi cita', 'Quiero ver mi cita', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000041', '11111111-1111-1111-1111-111111111111', 'consultar_reservas', 'confirmar mi hora', 'Confirmar mi hora agendada', 'CONSULTAR_RESERVAS', 30),
    ('a1000000-0000-0000-0000-000000000042', '11111111-1111-1111-1111-111111111111', 'reservar', 'reservar', 'Hola quiero reservar', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000043', '11111111-1111-1111-1111-111111111111', 'reservar', 'agendar', 'Quiero agendar una hora', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000044', '11111111-1111-1111-1111-111111111111', 'reservar', 'programar cita', 'Programar una cita', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000045', '11111111-1111-1111-1111-111111111111', 'reservar', 'pedir cita', 'Quiero pedir cita', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000046', '11111111-1111-1111-1111-111111111111', 'reservar', 'solicitar cita', 'Solicitar una cita', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000047', '11111111-1111-1111-1111-111111111111', 'reservar', 'pedir hora', 'Pedir hora para atencion', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000048', '11111111-1111-1111-1111-111111111111', 'reservar', 'tomar hora', 'Tomar hora para el viernes', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000049', '11111111-1111-1111-1111-111111111111', 'reservar', 'sacar hora', 'Sacar hora para manicure', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000050', '11111111-1111-1111-1111-111111111111', 'reservar', 'sacar turno', 'Sacar turno para manana', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000051', '11111111-1111-1111-1111-111111111111', 'reservar', 'tomar turno', 'Tomar turno con la especialista', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000052', '11111111-1111-1111-1111-111111111111', 'reservar', 'pedir turno', 'Pedir turno por favor', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000053', '11111111-1111-1111-1111-111111111111', 'reservar', 'sacar cita', 'Sacar cita para consulta', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000054', '11111111-1111-1111-1111-111111111111', 'reservar', 'apartar cita', 'Apartar una cita para el sabado', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000055', '11111111-1111-1111-1111-111111111111', 'reservar', 'apartar hora', 'Apartar una hora por favor', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000056', '11111111-1111-1111-1111-111111111111', 'reservar', 'separar hora', 'Separar hora para depilacion', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000057', '11111111-1111-1111-1111-111111111111', 'reservar', 'separar cupo', 'Separar cupo para la tarde', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000058', '11111111-1111-1111-1111-111111111111', 'reservar', 'concertar cita', 'Concertar cita con el profesional', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000059', '11111111-1111-1111-1111-111111111111', 'reservar', 'gestionar reserva', 'Gestionar una reserva', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000060', '11111111-1111-1111-1111-111111111111', 'reservar', 'hacer reserva', 'Hacer una reserva', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000061', '11111111-1111-1111-1111-111111111111', 'reservar', 'crear reserva', 'Crear una reserva nueva', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000062', '11111111-1111-1111-1111-111111111111', 'reservar', 'reservar cita online', 'Reservar cita online', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000063', '11111111-1111-1111-1111-111111111111', 'reservar', 'reservar consulta', 'Reservar una consulta', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000064', '11111111-1111-1111-1111-111111111111', 'reservar', 'agendar consulta', 'Agendar consulta', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000065', '11111111-1111-1111-1111-111111111111', 'reservar', 'programar consulta', 'Programar consulta', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000066', '11111111-1111-1111-1111-111111111111', 'reservar', 'solicitar atencion', 'Solicitar atencion', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000067', '11111111-1111-1111-1111-111111111111', 'reservar', 'reservar servicio', 'Reservar servicio de radiofrecuencia', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000068', '11111111-1111-1111-1111-111111111111', 'reservar', 'agendar servicio', 'Agendar un servicio', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000069', '11111111-1111-1111-1111-111111111111', 'reservar', 'tomar sesion', 'Tomar una sesion esta semana', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000070', '11111111-1111-1111-1111-111111111111', 'reservar', 'reservar sesion', 'Reservar una sesion', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000071', '11111111-1111-1111-1111-111111111111', 'reservar', 'inscribirme', 'Inscribirme para el tratamiento', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000072', '11111111-1111-1111-1111-111111111111', 'reservar', 'matricularme', 'Matricularme en la clase', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000073', '11111111-1111-1111-1111-111111111111', 'reservar', 'tienen hora', 'Tienen hora para hoy?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000074', '11111111-1111-1111-1111-111111111111', 'reservar', 'hay disponibilidad', 'Hay disponibilidad manana?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000075', '11111111-1111-1111-1111-111111111111', 'reservar', 'que horas tienen', 'Que horas tienen para unas?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000076', '11111111-1111-1111-1111-111111111111', 'reservar', 'me das una hora', 'Me das una hora para el viernes?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000077', '11111111-1111-1111-1111-111111111111', 'reservar', 'me anotas', 'Me anotas para manana?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000078', '11111111-1111-1111-1111-111111111111', 'reservar', 'me agendas', 'Me agendas con Camila?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000079', '11111111-1111-1111-1111-111111111111', 'reservar', 'me reservas', 'Me reservas un cupo?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000080', '11111111-1111-1111-1111-111111111111', 'reservar', 'quiero una hora', 'Quiero una hora para evaluacion', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000081', '11111111-1111-1111-1111-111111111111', 'reservar', 'necesito una hora', 'Necesito una hora urgente', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000082', '11111111-1111-1111-1111-111111111111', 'reservar', 'quiero atenderme', 'Quiero atenderme con ustedes', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000083', '11111111-1111-1111-1111-111111111111', 'reservar', 'quiero ir', 'Quiero ir manana en la tarde', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000084', '11111111-1111-1111-1111-111111111111', 'reservar', 'puedo ir', 'Puedo ir manana?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000085', '11111111-1111-1111-1111-111111111111', 'reservar', 'me interesa servicio', 'Me interesa el servicio, cuando puedo ir?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000086', '11111111-1111-1111-1111-111111111111', 'reservar', 'reservar y pagar', 'Reservar y pagar ahora', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000087', '11111111-1111-1111-1111-111111111111', 'reservar', 'enviame el enlace', 'Enviame el link para agendar', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000088', '11111111-1111-1111-1111-111111111111', 'reservar', 'link de reserva', 'Tienen link de reserva?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000089', '11111111-1111-1111-1111-111111111111', 'reservar', 'agenda disponible', 'La agenda esta disponible?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000090', '11111111-1111-1111-1111-111111111111', 'reservar', 'cupo disponible', 'Queda cupo disponible?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000091', '11111111-1111-1111-1111-111111111111', 'reservar', 'una horita', 'Una horita para hoy?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000092', '11111111-1111-1111-1111-111111111111', 'reservar', 'cupito', 'Tendran un cupito manana?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000093', '11111111-1111-1111-1111-111111111111', 'reservar', 'turnito', 'Tienen un turnito libre?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000094', '11111111-1111-1111-1111-111111111111', 'reservar', 'entrar a la agenda', 'Quiero entrar a la agenda', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000095', '11111111-1111-1111-1111-111111111111', 'reservar', 'reserbar', 'Quiero reserbar para manana', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000096', '11111111-1111-1111-1111-111111111111', 'reservar', 'recervar', 'Necesito recervar una hora', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000097', '11111111-1111-1111-1111-111111111111', 'reservar', 'resarvar', 'Quiero resarvar', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000098', '11111111-1111-1111-1111-111111111111', 'reservar', 'ajendar', 'Quiero ajendar una hora', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000099', '11111111-1111-1111-1111-111111111111', 'reservar', 'agndar', 'Necesito agndar para hoy', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000100', '11111111-1111-1111-1111-111111111111', 'reservar', 'agendarme', 'Quiero agendarme', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000101', '11111111-1111-1111-1111-111111111111', 'reservar', 'pedir ora', 'Quiero pedir ora para manana', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000102', '11111111-1111-1111-1111-111111111111', 'reservar', 'sacar hroa', 'Puedo sacar hroa?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000103', '11111111-1111-1111-1111-111111111111', 'reservar', 'cita xfa', 'Cita xfa para hoy', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000104', '11111111-1111-1111-1111-111111111111', 'reservar', 'hora xfa', 'Hora xfa para limpieza', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000105', '11111111-1111-1111-1111-111111111111', 'reservar', 'urgente hora', 'Urgente necesito hora hoy', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000106', '11111111-1111-1111-1111-111111111111', 'reservar', 'lo antes posible', 'Necesito agendar lo antes posible', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000107', '11111111-1111-1111-1111-111111111111', 'reservar', 'para hoy', 'Tienen hora para hoy?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000108', '11111111-1111-1111-1111-111111111111', 'reservar', 'para manana', 'Quiero reservar para manana', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000109', '11111111-1111-1111-1111-111111111111', 'reservar', 'esta semana', 'Agendar esta semana', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000110', '11111111-1111-1111-1111-111111111111', 'reservar', 'primera hora', 'Tienen primera hora el lunes?', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000111', '11111111-1111-1111-1111-111111111111', 'reservar', 'ultima hora', 'Quiero la ultima hora del dia', 'MOSTRAR_FORMULARIO_RESERVA', 40),
    ('a1000000-0000-0000-0000-000000000112', '11111111-1111-1111-1111-111111111111', 'reservar', 'despues de la pega', 'Hay hora despues de la pega?', 'MOSTRAR_FORMULARIO_RESERVA', 40);
