-- V26: cierre transaccional agenda + enlace.
-- Refuerza reglas para que las solicitudes completas de reserva no queden en respuesta intermedia.

create extension if not exists pgcrypto;

insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_COMPLETE_TRANSACTIONAL_LINK_RESPONSE',
     'Agenda completa con reserva temporal y enlace', 'AI_RESPONSE',
     'Si el cliente entrega servicio, fecha, hora y sucursal, validar disponibilidad real, crear reserva temporal y devolver enlace de confirmacion. No pedir sucursal si ya fue entregada.',
     20, true,
     jsonb_build_object(
        'template', 'Si el cliente entrega servicio, fecha, hora y sucursal, validar disponibilidad real, crear reserva temporal y devolver enlace de confirmacion.',
        'requiredData', jsonb_build_array('serviceId', 'locationId', 'date', 'time'),
        'mustNotSay', jsonb_build_array('si falta sucursal', 'voy a validar sin crear reserva'),
        'expectedAction', 'create_temporary_booking_and_confirmation_link'
     )),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_MISSING_DATA_ONLY_RESPONSE',
     'Agenda pide solo dato faltante', 'AI_RESPONSE',
     'Si falta un dato de reserva, pedir solo ese dato faltante. Si estan servicio, fecha, hora y sucursal, no pedir datos adicionales.',
     19, true,
     jsonb_build_object(
        'template', 'Pedir solo el dato faltante de la reserva.',
        'missingService', 'Que servicio quieres reservar?',
        'missingLocation', 'En que sucursal prefieres atenderte?',
        'missingDate', 'Que dia te gustaria agendar?',
        'missingTime', 'A que hora prefieres asistir?'
     ))
on conflict (business_id, code) do update set
    name = excluded.name,
    rule_type = excluded.rule_type,
    description = excluded.description,
    priority = excluded.priority,
    active = true,
    rule_payload = excluded.rule_payload,
    updated_at = current_timestamp;

insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'en providencia', 'sede', 'Providencia', 620, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'sucursal providencia', 'sede', 'Providencia', 620, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'sede providencia', 'sede', 'Providencia', 620, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'provi', 'sede', 'Providencia', 500, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = true,
    updated_at = current_timestamp;

update aesthetic_business_rule
set description = 'Plantilla de respaldo; la agenda completa ahora debe intentar reserva temporal y enlace mediante flujo transaccional.',
    rule_payload = jsonb_build_object(
        'template', 'Perfecto. Tengo {service} para {date} a las {time}. Si ya estan servicio, sucursal, fecha y hora, debo crear una reserva temporal y entregar enlace de confirmacion; no debo quedarme solo en voy a validar.',
        'variables', jsonb_build_array('service','date','time','sucursal')
    ),
    priority = greatest(priority, 18),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_BOOKING_COMPLETE_RESPONSE';
