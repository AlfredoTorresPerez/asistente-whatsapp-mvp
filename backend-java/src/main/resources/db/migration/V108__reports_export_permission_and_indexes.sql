insert into permission (id, code, name, module_name, description)
values (
    gen_random_uuid(),
    'REPORTS_EXPORT',
    'Exportar reportes',
    'Reportes',
    'Permite descargar reportes respetando filtros y alcance de acceso.'
)
on conflict (code) do update
set name = excluded.name,
    module_name = excluded.module_name,
    description = excluded.description,
    active = true,
    updated_at = current_timestamp;

insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r
join permission p on p.code = 'REPORTS_EXPORT'
where r.code in ('OWNER', 'ADMIN')
on conflict (role_id, permission_id) do nothing;

create index if not exists idx_booking_reports_business_starts
    on booking (business_id, starts_at desc);

create index if not exists idx_booking_reports_location_starts
    on booking (business_id, location_id, starts_at desc)
    where location_id is not null;

create index if not exists idx_booking_reports_professional_starts
    on booking (business_id, professional_id, starts_at desc)
    where professional_id is not null;

create index if not exists idx_booking_reports_room_starts
    on booking (business_id, room_id, starts_at desc)
    where room_id is not null;

create index if not exists idx_booking_reports_service_starts
    on booking (business_id, service_id, starts_at desc)
    where service_id is not null;

create index if not exists idx_booking_status_history_reports
    on booking_status_history (business_id, new_status, created_at desc);

create index if not exists idx_conversation_reports_status_created
    on conversation (business_id, status, created_at desc);

create index if not exists idx_lead_reports_stage_created
    on lead (business_id, stage, created_at desc)
    where active = true;

create index if not exists idx_notification_reports_pending
    on notification (business_id, status, created_at desc)
    where status = 'UNREAD';

create index if not exists idx_audit_log_reports_entity_date
    on audit_log (business_id, entity_type, entity_id, occurred_at desc);
