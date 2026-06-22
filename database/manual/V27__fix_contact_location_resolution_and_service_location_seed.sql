-- V27: Corrige resolución de sucursal para contactos sin sede y refuerza datos demo.
-- No elimina datos existentes. Todas las inserciones son idempotentes.

insert into business_location (
    id, business_id, code, name, address, city, commune, phone, whatsapp_number, timezone, active
)
values (
    '81000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'providencia',
    'Providencia',
    'Av. Providencia 2450, Santiago',
    'Santiago',
    'Providencia',
    '+56955550100',
    '+56955550100',
    'America/Santiago',
    true
)
on conflict (business_id, code) do update
set name = excluded.name,
    address = excluded.address,
    city = excluded.city,
    commune = excluded.commune,
    phone = excluded.phone,
    whatsapp_number = excluded.whatsapp_number,
    timezone = excluded.timezone,
    active = true,
    updated_at = current_timestamp;

insert into aesthetic_service_location (id, business_id, service_id, location_id, active, price_override, duration_override_minutes)
select gen_random_uuid(), s.business_id, s.id, bl.id, true, null, null
from aesthetic_service s
join business_location bl on bl.business_id = s.business_id
where s.business_id = '11111111-1111-1111-1111-111111111111'
  and bl.code = 'providencia'
  and lower(replace(replace(s.name, 'ó', 'o'), 'Ó', 'O')) in ('limpieza facial profunda', 'depilacion bozo')
on conflict (business_id, service_id, location_id) do update
set active = true,
    updated_at = current_timestamp;

insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values
    (
        gen_random_uuid(),
        '11111111-1111-1111-1111-111111111111',
        'AI_V2342_LOCATION_PRIORITY_MESSAGE_TEXT',
        'Prioridad de sucursal escrita por cliente',
        'AI_RESPONSE',
        'La sucursal mencionada explícitamente en el mensaje del cliente debe tener prioridad sobre la sucursal de conversación o contacto.',
        3,
        true,
        jsonb_build_object('version', '23.4.2', 'source', 'MESSAGE_TEXT', 'aliases', jsonb_build_array('Providencia', 'en Providencia', 'sucursal Providencia', 'sede Providencia', 'Provi'))
    ),
    (
        gen_random_uuid(),
        '11111111-1111-1111-1111-111111111111',
        'AI_V2342_SERVICE_LOCATION_ERROR_CONTROLLED',
        'Respuesta funcional para servicio no disponible en sucursal',
        'AI_RESPONSE',
        'Si un servicio no está configurado para una sucursal, responder de forma conversacional y no devolver error tecnico 404.',
        4,
        true,
        jsonb_build_object('version', '23.4.2', 'mustNotReturn', 404)
    )
on conflict (business_id, code) do update
set name = excluded.name,
    rule_type = excluded.rule_type,
    description = excluded.description,
    priority = excluded.priority,
    active = true,
    rule_payload = excluded.rule_payload,
    updated_at = current_timestamp;
