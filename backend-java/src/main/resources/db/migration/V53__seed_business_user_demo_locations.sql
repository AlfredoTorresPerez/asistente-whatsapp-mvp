-- V53: Asigna ubicacion por defecto a usuarios demo usando la primera sede activa.
-- V52 buscaba la sede 'principal', que puede estar inactiva en datos demo multisedes.

do $$
declare
    v_business_id constant uuid := '11111111-1111-1111-1111-111111111111';
    v_location_id uuid;
begin
    select id into v_location_id
    from business_location
    where business_id = v_business_id and active = true
    order by name
    limit 1;

    if v_location_id is not null then
        insert into business_user (id, business_id, user_id, location_id, active)
        select gen_random_uuid(), v_business_id, u.id, v_location_id, true
        from user_account u
        where u.business_id = v_business_id
          and u.email in ('admin@demo.cl', 'agente@demo.cl', 'supervisor@demo.cl', 'admin2@demo.cl')
        on conflict do nothing;
    end if;
end $$;
