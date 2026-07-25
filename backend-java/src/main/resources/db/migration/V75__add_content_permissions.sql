-- V75: Agrega permisos CONTENT_VIEW y CONTENT_MANAGE para gestión de contenido visual

insert into permission (id, code, name, module_name, description)
values
    ('40000000-0000-0000-0000-000000000022', 'CONTENT_VIEW', 'Ver contenido visual', 'CONTENT', 'Permite listar y ver contenido visual (hero, servicios, categorías).'),
    ('40000000-0000-0000-0000-000000000023', 'CONTENT_MANAGE', 'Gestionar contenido visual', 'CONTENT', 'Permite crear, editar y eliminar contenido visual.')
on conflict (code) do nothing;

-- OWNER: ambos permisos (el cross join de V46 no cubre permisos agregados después)
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in ('CONTENT_VIEW', 'CONTENT_MANAGE')
where r.code = 'OWNER'
on conflict (role_id, permission_id) do nothing;

-- ADMIN: ambos permisos
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in ('CONTENT_VIEW', 'CONTENT_MANAGE')
where r.code = 'ADMIN'
on conflict (role_id, permission_id) do nothing;

-- SUPERVISOR: solo CONTENT_VIEW
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code = 'CONTENT_VIEW'
where r.code = 'SUPERVISOR'
on conflict (role_id, permission_id) do nothing;

-- AGENT: solo CONTENT_VIEW
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code = 'CONTENT_VIEW'
where r.code = 'AGENT'
on conflict (role_id, permission_id) do nothing;
