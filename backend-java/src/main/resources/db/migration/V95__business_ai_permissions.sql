-- V95: Permisos BUSINESS_AI_* para configuración, prueba, auditoría y envío de IA

insert into permission (id, code, name, module_name, description)
values
    ('95000000-0000-0000-0000-000000000001', 'BUSINESS_AI_VIEW', 'Ver configuración IA', 'BUSINESS_AI', 'Permite ver el panel de IA del negocio y su resumen.'),
    ('95000000-0000-0000-0000-000000000002', 'BUSINESS_AI_MANAGE', 'Gestionar configuración IA', 'BUSINESS_AI', 'Permite modificar la configuración general y avanzada de IA.'),
    ('95000000-0000-0000-0000-000000000003', 'BUSINESS_AI_TEST', 'Probar asistente IA', 'BUSINESS_AI', 'Permite usar el simulador y probar respuestas del asistente.'),
    ('95000000-0000-0000-0000-000000000004', 'BUSINESS_AI_AUDIT_VIEW', 'Ver auditoría IA', 'BUSINESS_AI', 'Permite consultar el historial de respuestas y decisiones de IA.'),
    ('95000000-0000-0000-0000-000000000005', 'BUSINESS_AI_REVIEW', 'Revisar respuestas IA', 'BUSINESS_AI', 'Permite aprobar, rechazar o corregir respuestas generadas por IA.'),
    ('95000000-0000-0000-0000-000000000006', 'BUSINESS_AI_SEND', 'Enviar respuestas IA', 'BUSINESS_AI', 'Permite enviar respuestas generadas por IA a conversaciones reales.')
on conflict (code) do nothing;

-- OWNER: todos los permisos BUSINESS_AI
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
cross join permission p
where r.code = 'OWNER'
  and p.code like 'BUSINESS_AI\_%'
on conflict (role_id, permission_id) do nothing;

-- ADMIN: todos los permisos BUSINESS_AI
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
cross join permission p
where r.code = 'ADMIN'
  and p.code like 'BUSINESS_AI\_%'
on conflict (role_id, permission_id) do nothing;

-- SUPERVISOR: VIEW, TEST, AUDIT_VIEW, REVIEW
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in ('BUSINESS_AI_VIEW', 'BUSINESS_AI_TEST', 'BUSINESS_AI_AUDIT_VIEW', 'BUSINESS_AI_REVIEW')
where r.code = 'SUPERVISOR'
on conflict (role_id, permission_id) do nothing;

-- AGENT: VIEW, TEST
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in ('BUSINESS_AI_VIEW', 'BUSINESS_AI_TEST')
where r.code = 'AGENT'
on conflict (role_id, permission_id) do nothing;

-- SALES: TEST (opcional)
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code = 'BUSINESS_AI_TEST'
where r.code = 'SALES'
on conflict (role_id, permission_id) do nothing;
