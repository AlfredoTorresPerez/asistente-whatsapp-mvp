-- V31: Corrige datos demo y resolucion de sucursal para que las reservas por WhatsApp
-- usen la sucursal solicitada por el cliente y no la sede principal heredada de la conversacion.

-- Asegurar sedes demo activas. Esto corrige bases ya existentes donde V18/V27 no habian
-- dejado Providencia/Maipu/Santiago Centro disponibles para la agenda.
insert into business_location (
    id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone, active, opening_hours, notes
)
values
    ('81000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'providencia', 'Providencia', 'Av. Providencia 2450, Santiago', 'Santiago', 'Providencia', '+56955550100', '+56955550100', 'America/Santiago', true, '{"lun-vie":"09:00-19:00","sab":"10:00-14:00"}'::jsonb, 'Sede central para demostracion multisede.'),
    ('81000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'maipu', 'Maipu', 'Av. Pajaritos 3211, Maipu', 'Santiago', 'Maipu', '+56955550200', '+56955550200', 'America/Santiago', true, '{"lun-vie":"10:00-20:00","sab":"10:00-15:00"}'::jsonb, 'Sede demo zona poniente.'),
    ('81000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'santiago-centro', 'Santiago Centro', 'Huerfanos 920, Santiago', 'Santiago', 'Santiago Centro', '+56955550300', '+56955550300', 'America/Santiago', true, '{"lun-vie":"09:30-18:30"}'::jsonb, 'Sede demo centro.')
on conflict (business_id, code) do update
set name = excluded.name,
    address = excluded.address,
    city = excluded.city,
    commune = excluded.commune,
    phone = excluded.phone,
    whatsapp_number = excluded.whatsapp_number,
    timezone = excluded.timezone,
    active = true,
    opening_hours = excluded.opening_hours,
    notes = excluded.notes,
    updated_at = current_timestamp;

-- La sede principal queda como alias administrativo legacy para datos antiguos, pero no se ofrece
-- como sucursal operativa de agenda demo. Esto evita que "en Providencia" se resuelva por la
-- comuna de la sede principal cuando el cliente esta pidiendo la sucursal Providencia.
update business_location
set active = false,
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'principal';

-- Asegurar que todos los servicios demo, incluida Limpieza facial profunda, esten configurados
-- en las sedes operativas visibles.
insert into aesthetic_service_location (id, business_id, service_id, location_id, active, price_override, duration_override_minutes)
select gen_random_uuid(), s.business_id, s.id, bl.id, true, null, null
from aesthetic_service s
join business_location bl on bl.business_id = s.business_id
where s.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, service_id, location_id) do update
set active = true,
    updated_at = current_timestamp;

-- Asociar profesionales demo a sedes operativas.
insert into aesthetic_professional_location (id, business_id, professional_id, location_id, active)
select gen_random_uuid(), ap.business_id, ap.id, bl.id, true
from aesthetic_professional ap
join business_location bl on bl.business_id = ap.business_id
where ap.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, professional_id, location_id) do update
set active = true,
    updated_at = current_timestamp;

-- Horarios operativos por sede.
insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, d.day_of_week, '09:00'::time, '19:00'::time, true
from business_location bl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, location_id, day_of_week, start_time, end_time) do update
set active = true,
    updated_at = current_timestamp;

insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, 6, '10:00'::time, '14:00'::time, true
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, location_id, day_of_week, start_time, end_time) do update
set active = true,
    updated_at = current_timestamp;

-- Horarios de profesionales por sede.
insert into professional_location_schedule (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '09:00'::time, '18:00'::time, true
from aesthetic_professional_location apl
join business_location bl on bl.id = apl.location_id and bl.business_id = apl.business_id
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where apl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do update
set active = true,
    updated_at = current_timestamp;

insert into agenda_professional_hours (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '09:00'::time, '18:00'::time, true
from aesthetic_professional_location apl
join business_location bl on bl.id = apl.location_id and bl.business_id = apl.business_id
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where apl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do update
set active = true,
    updated_at = current_timestamp;

-- Cabinas por sede y servicios asociados.
insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes)
select gen_random_uuid(), bl.business_id, bl.id, lower(bl.code || '-cabina-1'), 'Cabina 1 - ' || bl.name, 'FACIAL_CORPORAL', 1, true, 'Cabina demo para servicios esteticos.'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, location_id, code) do update
set active = true,
    updated_at = current_timestamp;

insert into agenda_room (id, business_id, location_id, code, name, room_type, capacity, active, notes)
select gen_random_uuid(), bl.business_id, bl.id, lower(bl.code || '-cabina-2'), 'Cabina 2 - ' || bl.name, 'DEPILACION_MANOS', 1, true, 'Cabina demo para servicios rapidos.'
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, location_id, code) do update
set active = true,
    updated_at = current_timestamp;

insert into agenda_room_service (id, business_id, room_id, service_id, active)
select gen_random_uuid(), r.business_id, r.id, s.id, true
from agenda_room r
join business_location bl on bl.id = r.location_id and bl.business_id = r.business_id
join aesthetic_service s on s.business_id = r.business_id
where r.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code in ('providencia', 'maipu', 'santiago-centro')
on conflict (business_id, room_id, service_id) do update
set active = true,
    updated_at = current_timestamp;

insert into agenda_professional_service (id, business_id, professional_id, service_id, active)
select gen_random_uuid(), p.business_id, p.id, s.id, true
from aesthetic_professional p
join aesthetic_service s on s.business_id = p.business_id
where p.business_id = '11111111-1111-1111-1111-111111111111'
on conflict (business_id, professional_id, service_id) do update
set active = true,
    updated_at = current_timestamp;

-- Si una conversacion demo quedo asociada a la sede principal por la resolucion anterior,
-- dejarla sin sede para que la proxima solicitud use la sucursal mencionada por el cliente.
update conversation c
set location_id = null,
    updated_at = current_timestamp
from business_location bl
where c.business_id = bl.business_id
  and c.location_id = bl.id
  and c.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'principal';
