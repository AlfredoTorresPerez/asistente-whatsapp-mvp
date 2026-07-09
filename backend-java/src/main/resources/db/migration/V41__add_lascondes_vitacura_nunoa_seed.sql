-- V41: Agrega 3 sucursales demo adicionales (Las Condes, Vitacura, Ñuñoa)
-- para alcanzar 6 sucursales operativas y habilitar pruebas multi-sucursal.
-- Complementa las sedes existentes (Providencia, Maipu, Santiago Centro) de V31.

do $$
declare
    v_business_id constant uuid := '11111111-1111-1111-1111-111111111111';
    v_location record;
begin
    -- 1. Insertar las 3 nuevas sucursales
    insert into business_location (id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone, active, opening_hours, notes)
    values
        ('81000000-0000-0000-0000-000000000004', v_business_id, 'las-condes', 'Las Condes', 'Av. Apoquindo 4800, Las Condes', 'Santiago', 'Las Condes', '+56955550400', '+56955550400', 'America/Santiago', true, '{"lun-vie":"09:00-20:00","sab":"10:00-14:00"}'::jsonb, 'Sede demo zona oriente.'),
        ('81000000-0000-0000-0000-000000000005', v_business_id, 'vitacura', 'Vitacura', 'Av. Vitacura 5200, Vitacura', 'Santiago', 'Vitacura', '+56955550500', '+56955550500', 'America/Santiago', true, '{"lun-vie":"08:00-19:00","sab":"09:00-13:00"}'::jsonb, 'Sede demo zona alta.'),
        ('81000000-0000-0000-0000-000000000006', v_business_id, 'nunoa', 'Ñuñoa', 'Av. Irarrazaval 3400, Ñuñoa', 'Santiago', 'Ñuñoa', '+56955550600', '+56955550600', 'America/Santiago', true, '{"lun-vie":"09:30-19:30"}'::jsonb, 'Sede demo zona suroriente.')
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

    -- 2. Asociar servicios demo a las nuevas sucursales
    insert into aesthetic_service_location (id, business_id, service_id, location_id, active, price_override, duration_override_minutes)
    select gen_random_uuid(), s.business_id, s.id, bl.id, true, null, null
    from aesthetic_service s
    join business_location bl on bl.business_id = s.business_id
    where s.business_id = v_business_id
      and bl.code in ('las-condes', 'vitacura', 'nunoa')
    on conflict (business_id, service_id, location_id) do update
    set active = true,
        updated_at = current_timestamp;

    -- 3. Asociar profesionales demo a las nuevas sucursales
    insert into aesthetic_professional_location (id, business_id, professional_id, location_id, active)
    select gen_random_uuid(), ap.business_id, ap.id, bl.id, true
    from aesthetic_professional ap
    join business_location bl on bl.business_id = ap.business_id
    where ap.business_id = v_business_id
      and bl.code in ('las-condes', 'vitacura', 'nunoa')
    on conflict (business_id, professional_id, location_id) do update
    set active = true,
        updated_at = current_timestamp;

    -- 4. Horarios operativos por sucursal (lun-vie 09:00-19:00)
    insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
    select gen_random_uuid(), v_business_id, bl.id, d.day_of_week, '09:00'::time, '19:00'::time, true
    from business_location bl
    cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
    where bl.business_id = v_business_id
      and bl.code in ('las-condes', 'vitacura', 'nunoa')
    on conflict (business_id, location_id, day_of_week, start_time, end_time) do update
    set active = true,
        updated_at = current_timestamp;

    -- Sabado 10:00-14:00
    insert into agenda_business_hours (id, business_id, location_id, day_of_week, start_time, end_time, active)
    select gen_random_uuid(), v_business_id, bl.id, 6, '10:00'::time, '14:00'::time, true
    from business_location bl
    where bl.business_id = v_business_id
      and bl.code in ('las-condes', 'vitacura', 'nunoa')
    on conflict (business_id, location_id, day_of_week, start_time, end_time) do update
    set active = true,
        updated_at = current_timestamp;

    -- 5. Horarios de profesionales por sucursal
    for v_location in
        select bl.id, bl.code
        from business_location bl
        where bl.business_id = v_business_id
          and bl.code in ('las-condes', 'vitacura', 'nunoa')
    loop
        -- professional_location_schedule
        insert into professional_location_schedule (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
        select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '09:00'::time, '18:00'::time, true
        from aesthetic_professional_location apl
        cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
        where apl.business_id = v_business_id
          and apl.location_id = v_location.id
        on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do update
        set active = true,
            updated_at = current_timestamp;

        -- agenda_professional_hours
        insert into agenda_professional_hours (id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active)
        select gen_random_uuid(), apl.business_id, apl.professional_id, apl.location_id, d.day_of_week, '09:00'::time, '18:00'::time, true
        from aesthetic_professional_location apl
        cross join (values (1), (2), (3), (4), (5)) as d(day_of_week)
        where apl.business_id = v_business_id
          and apl.location_id = v_location.id
        on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do update
        set active = true,
            updated_at = current_timestamp;
    end loop;

end $$;
