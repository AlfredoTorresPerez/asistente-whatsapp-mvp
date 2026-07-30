-- V91: Admin CRUD mantenedores - Profesionales, Cabinas, Asignaciones
-- Agrega campos faltantes a tablas existentes + nuevos permisos + datos demo

-- 1. Campos adicionales para aesthetic_professional
alter table aesthetic_professional
    add column if not exists email varchar(255),
    add column if not exists phone varchar(30),
    add column if not exists display_name varchar(160),
    add column if not exists description text,
    add column if not exists color varchar(7),
    add column if not exists certification_ref varchar(120),
    add column if not exists avatar_url varchar(500),
    add column if not exists min_minutes_between_appointments integer not null default 0;

alter table aesthetic_professional
    drop constraint if exists chk_aesthetic_professional_min_minutes,
    add constraint chk_aesthetic_professional_min_minutes
        check (min_minutes_between_appointments >= 0);

-- Sincronizar display_name desde full_name para registros existentes
update aesthetic_professional
set display_name = full_name,
    updated_at = current_timestamp
where display_name is null;

-- 2. Campos adicionales para agenda_room
alter table agenda_room
    add column if not exists description text,
    add column if not exists color varchar(7);

-- 3. Nuevos permisos para mantenedores
insert into permission (id, code, name, module_name, description)
values
    ('91000000-0000-0000-0000-000000000001', 'PROFESSIONAL_VIEW', 'Ver profesionales', 'PROFESSIONALS', 'Permite listar y ver detalle de profesionales.'),
    ('91000000-0000-0000-0000-000000000002', 'PROFESSIONAL_MANAGE', 'Gestionar profesionales', 'PROFESSIONALS', 'Permite crear, editar y desactivar profesionales.'),
    ('91000000-0000-0000-0000-000000000003', 'ROOM_VIEW', 'Ver cabinas y recursos', 'ROOMS', 'Permite listar y ver detalle de cabinas y recursos.'),
    ('91000000-0000-0000-0000-000000000004', 'ROOM_MANAGE', 'Gestionar cabinas y recursos', 'ROOMS', 'Permite crear, editar y desactivar cabinas y recursos.'),
    ('91000000-0000-0000-0000-000000000005', 'ASSIGNMENT_VIEW', 'Ver asignaciones', 'ASSIGNMENTS', 'Permite ver asignaciones de profesionales y cabinas a servicios.'),
    ('91000000-0000-0000-0000-000000000006', 'ASSIGNMENT_MANAGE', 'Gestionar asignaciones', 'ASSIGNMENTS', 'Permite crear y eliminar asignaciones.')
on conflict (code) do nothing;

-- 4. Asignar nuevos permisos a roles
-- OWNER: todos
insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r
cross join permission p
where r.code = 'OWNER'
  and p.code in ('PROFESSIONAL_VIEW', 'PROFESSIONAL_MANAGE', 'ROOM_VIEW', 'ROOM_MANAGE', 'ASSIGNMENT_VIEW', 'ASSIGNMENT_MANAGE')
on conflict (role_id, permission_id) do nothing;

-- ADMIN: todos
insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r
cross join permission p
where r.code = 'ADMIN'
  and p.code in ('PROFESSIONAL_VIEW', 'PROFESSIONAL_MANAGE', 'ROOM_VIEW', 'ROOM_MANAGE', 'ASSIGNMENT_VIEW', 'ASSIGNMENT_MANAGE')
on conflict (role_id, permission_id) do nothing;

-- SUPERVISOR: solo lectura
insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r
cross join permission p
where r.code = 'SUPERVISOR'
  and p.code in ('PROFESSIONAL_VIEW', 'ROOM_VIEW', 'ASSIGNMENT_VIEW')
on conflict (role_id, permission_id) do nothing;

-- AGENT: solo lectura
insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r
cross join permission p
where r.code = 'AGENT'
  and p.code in ('PROFESSIONAL_VIEW', 'ROOM_VIEW', 'ASSIGNMENT_VIEW')
on conflict (role_id, permission_id) do nothing;

-- SALES: solo lectura
insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r
cross join permission p
where r.code = 'SALES'
  and p.code in ('PROFESSIONAL_VIEW', 'ROOM_VIEW', 'ASSIGNMENT_VIEW')
on conflict (role_id, permission_id) do nothing;

-- 5. Agregar segunda sede demo (Las Condes) con cabinas adicionales
insert into business_location (id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone, active)
select
    '91000000-0000-0000-0000-000000000010',
    '11111111-1111-1111-1111-111111111111',
    'las-condes',
    'Centro Estetico Bella - Las Condes',
    'Av. Las Condes 8320, Santiago',
    'Santiago',
    'Las Condes',
    '+56955550200',
    '+56955550200',
    'America/Santiago',
    true
where not exists (
    select 1 from business_location
    where business_id = '11111111-1111-1111-1111-111111111111'
      and code = 'las-condes'
);

-- 6. Actualizar nombres de cabinas demo existentes (sede principal)
update agenda_room
set name = 'Cabina Facial Principal',
    description = 'Cabina equipada para tratamientos faciales, evaluaciones y radiofrecuencia.',
    color = '#E8F4FD',
    room_type = 'FACIAL',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'principal-cabina-1';

update agenda_room
set name = 'Cabina Corporal y Depilacion',
    description = 'Cabina multiuso para tratamientos corporales, depilacion laser y servicios rapidos.',
    color = '#F3E8FF',
    room_type = 'CORPORAL',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'principal-cabina-2';

-- 7. Agregar cabinas adicionales en sede principal
insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes, description, color)
select
    '91000000-0000-0000-0000-000000000020',
    bl.business_id,
    bl.id,
    'principal-cabina-3',
    'Sala de Manicure y Pedicure',
    'MANOS_PIES',
    2,
    true,
    'Cabina con dos puestos para atencion simultanea.',
    'Espacio equipado con mesas de manicure y pedicure.',
    '#FFF3E0'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'principal'
  and not exists (
      select 1 from agenda_room r
      where r.business_id = bl.business_id
        and r.code = 'principal-cabina-3'
  );

insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes, description, color)
select
    '91000000-0000-0000-0000-000000000021',
    bl.business_id,
    bl.id,
    'principal-cabina-4',
    'Sala de Peluqueria y Maquillaje',
    'PELUQUERIA',
    2,
    true,
    'Espacio con espejos grandes y silla hidraulica.',
    'Sala especialmente acondicionada para servicios de peluqueria y maquillaje.',
    '#E8F5E9'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'principal'
  and not exists (
      select 1 from agenda_room r
      where r.business_id = bl.business_id
        and r.code = 'principal-cabina-4'
  );

-- 8. Agregar cabinas en sede Las Condes
insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes, description, color)
select
    '91000000-0000-0000-0000-000000000030',
    bl.business_id,
    bl.id,
    'las-condes-cabina-1',
    'Cabina Premium Las Condes',
    'FACIAL_CORPORAL',
    1,
    true,
    'Cabina de atencion preferencial con equipamiento completo.',
    'Cabina de gran capacidad con equipos faciales y corporales.',
    '#FCE4EC'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'las-condes'
  and not exists (
      select 1 from agenda_room r
      where r.business_id = bl.business_id
        and r.code = 'las-condes-cabina-1'
  );

insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes, description, color)
select
    '91000000-0000-0000-0000-000000000031',
    bl.business_id,
    bl.id,
    'las-condes-cabina-2',
    'Sala Express Las Condes',
    'DEPILACION_MANOS',
    1,
    true,
    'Cabina para servicios de depilacion y manicure express.',
    'Espacio optimizado para atencion rapida.',
    '#E0F7FA'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'las-condes'
  and not exists (
      select 1 from agenda_room r
      where r.business_id = bl.business_id
        and r.code = 'las-condes-cabina-2'
  );

-- 9. Agregar profesionales a sede Las Condes
insert into aesthetic_professional_location (id, business_id, professional_id, location_id, active)
select
    '91000000-0000-0000-0000-000000000040',
    p.business_id,
    p.id,
    l.id,
    true
from aesthetic_professional p
cross join business_location l
where p.business_id = '11111111-1111-1111-1111-111111111111'
  and l.business_id = '11111111-1111-1111-1111-111111111111'
  and l.code = 'las-condes'
  and not exists (
      select 1 from aesthetic_professional_location apl
      where apl.business_id = p.business_id
        and apl.professional_id = p.id
        and apl.location_id = l.id
  );

-- 10. Agregar horarios de atencion para Las Condes
insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, d.day_of_week, '10:00'::time, '20:00'::time, true
from business_location bl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'las-condes'
  and not exists (
      select 1 from agenda_business_hours bh
      where bh.business_id = bl.business_id
        and bh.location_id = bl.id
        and bh.day_of_week = d.day_of_week
        and bh.start_time = '10:00'::time
  );

insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, 6, '10:00'::time, '15:00'::time, true
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'las-condes'
  and not exists (
      select 1 from agenda_business_hours bh
      where bh.business_id = bl.business_id
        and bh.location_id = bl.id
        and bh.day_of_week = 6
        and bh.start_time = '10:00'::time
  );

-- 11. Agregar horarios de profesionales para Las Condes
insert into agenda_professional_hours (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '10:00'::time, '19:00'::time, true
from aesthetic_professional_location apl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where apl.business_id = '11111111-1111-1111-1111-111111111111'
  and apl.location_id = '91000000-0000-0000-0000-000000000010'
  and not exists (
      select 1 from agenda_professional_hours ph
      where ph.business_id = apl.business_id
        and ph.professional_id = apl.professional_id
        and ph.location_id = apl.location_id
        and ph.day_of_week = d.day_of_week
  );

-- 12. Asignar servicios a nuevas cabinas
-- Cabina 3 (Manicure): servicios de manicure/pedicure
insert into agenda_room_service (id, business_id, room_id, service_id, active)
select gen_random_uuid(), '11111111-1111-1111-1111-111111111111', r.id, s.id, true
from agenda_room r
join aesthetic_service s on s.business_id = r.business_id
where r.id = '91000000-0000-0000-0000-000000000020'
  and s.code in (
      'MAN-TRADICIONAL', 'MAN-PERMANENTE', 'PED-SPA',
      'MAN-ACRILICAS', 'MAN-GEL', 'MAN-RETIRO'
  )
  and not exists (
      select 1 from agenda_room_service rs
      where rs.business_id = r.business_id
        and rs.room_id = r.id
        and rs.service_id = s.id
  );

-- Cabina 4 (Peluqueria): servicios de peluqueria/maquillaje
insert into agenda_room_service (id, business_id, room_id, service_id, active)
select gen_random_uuid(), '11111111-1111-1111-1111-111111111111', r.id, s.id, true
from agenda_room r
join aesthetic_service s on s.business_id = r.business_id
where r.id in ('91000000-0000-0000-0000-000000000021', '91000000-0000-0000-0000-000000000030')
  and s.code in (
      'PEL-CORTE', 'PEL-LAVADO', 'PEL-BRUSHING', 'PEL-TINTURA',
      'PEL-BALAYAGE', 'PEL-ALISADO', 'PEL-TRATAMIENTO-CAPILAR',
      'MAQ-SOCIAL', 'MAQ-NOVIA', 'MAQ-EVENTOS', 'MAQ-AUTOMAQUILLAJE'
  )
  and not exists (
      select 1 from agenda_room_service rs
      where rs.business_id = r.business_id
        and rs.room_id = r.id
        and rs.service_id = s.id
  );

-- Cabina express Las Condes: servicios de depilacion
insert into agenda_room_service (id, business_id, room_id, service_id, active)
select gen_random_uuid(), '11111111-1111-1111-1111-111111111111', r.id, s.id, true
from agenda_room r
join aesthetic_service s on s.business_id = r.business_id
where r.id = '91000000-0000-0000-0000-000000000031'
  and s.code in (
      'DEP-LASER', 'DEP-CERA', 'DEP-CEJAS', 'DEP-BOZO', 'DEP-AXILAS', 'DEP-PIERNAS', 'DEP-ROSTRO'
  )
  and not exists (
      select 1 from agenda_room_service rs
      where rs.business_id = r.business_id
        and rs.room_id = r.id
        and rs.service_id = s.id
  );

-- 13. Actualizar datos demo de profesionales con campos extendidos
update aesthetic_professional
set
    email = 'carla.mendez@centroesteticobella.cl',
    phone = '+56955550101',
    display_name = 'Carla Mendez',
    description = 'Cosmetologa facial con mas de 8 anos de experiencia en limpieza profunda, peeling y evaluacion estetica.',
    color = '#E8F4FD',
    certification_ref = 'COS-FAC-001',
    qualification_level = 3,
    certification_valid_until = '2028-12-31',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and id = '71000000-0000-0000-0000-000000000001';

update aesthetic_professional
set
    email = 'valentina.rios@centroesteticobella.cl',
    phone = '+56955550102',
    display_name = 'Valentina Rios',
    description = 'Masoterapeuta especializada en tratamientos corporales reductivos, drenaje linfatico y presoterapia.',
    color = '#F3E8FF',
    certification_ref = 'CORP-001',
    qualification_level = 3,
    certification_valid_until = '2028-12-31',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and id = '71000000-0000-0000-0000-000000000002';

update aesthetic_professional
set
    email = 'daniela.soto@centroesteticobella.cl',
    phone = '+56955550103',
    display_name = 'Daniela Soto',
    description = 'Especialista en depilacion laser, cera, perfilado de cejas, lifting y extension de pestanas.',
    color = '#FFF3E0',
    certification_ref = 'DEP-LAS-002',
    qualification_level = 2,
    certification_valid_until = '2028-06-30',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and id = '71000000-0000-0000-0000-000000000003';

update aesthetic_professional
set
    email = 'marcela.fuentes@centroesteticobella.cl',
    phone = '+56955550104',
    display_name = 'Marcela Fuentes',
    description = 'Manicurista y estilista con experiencia en unas acrilicas, gel, pedicure spa, corte y coloracion capilar.',
    color = '#E8F5E9',
    certification_ref = 'MAN-PEL-003',
    qualification_level = 2,
    certification_valid_until = '2028-09-30',
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and id = '71000000-0000-0000-0000-000000000004';

-- 14. Agregar indice compuesto para consultas de disponibilidad
create index if not exists idx_booking_professional_slot
    on booking (business_id, professional_id, starts_at, ends_at, status)
    where professional_id is not null
      and status not in ('CANCELADA', 'EXPIRADA', 'ATENDIDA', 'NO_ASISTE');

create index if not exists idx_booking_room_slot
    on booking (business_id, room_id, starts_at, ends_at, status)
    where room_id is not null
      and status not in ('CANCELADA', 'EXPIRADA', 'ATENDIDA', 'NO_ASISTE');
