-- V46: Permisos canónicos completos y asignación a roles existentes
-- Añade los permisos granulares requeridos y los asigna a roles OWNER, ADMIN, SUPERVISOR, AGENT, SALES

-- Permisos canónicos
insert into permission (id, code, name, module_name, description)
values
    -- Dashboard
    ('40000000-0000-0000-0000-000000000001', 'DASHBOARD_VIEW', 'Ver dashboard', 'DASHBOARD', 'Permite ver el panel principal.'),
    
    -- Conversaciones
    ('40000000-0000-0000-0000-000000000002', 'CONVERSATIONS_VIEW', 'Ver conversaciones', 'CONVERSATIONS', 'Permite listar y abrir conversaciones.'),
    ('40000000-0000-0000-0000-000000000003', 'CONVERSATIONS_REPLY', 'Responder conversaciones', 'CONVERSATIONS', 'Permite enviar mensajes salientes.'),
    ('40000000-0000-0000-0000-000000000004', 'CONVERSATIONS_ASSIGN', 'Asignar conversaciones', 'CONVERSATIONS', 'Permite asignar conversaciones a usuarios.'),
    
    -- Agenda y Reservas
    ('40000000-0000-0000-0000-000000000005', 'AGENDA_VIEW', 'Ver agenda', 'AGENDA', 'Permite ver la agenda y disponibilidad.'),
    ('40000000-0000-0000-0000-000000000006', 'BOOKINGS_CREATE', 'Crear reservas', 'BOOKINGS', 'Permite crear nuevas reservas.'),
    ('40000000-0000-0000-0000-000000000007', 'BOOKINGS_UPDATE', 'Actualizar reservas', 'BOOKINGS', 'Permite editar reservas existentes.'),
    ('40000000-0000-0000-0000-000000000008', 'BOOKINGS_CANCEL', 'Cancelar reservas', 'BOOKINGS', 'Permite cancelar reservas.'),
    ('40000000-0000-0000-0000-000000000009', 'BOOKINGS_RESCHEDULE', 'Reprogramar reservas', 'BOOKINGS', 'Permite reprogramar reservas.'),
    
    -- Catálogo
    ('40000000-0000-0000-0000-000000000010', 'CATALOG_VIEW', 'Ver catálogo', 'CATALOG', 'Permite ver productos y servicios.'),
    ('40000000-0000-0000-0000-000000000011', 'CATALOG_MANAGE', 'Gestionar catálogo', 'CATALOG', 'Permite crear, editar y eliminar productos/servicios.'),
    
    -- Sedes
    ('40000000-0000-0000-0000-000000000012', 'LOCATIONS_VIEW', 'Ver sedes', 'LOCATIONS', 'Permite ver sedes y configuración.'),
    ('40000000-0000-0000-0000-000000000013', 'LOCATIONS_MANAGE', 'Gestionar sedes', 'LOCATIONS', 'Permite crear, editar y eliminar sedes.'),
    
    -- Usuarios
    ('40000000-0000-0000-0000-000000000014', 'USERS_VIEW', 'Ver usuarios', 'USERS', 'Permite listar usuarios.'),
    ('40000000-0000-0000-0000-000000000015', 'USERS_MANAGE', 'Gestionar usuarios', 'USERS', 'Permite crear, editar y desactivar usuarios.'),
    
    -- Seguridad y Auditoría
    ('40000000-0000-0000-0000-000000000016', 'SECURITY_AUDIT_VIEW', 'Ver auditoría/seguridad', 'SECURITY', 'Permite ver logs de auditoría y configuración de seguridad.'),
    
    -- WhatsApp
    ('40000000-0000-0000-0000-000000000017', 'WHATSAPP_CONFIG_VIEW', 'Ver configuración WhatsApp', 'WHATSAPP', 'Permite ver estado y configuración de WhatsApp.'),
    ('40000000-0000-0000-0000-000000000018', 'WHATSAPP_CONFIG_MANAGE', 'Gestionar configuración WhatsApp', 'WHATSAPP', 'Permite conectar, desconectar y configurar WhatsApp.'),
    
    -- Reportes
    ('40000000-0000-0000-0000-000000000019', 'REPORTS_VIEW', 'Ver reportes', 'REPORTS', 'Permite consultar reportes y métricas.'),

    -- Calendario
    ('40000000-0000-0000-0000-000000000020', 'CALENDAR_CONFIG_VIEW', 'Ver configuraci\u00f3n de calendario', 'CALENDAR', 'Permite ver el estado y configuraci\u00f3n de la integraci\u00f3n de calendario.'),
    ('40000000-0000-0000-0000-000000000021', 'CALENDAR_CONFIG_MANAGE', 'Gestionar configuraci\u00f3n de calendario', 'CALENDAR', 'Permite conectar, desconectar y configurar el calendario.')
on conflict (code) do nothing;

-- Asignación de permisos a roles
-- OWNER: todos los permisos
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
cross join permission p
where r.code = 'OWNER'
on conflict (role_id, permission_id) do nothing;

-- ADMIN: permisos operativos principales (sin USER_MANAGE, SECURITY_AUDIT_VIEW, WHATSAPP_CONFIG_MANAGE)
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in (
    'DASHBOARD_VIEW',
    'CONVERSATIONS_VIEW',
    'CONVERSATIONS_REPLY',
    'CONVERSATIONS_ASSIGN',
    'AGENDA_VIEW',
    'BOOKINGS_CREATE',
    'BOOKINGS_UPDATE',
    'BOOKINGS_CANCEL',
    'BOOKINGS_RESCHEDULE',
    'CATALOG_VIEW',
    'CATALOG_MANAGE',
    'LOCATIONS_VIEW',
    'LOCATIONS_MANAGE',
    'USERS_VIEW',
    'WHATSAPP_CONFIG_VIEW',
    'REPORTS_VIEW',
    'CALENDAR_CONFIG_VIEW',
    'CALENDAR_CONFIG_MANAGE'
)
where r.code = 'ADMIN'
on conflict (role_id, permission_id) do nothing;

-- SUPERVISOR: permisos de visualización y operativos (sin gestión de usuarios, seguridad, WhatsApp config)
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in (
    'DASHBOARD_VIEW',
    'CONVERSATIONS_VIEW',
    'CONVERSATIONS_REPLY',
    'CONVERSATIONS_ASSIGN',
    'AGENDA_VIEW',
    'BOOKINGS_CREATE',
    'BOOKINGS_UPDATE',
    'BOOKINGS_CANCEL',
    'BOOKINGS_RESCHEDULE',
    'CATALOG_VIEW',
    'LOCATIONS_VIEW',
    'USERS_VIEW',
    'REPORTS_VIEW',
    'CALENDAR_CONFIG_VIEW'
)
where r.code = 'SUPERVISOR'
on conflict (role_id, permission_id) do nothing;

-- AGENT: permisos operativos básicos
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in (
    'DASHBOARD_VIEW',
    'CONVERSATIONS_VIEW',
    'CONVERSATIONS_REPLY',
    'AGENDA_VIEW',
    'BOOKINGS_CREATE',
    'BOOKINGS_UPDATE',
    'BOOKINGS_CANCEL',
    'BOOKINGS_RESCHEDULE',
    'CATALOG_VIEW',
    'REPORTS_VIEW'
)
where r.code = 'AGENT'
on conflict (role_id, permission_id) do nothing;

-- SALES: permisos orientados a ventas y prospectos
insert into role_permission (id, role_id, permission_id)
select
    gen_random_uuid(),
    r.id,
    p.id
from role r
join permission p on p.code in (
    'DASHBOARD_VIEW',
    'CONVERSATIONS_VIEW',
    'CONVERSATIONS_REPLY',
    'AGENDA_VIEW',
    'BOOKINGS_CREATE',
    'BOOKINGS_UPDATE',
    'BOOKINGS_CANCEL',
    'CATALOG_VIEW',
    'REPORTS_VIEW'
)
where r.code = 'SALES'
on conflict (role_id, permission_id) do nothing;

-- Mantener permisos existentes de V3 para compatibilidad (no eliminar)
-- Los permisos existentes en V3 se mapean a los nuevos canónicos
-- DASHBOBBARD_VIEW ya existe
-- NOTIFICATION_VIEW -> mantener
-- CONVERSATION_VIEW -> CONVERSATIONS_VIEW
-- CONVERSATION_REPLY -> CONVERSATIONS_REPLY
-- TEMPLATE_MANAGE -> mantener
-- LEAD_MANAGE -> mantener
-- BOOKING_MANAGE -> BOOKINGS_CREATE + BOOKINGS_UPDATE + BOOKINGS_RESCHEDULE
-- ORDER_MANAGE -> mantener
-- CATALOG_MANAGE ya existe
-- AUTOMATION_MANAGE -> mantener
-- REPORT_VIEW -> REPORTS_VIEW
-- ADMIN_MANAGE -> mantener
-- USER_MANAGE -> USERS_MANAGE
-- SECURITY_MANAGE -> SECURITY_AUDIT_VIEW
-- WHATSAPP_WEB_MANAGE -> WHATSAPP_CONFIG_MANAGE