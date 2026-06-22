-- Corrige extraccion de entidades para agenda por WhatsApp.
-- Objetivo: reconocer servicios faciales y dias de semana antes de pedir nuevamente el servicio.

insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza facial profunda', 'servicio_o_producto', 'Limpieza facial profunda', 390, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza facial', 'servicio_o_producto', 'Limpieza facial profunda', 380, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 360, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza rostro', 'servicio_o_producto', 'Limpieza facial profunda', 350, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'higiene facial', 'servicio_o_producto', 'Limpieza facial profunda', 340, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'tratamiento facial', 'servicio_o_producto', 'Limpieza facial profunda', 300, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'facial limpieza', 'servicio_o_producto', 'Limpieza facial profunda', 300, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'facial', 'servicio_o_producto', 'Limpieza facial profunda', 120, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'lunes', 'fecha_relativa', 'lunes', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'martes', 'fecha_relativa', 'martes', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'miercoles', 'fecha_relativa', 'miércoles', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'miércoles', 'fecha_relativa', 'miércoles', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'jueves', 'fecha_relativa', 'jueves', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'viernes', 'fecha_relativa', 'viernes', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'sabado', 'fecha_relativa', 'sábado', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'sábado', 'fecha_relativa', 'sábado', 130, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'domingo', 'fecha_relativa', 'domingo', 130, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = true,
    updated_at = current_timestamp;

update aesthetic_business_rule
set rule_payload = jsonb_set(
        coalesce(rule_payload, '{}'::jsonb),
        '{examples}',
        '["limpieza facial", "limpieza facial profunda", "depilación bozo", "rostro", "axilas", "piernas", "bikini"]'::jsonb,
        true
    ),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_BOOKING_MISSING_SERVICE_RESPONSE';
