-- V25 - Ajustes V23.3 de confianza y evaluación
-- Objetivo:
-- 1) Mantener la priorización de "limpieza de rostro" como Limpieza facial profunda.
-- 2) Registrar regla de calidad para elevar confianza cuando un sinónimo facial fuerte fue resuelto.
-- 3) Documentar normalización de acentos en pruebas automáticas.

insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 520, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza rostro', 'servicio_o_producto', 'Limpieza facial profunda', 510, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza de cutis', 'servicio_o_producto', 'Limpieza facial profunda', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza cutis', 'servicio_o_producto', 'Limpieza facial profunda', 490, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'higiene de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 480, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = true,
    updated_at = current_timestamp;

insert into aesthetic_business_rule (
    id,
    business_id,
    code,
    name,
    rule_type,
    description,
    priority,
    active,
    rule_payload
)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'AI_CONFIDENCE_FACIAL_ALIAS_STRONG_MATCH',
    'Confianza alta para sinónimos faciales fuertes',
    'AI_RESPONSE',
    'Cuando el mensaje del cliente incluya limpieza de rostro, limpieza de cutis, higiene de rostro o limpieza rostro y el servicio resuelto sea Limpieza facial profunda, la intención de agenda debe conservar confianza alta, equivalente a una coincidencia directa de servicio.',
    33,
    true,
    jsonb_build_object(
        'module', 'intent-confidence',
        'strongAliases', jsonb_build_array('limpieza de rostro', 'limpieza rostro', 'limpieza de cutis', 'limpieza cutis', 'higiene de rostro'),
        'resolvedService', 'Limpieza facial profunda',
        'minimumConfidence', 0.88,
        'reason', 'Evitar que una respuesta coherente quede marcada como baja confianza cuando el extractor ya resolvió el servicio facial correctamente.'
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

insert into aesthetic_business_rule (
    id,
    business_id,
    code,
    name,
    rule_type,
    description,
    priority,
    active,
    rule_payload
)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'QA_TEST_NORMALIZAR_ACENTOS_Y_UTF8',
    'Normalización de acentos y UTF-8 en pruebas IA',
    'AI_RESPONSE',
    'Los scripts de prueba deben comparar respuestas normalizando acentos y reparando mojibake UTF-8 para evitar falsos negativos entre día/dia, señal/senal y qué/que.',
    34,
    true,
    jsonb_build_object(
        'module', 'qa-test',
        'normalizeAccents', true,
        'repairUtf8Mojibake', true,
        'examples', jsonb_build_array('día = dia', 'señal = senal', 'qué = que')
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
