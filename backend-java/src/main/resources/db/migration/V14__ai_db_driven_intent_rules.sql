create table if not exists ai_entity_alias (
    id uuid primary key,
    business_id uuid not null,
    alias varchar(180) not null,
    entity_key varchar(80) not null,
    entity_value varchar(180) not null,
    priority integer not null default 100,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_ai_entity_alias_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_ai_entity_alias_business_key_alias
        unique (business_id, entity_key, alias),
    constraint chk_ai_entity_alias_priority
        check (priority between 1 and 999)
);

create index if not exists idx_ai_entity_alias_business_active
    on ai_entity_alias (business_id, active, priority desc);

insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values
    ('78000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'AI_DEPILATION_CATALOG_RESPONSE', 'Respuesta catalogo depilacion desde BD', 'AI_RESPONSE', 'Plantilla usada para listar servicios de depilacion desde configuracion de negocio.', 10, true, '{"template":"Tenemos {services}. ¿Cuál quieres revisar?","labels":["depilación bozo","rostro","axilas","cera","láser"]}'::jsonb),
    ('78000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'AI_PRICE_KNOWN_SERVICE_RESPONSE', 'Respuesta precio servicio conocido desde BD', 'AI_RESPONSE', 'Plantilla usada cuando el servicio existe en catalogo.', 11, true, '{"template":"El valor base de {service} es {price} y dura aproximadamente {duration} minutos. ¿Quieres agendar una hora?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'AI_PRICE_UNKNOWN_SERVICE_RESPONSE', 'Respuesta precio servicio ambiguo desde BD', 'AI_RESPONSE', 'Plantilla usada cuando falta precisar el servicio para cotizar.', 12, true, '{"template":"Para darte un precio correcto, ¿me indicas el servicio exacto que quieres revisar?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'AI_QUOTE_MISSING_DETAIL_RESPONSE', 'Respuesta cotizacion con detalle faltante desde BD', 'AI_RESPONSE', 'Plantilla usada para cotizaciones donde falta zona o modalidad.', 13, true, '{"template":"Puedo ayudarte con la cotización de {category}. ¿Qué zona quieres cotizar: {options}?","options":["rostro","axilas","u otra"]}'::jsonb),
    ('78000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_MISSING_SERVICE_RESPONSE', 'Agenda pide servicio desde BD', 'AI_RESPONSE', 'Plantilla usada cuando falta servicio para agendar.', 20, true, '{"template":"Perfecto. Para revisar disponibilidad necesito el servicio específico. Por ejemplo: {examples}.","examples":["depilación bozo","rostro","axilas","piernas","bikini"]}'::jsonb),
    ('78000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_MISSING_DATE_RESPONSE', 'Agenda pide fecha desde BD', 'AI_RESPONSE', 'Plantilla usada cuando falta fecha para agendar.', 21, true, '{"template":"Perfecto, reviso {service}. ¿Para qué día quieres agendar?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_MISSING_TIME_RESPONSE', 'Agenda pide hora desde BD', 'AI_RESPONSE', 'Plantilla usada cuando falta hora para agendar.', 22, true, '{"template":"Perfecto, reviso {service} para {date}. ¿Qué horario te acomoda?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_COMPLETE_RESPONSE', 'Agenda completa desde BD', 'AI_RESPONSE', 'Plantilla usada cuando existen servicio, fecha y hora.', 23, true, '{"template":"Perfecto. Tengo {service} para {date} a las {time}. Debo validar disponibilidad real en agenda antes de confirmar. ¿Quieres que revise esa hora?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_CHANGE_IDENTIFY_RESPONSE', 'Cambio de cita identifica reserva desde BD', 'AI_RESPONSE', 'Plantilla usada para cambios de cita existentes.', 24, true, '{"template":"Claro. Para ayudarte a cambiar la hora, ¿me indicas tu nombre, correo o la fecha de la cita actual?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_CANCEL_IDENTIFY_RESPONSE', 'Cancelacion identifica reserva desde BD', 'AI_RESPONSE', 'Plantilla usada para cancelar cita.', 25, true, '{"template":"Claro. Para revisar tu cancelación, ¿me indicas el nombre o número asociado a la cita?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_STATUS_IDENTIFY_RESPONSE', 'Estado de agenda identifica fecha desde BD', 'AI_RESPONSE', 'Plantilla usada para confirmar reserva.', 26, true, '{"template":"Puedo ayudarte a revisar tus reservas, pero debo validarlo en agenda. ¿Qué fecha o mes quieres revisar?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'AI_PAYMENT_REQUEST_AMOUNT_RESPONSE', 'Pago con solicitud y monto desde BD', 'PAYMENT', 'Plantilla usada cuando el cliente informa solicitud y monto.', 30, true, '{"template":"Perfecto. Tengo la solicitud {requestNumber} por {amount}. ¿Qué método de pago quieres usar?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', 'AI_PAYMENT_MISSING_AMOUNT_RESPONSE', 'Pago pide monto desde BD', 'PAYMENT', 'Plantilla usada cuando falta monto.', 31, true, '{"template":"Gracias. Tengo la solicitud {requestNumber}. ¿Me indicas el monto y método de pago?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', 'AI_PAYMENT_MISSING_REQUEST_RESPONSE', 'Pago pide solicitud desde BD', 'PAYMENT', 'Plantilla usada cuando falta identificador de pago.', 32, true, '{"template":"Gracias. Para revisar el pago, ¿me indicas el número de pedido o solicitud?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000015', '11111111-1111-1111-1111-111111111111', 'AI_SALES_MISSING_SERVICE_RESPONSE', 'Venta pide servicio desde BD', 'AI_RESPONSE', 'Plantilla usada cuando falta servicio o producto.', 40, true, '{"template":"Perfecto, puedo ayudarte. ¿Qué producto o servicio estás buscando exactamente?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', 'AI_SALES_NEXT_STEP_RESPONSE', 'Venta siguiente paso desde BD', 'AI_RESPONSE', 'Plantilla usada para venta general con servicio identificado.', 41, true, '{"template":"Puedo orientarte con {service}. ¿Quieres revisar precio, características o agendar una atención?"}'::jsonb),
    ('78000000-0000-0000-0000-000000000017', '11111111-1111-1111-1111-111111111111', 'AI_GENERIC_NEXT_STEP', 'Respuesta generica desde BD', 'AI_RESPONSE', 'Plantilla generica de respaldo.', 999, true, '{"template":"¿Qué necesitas revisar hoy?"}'::jsonb)
on conflict (business_id, code) do update set
    name = excluded.name,
    rule_type = excluded.rule_type,
    description = excluded.description,
    priority = excluded.priority,
    active = excluded.active,
    rule_payload = excluded.rule_payload,
    updated_at = current_timestamp;

insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    ('79000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'depilacion bozo', 'servicio_o_producto', 'Depilacion bozo', 300, true),
    ('79000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'bozo', 'servicio_o_producto', 'Depilacion bozo', 290, true),
    ('79000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'depilacion axilas', 'servicio_o_producto', 'Depilacion axilas', 280, true),
    ('79000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'axilas', 'servicio_o_producto', 'Depilacion axilas', 270, true),
    ('79000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'depilacion rostro', 'servicio_o_producto', 'Depilacion rostro', 260, true),
    ('79000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'depilacion facial', 'servicio_o_producto', 'Depilacion rostro', 250, true),
    ('79000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'depilacion laser', 'categoria_servicio', 'depilación láser', 240, true),
    ('79000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'depilacion con cera', 'servicio_o_producto', 'Depilacion con cera', 230, true),
    ('79000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'cera', 'servicio_o_producto', 'Depilacion con cera', 220, true),
    ('79000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'depilacion piernas', 'servicio_o_producto', 'Depilacion piernas', 210, true),
    ('79000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'depilacion bikini', 'servicio_o_producto', 'Depilacion bikini', 200, true),
    ('79000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'manana', 'fecha_relativa', 'mañana', 100, true),
    ('79000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', 'hoy', 'fecha_relativa', 'hoy', 100, true),
    ('79000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', 'esta semana', 'fecha_relativa', 'esta semana', 100, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = excluded.active,
    updated_at = current_timestamp;
