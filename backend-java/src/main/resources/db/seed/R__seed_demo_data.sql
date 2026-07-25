-- R__seed_demo_data.sql
-- Repeatable migration: seed demo data for local/demo environments.
-- Uses ON CONFLICT DO NOTHING for idempotent re-runs.

-- Business
insert into business (id, code, company_name, business_name, timezone, currency, contact_email, support_phone, address, active)
values ('11111111-1111-1111-1111-111111111111', 'centro-estetico-bella', 'Centro Estetico Bella SpA', 'Centro Estetico Bella', 'America/Santiago', 'CLP', 'admin@demo.cl', '+56955550100', 'Av. Providencia 2450, Santiago', true)
on conflict (code) do nothing;

-- Security policy
insert into security_policy (id, business_id, session_timeout_minutes, password_min_length, require_uppercase, require_number, require_symbol, max_failed_login_attempts)
values ('11111111-1111-1111-1111-111111111112', '11111111-1111-1111-1111-111111111111', 30, 8, true, true, false, 5)
on conflict (business_id) do nothing;

-- User accounts
insert into user_account (id, business_id, first_name, last_name, email, phone, password_hash, timezone, status, last_login_at, failed_login_attempts)
values ('40000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Carla', 'Mendez', 'admin@demo.cl', '+56955550101', '$2a$10$n7vTmgWhJDL9XDuOn9e5ve6NAhXH4zP6WtU0b7ib/KcN7/TfIz0Gi', 'America/Santiago', 'ACTIVE', '2026-06-25T14:00:00Z', 0)
on conflict (business_id, email) do nothing;

-- User roles
insert into user_role (id, business_id, user_id, role_id)
values ('50000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001')
on conflict (user_id, role_id) do nothing;

-- Product categories
insert into product_category (id, business_id, code, name, description, active)
values ('60000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'FACIAL', 'Faciales', 'Servicios de limpieza e hidratacion facial.', true),
       ('60000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'LASER', 'Depilacion laser', 'Servicios de depilacion laser por zona.', true)
on conflict (business_id, code) do nothing;

-- Product services
insert into product_service (id, business_id, category_id, type, name, sku, description, price, duration_minutes, active)
values ('61000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '60000000-0000-0000-0000-000000000001', 'SERVICE', 'Limpieza Facial Profunda', 'FAC-001', 'Servicio de limpieza profunda con extraccion y mascarilla.', 34990.00, 60, true),
       ('61000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '60000000-0000-0000-0000-000000000001', 'SERVICE', 'Facial Hidratante Express', 'FAC-002', 'Tratamiento express para hidratacion y luminosidad.', 24990.00, 45, true),
       ('61000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', '60000000-0000-0000-0000-000000000002', 'SERVICE', 'Depilacion Laser Axilas', 'LAS-001', 'Sesion individual de depilacion laser para axilas.', 19990.00, 30, true)
on conflict (business_id, sku) do nothing;

-- Response templates
insert into response_template (id, business_id, name, category, body, active)
values ('62000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Seguimiento 24h', 'FOLLOW_UP', 'Hola {{customer_name}}, gracias por escribir a Centro Estetico Bella. Si quieres, te ayudo a reservar tu evaluacion.', true),
       ('62000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Recordatorio de cita', 'REMINDER', 'Hola {{customer_name}}, te recordamos tu cita de manana en Centro Estetico Bella.', true)
on conflict (business_id, name) do nothing;

-- Customers
insert into customer (id, business_id, first_name, last_name, display_name, phone, normalized_phone, email, active)
values ('63000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Sofia', 'Rojas', 'Sofia Rojas', '+56911112222', '+56911112222', 'sofia@demo.cl', true),
       ('63000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Paula', 'Diaz', 'Paula Diaz', '+56933334444', '+56933334444', 'paula@demo.cl', true),
       ('63000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Camila', 'Torres', 'Camila Torres', '+56977778888', '+56977778888', null, true)
on conflict (business_id, normalized_phone) do nothing;

-- Conversations
insert into conversation (id, business_id, channel_account_id, customer_id, assigned_user_id, channel_type, customer_name, customer_phone, status, unread_count, last_message_at, last_message_preview, opened_at, closed_at)
values ('64000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '69500000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'WHATSAPP', 'Sofia Rojas', '+56911112222', 'OPEN', 1, '2026-06-25T16:00:00Z', 'Quiero saber el precio de la limpieza facial.', '2026-06-25T15:50:00Z', null),
       ('64000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '69500000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 'WHATSAPP', 'Camila Torres', '+56977778888', 'PENDING', 0, '2026-06-24T16:30:00Z', 'Gracias, te contacto manana.', '2026-06-24T16:00:00Z', null)
on conflict (id) do nothing;

-- Messages
insert into message (id, business_id, conversation_id, sent_by_user_id, direction, message_type, body, status, external_message_id, provider_event_id, sent_at, received_at, failed_at, error_code)
values ('65000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '64000000-0000-0000-0000-000000000001', null, 'INBOUND', 'TEXT', 'Hola, quiero saber el precio de la limpieza facial.', 'RECEIVED', null, 'evt-demo-1001', null, '2026-06-25T15:55:00Z', null, null),
       ('65000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '64000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'OUTBOUND', 'TEXT', 'Hola Sofia, la limpieza facial profunda tiene un valor de $34.990. Si quieres, te ayudo a reservar.', 'DELIVERED', 'whatsapp-web-msg-1001', 'ack-demo-1001', '2026-06-25T16:00:00Z', null, null, null)
on conflict (id) do nothing;

-- Leads
insert into lead (id, business_id, customer_id, conversation_id, source_type, first_name, last_name, phone, normalized_phone, email, stage, notes, assigned_user_id, active)
values ('66000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '63000000-0000-0000-0000-000000000001', '64000000-0000-0000-0000-000000000001', 'CONVERSATION', 'Sofia', 'Rojas', '+56911112222', '+56911112222', 'sofia@demo.cl', 'INTERESTED', 'Interesada en reservar una limpieza facial para la proxima semana.', '40000000-0000-0000-0000-000000000001', true),
       ('66000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '63000000-0000-0000-0000-000000000002', null, 'MANUAL', 'Paula', 'Diaz', '+56933334444', '+56933334444', 'paula@demo.cl', 'NEW', 'Pidio informacion por depilacion laser.', '40000000-0000-0000-0000-000000000001', true)
on conflict (id) do nothing;

-- Lead notes
insert into lead_note (id, business_id, lead_id, author_user_id, note_text)
values ('67000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '66000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Cliente con alta intencion de compra. Prefiere horario tarde.')
on conflict (id) do nothing;

-- Bookings
insert into booking (id, business_id, customer_id, lead_id, conversation_id, assigned_user_id, subject, status, starts_at, ends_at, duration_minutes, location, notes, completed_at)
values ('68000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '63000000-0000-0000-0000-000000000001', '66000000-0000-0000-0000-000000000001', '64000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Evaluacion facial inicial', 'CONFIRMADA', '2026-07-20T14:00:00Z', '2026-07-20T14:45:00Z', 45, 'Sucursal Providencia', 'Confirmar por WhatsApp una hora antes.', null),
       ('68000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '63000000-0000-0000-0000-000000000002', '66000000-0000-0000-0000-000000000002', null, '40000000-0000-0000-0000-000000000001', 'Evaluacion depilacion laser', 'REPROGRAMADA', '2026-07-22T17:00:00Z', '2026-07-22T17:30:00Z', 30, 'Sucursal Providencia', 'Cliente solicito mover la cita por trabajo.', null)
on conflict (id) do nothing;

-- Order requests
insert into order_request (id, business_id, customer_id, lead_id, conversation_id, created_by_user_id, status, payment_status, subtotal_amount, discount_amount, total_amount, paid_amount, balance_due, currency, due_date, notes)
values ('69000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '63000000-0000-0000-0000-000000000001', '66000000-0000-0000-0000-000000000001', '64000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'CONFIRMED', 'PARTIAL', 59980.00, 5000.00, 54980.00, 20000.00, 34980.00, 'CLP', '2026-08-05', 'Pedido demo creado desde la conversacion de Sofia.')
on conflict (id) do nothing;

-- Order items
insert into order_item (id, business_id, order_request_id, product_service_id, product_name_snapshot, sku_snapshot, quantity, unit_price, line_total)
values ('69100000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '69000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000001', 'Limpieza Facial Profunda', 'FAC-001', 1, 34990.00, 34990.00),
       ('69100000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '69000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000002', 'Facial Hidratante Express', 'FAC-002', 1, 24990.00, 24990.00)
on conflict (id) do nothing;

-- Payments
insert into payment (id, business_id, order_request_id, amount, method, paid_at, reference, notes)
values ('69200000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '69000000-0000-0000-0000-000000000001', 20000.00, 'TRANSFER', '2026-06-25T19:10:00Z', 'TRX-781', 'Abono inicial del pedido demo.')
on conflict (id) do nothing;

-- Automation rules
insert into automation_rule (id, business_id, name, trigger_type, keyword, delay_minutes, action_type, response_template_id, assigned_user_id, active)
values ('69300000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Seguimiento palabra precio', 'KEYWORD_MATCH', 'precio', 5, 'SEND_TEMPLATE', '62000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', true)
on conflict (id) do nothing;

-- Automation executions
insert into automation_execution (id, business_id, rule_id, conversation_id, run_type, status, input_payload, result_payload, executed_at)
values ('69400000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '69300000-0000-0000-0000-000000000001', '64000000-0000-0000-0000-000000000001', 'TEST', 'SUCCESS', '{"sampleMessage":"Quiero saber el precio"}'::jsonb, '{"matched":true,"action":"SEND_TEMPLATE","delayMinutes":5}'::jsonb, '2026-06-25T19:20:00Z')
on conflict (id) do nothing;

-- Channel event logs
insert into channel_event_log (id, business_id, channel_account_id, delivery_id, event_type, payload, received_at, processed_at, processing_status)
values ('69600000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '69500000-0000-0000-0000-000000000001', 'evt-demo-1001', 'MESSAGE_RECEIVED', '{"from":"+56911112222","body":"Hola, quiero saber el precio de la limpieza facial."}'::jsonb, '2026-06-25T18:10:05Z', '2026-06-25T18:10:07Z', 'PROCESSED')
on conflict (business_id, delivery_id) do nothing;

-- Message delivery logs
insert into message_delivery_log (id, business_id, message_id, delivery_status, provider_event_id, provider_payload, occurred_at)
values ('69700000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '65000000-0000-0000-0000-000000000002', 'DELIVERED', 'ack-demo-1001', '{"status":"DELIVERED"}'::jsonb, '2026-06-25T18:15:10Z')
on conflict (id) do nothing;

-- Notifications
insert into notification (id, business_id, user_id, type, status, title, body, related_entity_type, related_entity_id, read_at)
values ('69800000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '40000000-0000-0000-0000-000000000001', 'NEW_MESSAGE', 'UNREAD', 'Nuevo mensaje de Sofia Rojas', 'Sofia consulto por limpieza facial y quedo una conversacion abierta.', 'CONVERSATION', '64000000-0000-0000-0000-000000000001', null)
on conflict (id) do nothing;

-- Audit logs
insert into audit_log (id, business_id, actor_user_id, action_type, entity_type, entity_id, summary, metadata, occurred_at)
values ('69900000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '40000000-0000-0000-0000-000000000001', 'USER_CREATED', 'USER_ACCOUNT', '40000000-0000-0000-0000-000000000001', 'Usuario administrador demo creado.', '{"email":"admin@demo.cl"}'::jsonb, '2026-06-25T17:50:00Z'),
       ('69900000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '40000000-0000-0000-0000-000000000001', 'RULE_TESTED', 'AUTOMATION_RULE', '69300000-0000-0000-0000-000000000001', 'Regla demo probada con coincidencia positiva.', '{"matched":true}'::jsonb, '2026-06-25T19:20:01Z')
on conflict (id) do nothing;


