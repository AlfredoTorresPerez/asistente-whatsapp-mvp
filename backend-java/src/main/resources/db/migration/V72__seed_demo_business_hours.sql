insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, d.day_of_week, '09:00'::time, '19:00'::time, true
from business_location bl
cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
where bl.business_id = '11111111-1111-1111-1111-111111111111' and bl.active = true
on conflict (business_id, location_id, day_of_week, start_time, end_time) do nothing;

insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
select gen_random_uuid(), bl.business_id, bl.id, 6, '10:00'::time, '14:00'::time, true
from business_location bl
where bl.business_id = '11111111-1111-1111-1111-111111111111' and bl.active = true
on conflict (business_id, location_id, day_of_week, start_time, end_time) do nothing;
