-- V24: Ajustes V23.2 para coherencia conversacional de agenda y pruebas.
-- Objetivo:
-- 1) reforzar que "limpieza de rostro" se resuelva como Limpieza facial profunda;
-- 2) documentar que la sucursal explicita debe conservarse en respuestas parciales;
-- 3) mantener trazabilidad de correccion V23.2.

insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 450, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza rostro', 'servicio_o_producto', 'Limpieza facial profunda', 440, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'higiene de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 430, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza de cutis', 'servicio_o_producto', 'Limpieza facial profunda', 420, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza cutis', 'servicio_o_producto', 'Limpieza facial profunda', 420, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = true,
    updated_at = current_timestamp;

insert into aesthetic_business_rule (
    id, business_id, code, name, rule_type, description, priority, active, rule_payload
)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'AI_V232_PRIORIDAD_LIMPIEZA_ROSTRO',
    'Priorizar limpieza de rostro como servicio facial',
    'AI_RESPONSE',
    'Cuando el cliente diga limpieza de rostro, limpieza rostro, higiene de rostro o limpieza de cutis, debe interpretarse como Limpieza facial profunda y nunca como Depilacion rostro.',
    2,
    true,
    jsonb_build_object(
        'version', '23.2',
        'module', 'entity-extraction',
        'expectedService', 'Limpieza facial profunda',
        'blockedService', 'Depilacion rostro'
    )
), (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'AI_V232_CONSERVAR_SUCURSAL_EN_RESPUESTA',
    'Conservar sucursal indicada en respuesta parcial de agenda',
    'BOOKING',
    'Si el cliente ya indico sucursal, la respuesta de agenda debe repetir esa sucursal y no pedirla nuevamente. Ejemplo: Puedo ayudarte a reservar limpieza facial profunda en Providencia. Que dia te gustaria agendar?',
    3,
    true,
    jsonb_build_object(
        'version', '23.2',
        'module', 'booking-agent',
        'mustPreserveExplicitLocation', true
    )
)
on conflict (business_id, code) do update set
    name = excluded.name,
    rule_type = excluded.rule_type,
    description = excluded.description,
    priority = excluded.priority,
    active = true,
    rule_payload = excluded.rule_payload,
    updated_at = current_timestamp;
