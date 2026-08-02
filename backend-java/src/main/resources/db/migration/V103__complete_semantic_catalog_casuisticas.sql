-- =============================================================================
-- V103: Catalogo semantico completo desde agenda_digital_whatsapp_casuisticas.xlsx
-- Casuisticas y capacidades de la agenda digital -> ai_canonical_entity (OTHER)
-- con alias de sus frases/mensajes -> ai_entity_alias
-- Generado automaticamente desde el Excel (nunca editar a mano).
-- =============================================================================

do $$
declare
    v_business uuid := '11111111-1111-1111-1111-111111111111'::uuid;
    v_canonical_id uuid;
begin

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 001 identificar intencion', 'CAP-001 Identificar intencion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 001 identificar intencion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-001 Identificar intencion', 'capacidad', 'Identificar intencion', 'identificar intencion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-001 Identificar intencion', 'capacidad', 'Recepcion', 'recepcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-001 Identificar intencion', 'capacidad', 'Detectar si el cliente quiere reservar, reprogramar, cancelar, consultar precio, consultar ubicacion o hablar con humano.', 'detectar si el cliente quiere reservar reprogramar cancelar consultar precio consultar ubicacion o hablar con humano', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 002 identificar cliente', 'CAP-002 Identificar cliente', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 002 identificar cliente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-002 Identificar cliente', 'capacidad', 'Identificar cliente', 'identificar cliente', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-002 Identificar cliente', 'capacidad', 'Recepcion', 'recepcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-002 Identificar cliente', 'capacidad', 'Solicitar nombre, telefono y documento solo si el negocio lo requiere.', 'solicitar nombre telefono y documento solo si el negocio lo requiere', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 003 mostrar servicios', 'CAP-003 Mostrar servicios', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 003 mostrar servicios'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-003 Mostrar servicios', 'capacidad', 'Mostrar servicios', 'mostrar servicios', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-003 Mostrar servicios', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-003 Mostrar servicios', 'capacidad', 'Listar categorias y servicios activos con precio, duracion, descripcion, requisitos y sucursales disponibles.', 'listar categorias y servicios activos con precio duracion descripcion requisitos y sucursales disponibles', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 004 seleccionar sucursal', 'CAP-004 Seleccionar sucursal', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 004 seleccionar sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-004 Seleccionar sucursal', 'capacidad', 'Seleccionar sucursal', 'seleccionar sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-004 Seleccionar sucursal', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-004 Seleccionar sucursal', 'capacidad', 'Permitir elegir una sucursal o recomendar la mas cercana si existe direccion o comuna.', 'permitir elegir una sucursal o recomendar la mas cercana si existe direccion o comuna', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 005 seleccionar profesional', 'CAP-005 Seleccionar profesional', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 005 seleccionar profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-005 Seleccionar profesional', 'capacidad', 'Seleccionar profesional', 'seleccionar profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-005 Seleccionar profesional', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-005 Seleccionar profesional', 'capacidad', 'Permitir elegir profesional especifico o asignacion automatica segun disponibilidad y competencia.', 'permitir elegir profesional especifico o asignacion automatica segun disponibilidad y competencia', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 006 consultar disponibilidad', 'CAP-006 Consultar disponibilidad', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 006 consultar disponibilidad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-006 Consultar disponibilidad', 'capacidad', 'Consultar disponibilidad', 'consultar disponibilidad', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-006 Consultar disponibilidad', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-006 Consultar disponibilidad', 'capacidad', 'Generar horarios libres considerando duracion, buffer, bloqueos, feriados, cupos, profesional y sucursal.', 'generar horarios libres considerando duracion buffer bloqueos feriados cupos profesional y sucursal', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 007 reservar cita', 'CAP-007 Reservar cita', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 007 reservar cita'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-007 Reservar cita', 'capacidad', 'Reservar cita', 'reservar cita', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-007 Reservar cita', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-007 Reservar cita', 'capacidad', 'Crear reserva confirmada o pre-reserva segun regla de pago/confirmacion.', 'crear reserva confirmada o pre reserva segun regla de pago confirmacion', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 008 bloqueo temporal', 'CAP-008 Bloqueo temporal', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 008 bloqueo temporal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-008 Bloqueo temporal', 'capacidad', 'Bloqueo temporal', 'bloqueo temporal', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-008 Bloqueo temporal', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-008 Bloqueo temporal', 'capacidad', 'Bloquear el horario elegido durante un tiempo acotado mientras el cliente confirma o paga.', 'bloquear el horario elegido durante un tiempo acotado mientras el cliente confirma o paga', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 009 pago previo opcional', 'CAP-009 Pago previo opcional', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 009 pago previo opcional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-009 Pago previo opcional', 'capacidad', 'Pago previo opcional', 'pago previo opcional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-009 Pago previo opcional', 'capacidad', 'Pagos', 'pagos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-009 Pago previo opcional', 'capacidad', 'Generar enlace de pago cuando el servicio requiere abono, garantia o pago total.', 'generar enlace de pago cuando el servicio requiere abono garantia o pago total', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 010 confirmacion whatsapp', 'CAP-010 Confirmacion WhatsApp', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 010 confirmacion whatsapp'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-010 Confirmacion WhatsApp', 'capacidad', 'Confirmacion WhatsApp', 'confirmacion whatsapp', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-010 Confirmacion WhatsApp', 'capacidad', 'Notificaciones', 'notificaciones', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-010 Confirmacion WhatsApp', 'capacidad', 'Enviar resumen de reserva con fecha, hora, servicio, profesional, sucursal, direccion y politica.', 'enviar resumen de reserva con fecha hora servicio profesional sucursal direccion y politica', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 011 recordatorios', 'CAP-011 Recordatorios', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 011 recordatorios'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-011 Recordatorios', 'capacidad', 'Recordatorios', 'recordatorios', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-011 Recordatorios', 'capacidad', 'Notificaciones', 'notificaciones', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-011 Recordatorios', 'capacidad', 'Enviar recordatorios configurables antes de la cita.', 'enviar recordatorios configurables antes de la cita', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 012 reprogramar cita', 'CAP-012 Reprogramar cita', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 012 reprogramar cita'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-012 Reprogramar cita', 'capacidad', 'Reprogramar cita', 'reprogramar cita', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-012 Reprogramar cita', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-012 Reprogramar cita', 'capacidad', 'Cambiar fecha, hora, profesional o sucursal segun reglas y disponibilidad.', 'cambiar fecha hora profesional o sucursal segun reglas y disponibilidad', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 013 cancelar cita', 'CAP-013 Cancelar cita', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 013 cancelar cita'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-013 Cancelar cita', 'capacidad', 'Cancelar cita', 'cancelar cita', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-013 Cancelar cita', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-013 Cancelar cita', 'capacidad', 'Permitir cancelacion por cliente, operador o sistema segun estado y politica.', 'permitir cancelacion por cliente operador o sistema segun estado y politica', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 014 gestion multi sucursal', 'CAP-014 Gestion multi-sucursal', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 014 gestion multi sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-014 Gestion multi-sucursal', 'capacidad', 'Gestion multi-sucursal', 'gestion multi sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-014 Gestion multi-sucursal', 'capacidad', 'Sucursal', 'sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-014 Gestion multi-sucursal', 'capacidad', 'Manejar reglas por sucursal: horario, feriados, direccion, recursos, profesionales, servicios y politicas.', 'manejar reglas por sucursal horario feriados direccion recursos profesionales servicios y politicas', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 015 agenda individual', 'CAP-015 Agenda individual', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 015 agenda individual'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-015 Agenda individual', 'capacidad', 'Agenda individual', 'agenda individual', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-015 Agenda individual', 'capacidad', 'Profesional', 'profesional', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-015 Agenda individual', 'capacidad', 'Mantener calendario por profesional con jornada, pausas, ausencias, vacaciones, permisos y excepciones.', 'mantener calendario por profesional con jornada pausas ausencias vacaciones permisos y excepciones', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 016 validar cabina equipo', 'CAP-016 Validar cabina/equipo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 016 validar cabina equipo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-016 Validar cabina/equipo', 'capacidad', 'Validar cabina/equipo', 'validar cabina equipo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-016 Validar cabina/equipo', 'capacidad', 'Recursos', 'recursos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-016 Validar cabina/equipo', 'capacidad', 'Si el servicio requiere cabina, sala o maquina, validar disponibilidad del recurso.', 'si el servicio requiere cabina sala o maquina validar disponibilidad del recurso', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 017 derivacion humana', 'CAP-017 Derivacion humana', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 017 derivacion humana'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-017 Derivacion humana', 'capacidad', 'Derivacion humana', 'derivacion humana', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-017 Derivacion humana', 'capacidad', 'Atencion', 'atencion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-017 Derivacion humana', 'capacidad', 'Escalar reclamos, urgencias, datos ambiguos, excepciones de pago o solicitudes fuera de alcance.', 'escalar reclamos urgencias datos ambiguos excepciones de pago o solicitudes fuera de alcance', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 018 trazabilidad', 'CAP-018 Trazabilidad', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 018 trazabilidad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-018 Trazabilidad', 'capacidad', 'Trazabilidad', 'trazabilidad', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-018 Trazabilidad', 'capacidad', 'Auditoria', 'auditoria', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-018 Trazabilidad', 'capacidad', 'Registrar quien, cuando, que cambio y desde que canal se realizo cada accion.', 'registrar quien cuando que cambio y desde que canal se realizo cada accion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 019 idempotencia', 'CAP-019 Idempotencia', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 019 idempotencia'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-019 Idempotencia', 'capacidad', 'Idempotencia', 'idempotencia', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-019 Idempotencia', 'capacidad', 'Seguridad', 'seguridad', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-019 Idempotencia', 'capacidad', 'Evitar duplicados si el cliente envia varias veces el mismo mensaje o si se reintenta una operacion.', 'evitar duplicados si el cliente envia varias veces el mismo mensaje o si se reintenta una operacion', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 020 metricas operativas', 'CAP-020 Metricas operativas', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 020 metricas operativas'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-020 Metricas operativas', 'capacidad', 'Metricas operativas', 'metricas operativas', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-020 Metricas operativas', 'capacidad', 'Reportes', 'reportes', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-020 Metricas operativas', 'capacidad', 'Medir reservas, cancelaciones, reprogramaciones, ausencias, ocupacion por profesional/sucursal y conversion WhatsApp.', 'medir reservas cancelaciones reprogramaciones ausencias ocupacion por profesional sucursal y conversion whatsapp', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 021 parametros por negocio', 'CAP-021 Parametros por negocio', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 021 parametros por negocio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-021 Parametros por negocio', 'capacidad', 'Parametros por negocio', 'parametros por negocio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-021 Parametros por negocio', 'capacidad', 'Configuracion', 'configuracion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-021 Parametros por negocio', 'capacidad', 'Definir duracion base, buffers, anticipacion minima, anticipacion maxima, tolerancia de atraso, politicas de cancelacion y reprogramacion.', 'definir duracion base buffers anticipacion minima anticipacion maxima tolerancia de atraso politicas de cancelacion y reprogramacion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 022 horarios especiales', 'CAP-022 Horarios especiales', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 022 horarios especiales'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-022 Horarios especiales', 'capacidad', 'Horarios especiales', 'horarios especiales', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-022 Horarios especiales', 'capacidad', 'Configuracion', 'configuracion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-022 Horarios especiales', 'capacidad', 'Registrar horarios por fecha especifica, turnos extendidos, cierre temprano y apertura extraordinaria.', 'registrar horarios por fecha especifica turnos extendidos cierre temprano y apertura extraordinaria', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 023 lista de espera', 'CAP-023 Lista de espera', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 023 lista de espera'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-023 Lista de espera', 'capacidad', 'Lista de espera', 'lista de espera', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-023 Lista de espera', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-023 Lista de espera', 'capacidad', 'Ofrecer ingreso a lista de espera cuando no exista horario disponible.', 'ofrecer ingreso a lista de espera cuando no exista horario disponible', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 024 confirmacion activa', 'CAP-024 Confirmacion activa', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 024 confirmacion activa'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-024 Confirmacion activa', 'capacidad', 'Confirmacion activa', 'confirmacion activa', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-024 Confirmacion activa', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-024 Confirmacion activa', 'capacidad', 'Pedir confirmacion del cliente antes de ejecutar reserva, reprogramacion o cancelacion.', 'pedir confirmacion del cliente antes de ejecutar reserva reprogramacion o cancelacion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 025 evitar reservas fuera de rango', 'CAP-025 Evitar reservas fuera de rango', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 025 evitar reservas fuera de rango'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-025 Evitar reservas fuera de rango', 'capacidad', 'Evitar reservas fuera de rango', 'evitar reservas fuera de rango', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-025 Evitar reservas fuera de rango', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-025 Evitar reservas fuera de rango', 'capacidad', 'Impedir citas antes de la fecha actual, demasiado lejanas o fuera del calendario publicado.', 'impedir citas antes de la fecha actual demasiado lejanas o fuera del calendario publicado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 026 zona horaria', 'CAP-026 Zona horaria', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 026 zona horaria'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-026 Zona horaria', 'capacidad', 'Zona horaria', 'zona horaria', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-026 Zona horaria', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-026 Zona horaria', 'capacidad', 'Normalizar fecha y hora segun zona horaria del negocio y mostrar al cliente en formato local.', 'normalizar fecha y hora segun zona horaria del negocio y mostrar al cliente en formato local', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 027 servicios encadenados', 'CAP-027 Servicios encadenados', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 027 servicios encadenados'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-027 Servicios encadenados', 'capacidad', 'Servicios encadenados', 'servicios encadenados', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-027 Servicios encadenados', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-027 Servicios encadenados', 'capacidad', 'Reservar multiples servicios consecutivos cuando el cliente elija un paquete.', 'reservar multiples servicios consecutivos cuando el cliente elija un paquete', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 028 servicio con evaluacion previa', 'CAP-028 Servicio con evaluacion previa', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 028 servicio con evaluacion previa'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-028 Servicio con evaluacion previa', 'capacidad', 'Servicio con evaluacion previa', 'servicio con evaluacion previa', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-028 Servicio con evaluacion previa', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-028 Servicio con evaluacion previa', 'capacidad', 'Marcar servicios que requieren evaluacion y limitar reserva directa si corresponde.', 'marcar servicios que requieren evaluacion y limitar reserva directa si corresponde', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 029 consentimiento informado', 'CAP-029 Consentimiento informado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 029 consentimiento informado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-029 Consentimiento informado', 'capacidad', 'Consentimiento informado', 'consentimiento informado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-029 Consentimiento informado', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-029 Consentimiento informado', 'capacidad', 'Solicitar aceptacion previa cuando el servicio lo requiera.', 'solicitar aceptacion previa cuando el servicio lo requiera', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 030 restricciones por edad', 'CAP-030 Restricciones por edad', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 030 restricciones por edad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-030 Restricciones por edad', 'capacidad', 'Restricciones por edad', 'restricciones por edad', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-030 Restricciones por edad', 'capacidad', 'Agenda', 'agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-030 Restricciones por edad', 'capacidad', 'Bloquear o derivar servicios con edad minima, tutor requerido o restricciones internas.', 'bloquear o derivar servicios con edad minima tutor requerido o restricciones internas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 031 historial de ausencias', 'CAP-031 Historial de ausencias', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 031 historial de ausencias'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-031 Historial de ausencias', 'capacidad', 'Historial de ausencias', 'historial de ausencias', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-031 Historial de ausencias', 'capacidad', 'Cliente', 'cliente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-031 Historial de ausencias', 'capacidad', 'Aplicar reglas especiales a clientes con inasistencias reiteradas.', 'aplicar reglas especiales a clientes con inasistencias reiteradas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 032 bloqueo administrativo', 'CAP-032 Bloqueo administrativo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 032 bloqueo administrativo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-032 Bloqueo administrativo', 'capacidad', 'Bloqueo administrativo', 'bloqueo administrativo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-032 Bloqueo administrativo', 'capacidad', 'Cliente', 'cliente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-032 Bloqueo administrativo', 'capacidad', 'Impedir reservas a clientes bloqueados o con deuda critica segun politica.', 'impedir reservas a clientes bloqueados o con deuda critica segun politica', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 033 abono obligatorio', 'CAP-033 Abono obligatorio', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 033 abono obligatorio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-033 Abono obligatorio', 'capacidad', 'Abono obligatorio', 'abono obligatorio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-033 Abono obligatorio', 'capacidad', 'Pagos', 'pagos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-033 Abono obligatorio', 'capacidad', 'Reservar solo cuando el abono quede validado o dejar en estado pendiente de pago.', 'reservar solo cuando el abono quede validado o dejar en estado pendiente de pago', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 034 diferencia de precio', 'CAP-034 Diferencia de precio', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 034 diferencia de precio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-034 Diferencia de precio', 'capacidad', 'Diferencia de precio', 'diferencia de precio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-034 Diferencia de precio', 'capacidad', 'Pagos', 'pagos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-034 Diferencia de precio', 'capacidad', 'Calcular diferencia cuando una reprogramacion cambia servicio, profesional, horario o sucursal.', 'calcular diferencia cuando una reprogramacion cambia servicio profesional horario o sucursal', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 035 devolucion', 'CAP-035 Devolucion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 035 devolucion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-035 Devolucion', 'capacidad', 'Devolucion', 'devolucion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-035 Devolucion', 'capacidad', 'Pagos', 'pagos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-035 Devolucion', 'capacidad', 'Iniciar flujo de devolucion o saldo a favor cuando aplique por cancelacion.', 'iniciar flujo de devolucion o saldo a favor cuando aplique por cancelacion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 036 insumos criticos', 'CAP-036 Insumos criticos', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 036 insumos criticos'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-036 Insumos criticos', 'capacidad', 'Insumos criticos', 'insumos criticos', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-036 Insumos criticos', 'capacidad', 'Inventario', 'inventario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-036 Insumos criticos', 'capacidad', 'Validar disponibilidad de insumos si el servicio depende de stock minimo.', 'validar disponibilidad de insumos si el servicio depende de stock minimo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 037 capacidad simultanea', 'CAP-037 Capacidad simultanea', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 037 capacidad simultanea'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-037 Capacidad simultanea', 'capacidad', 'Capacidad simultanea', 'capacidad simultanea', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-037 Capacidad simultanea', 'capacidad', 'Recursos', 'recursos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-037 Capacidad simultanea', 'capacidad', 'Validar cupos por sala, cabina, maquina o grupo.', 'validar cupos por sala cabina maquina o grupo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 038 limpieza preparacion', 'CAP-038 Limpieza/preparacion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 038 limpieza preparacion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-038 Limpieza/preparacion', 'capacidad', 'Limpieza/preparacion', 'limpieza preparacion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-038 Limpieza/preparacion', 'capacidad', 'Recursos', 'recursos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-038 Limpieza/preparacion', 'capacidad', 'Agregar tiempo de preparacion antes o despues del servicio.', 'agregar tiempo de preparacion antes o despues del servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 039 competencias', 'CAP-039 Competencias', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 039 competencias'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-039 Competencias', 'capacidad', 'Competencias', 'competencias', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-039 Competencias', 'capacidad', 'Profesional', 'profesional', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-039 Competencias', 'capacidad', 'Asignar solo profesionales certificados o habilitados para el servicio.', 'asignar solo profesionales certificados o habilitados para el servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 040 bloqueos personales', 'CAP-040 Bloqueos personales', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 040 bloqueos personales'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-040 Bloqueos personales', 'capacidad', 'Bloqueos personales', 'bloqueos personales', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-040 Bloqueos personales', 'capacidad', 'Profesional', 'profesional', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-040 Bloqueos personales', 'capacidad', 'Respetar permisos, vacaciones, licencias, colacion y pausas.', 'respetar permisos vacaciones licencias colacion y pausas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 041 feriados locales', 'CAP-041 Feriados locales', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 041 feriados locales'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-041 Feriados locales', 'capacidad', 'Feriados locales', 'feriados locales', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-041 Feriados locales', 'capacidad', 'Sucursal', 'sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-041 Feriados locales', 'capacidad', 'Aplicar feriados o cierres propios por sucursal.', 'aplicar feriados o cierres propios por sucursal', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 042 traslado profesional', 'CAP-042 Traslado profesional', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 042 traslado profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-042 Traslado profesional', 'capacidad', 'Traslado profesional', 'traslado profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-042 Traslado profesional', 'capacidad', 'Sucursal', 'sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-042 Traslado profesional', 'capacidad', 'Agregar tiempo de traslado si el profesional trabaja en mas de una sucursal el mismo dia.', 'agregar tiempo de traslado si el profesional trabaja en mas de una sucursal el mismo dia', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 043 servicios por sede', 'CAP-043 Servicios por sede', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 043 servicios por sede'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-043 Servicios por sede', 'capacidad', 'Servicios por sede', 'servicios por sede', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-043 Servicios por sede', 'capacidad', 'Sucursal', 'sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-043 Servicios por sede', 'capacidad', 'Mostrar solo servicios habilitados en la sede seleccionada.', 'mostrar solo servicios habilitados en la sede seleccionada', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 044 preguntas frecuentes', 'CAP-044 Preguntas frecuentes', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 044 preguntas frecuentes'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-044 Preguntas frecuentes', 'capacidad', 'Preguntas frecuentes', 'preguntas frecuentes', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-044 Preguntas frecuentes', 'capacidad', 'Soporte', 'soporte', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-044 Preguntas frecuentes', 'capacidad', 'Responder horarios, direccion, estacionamiento, medios de pago y politicas.', 'responder horarios direccion estacionamiento medios de pago y politicas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 045 reclamos', 'CAP-045 Reclamos', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 045 reclamos'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-045 Reclamos', 'capacidad', 'Reclamos', 'reclamos', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-045 Reclamos', 'capacidad', 'Soporte', 'soporte', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-045 Reclamos', 'capacidad', 'Derivar inmediatamente reclamos complejos, clientes molestos o amenazas de denuncia.', 'derivar inmediatamente reclamos complejos clientes molestos o amenazas de denuncia', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 046 permisos por rol', 'CAP-046 Permisos por rol', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 046 permisos por rol'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-046 Permisos por rol', 'capacidad', 'Permisos por rol', 'permisos por rol', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-046 Permisos por rol', 'capacidad', 'Seguridad', 'seguridad', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-046 Permisos por rol', 'capacidad', 'Permitir acciones distintas para cliente, recepcionista, administrador y sistema.', 'permitir acciones distintas para cliente recepcionista administrador y sistema', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 047 privacidad', 'CAP-047 Privacidad', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 047 privacidad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-047 Privacidad', 'capacidad', 'Privacidad', 'privacidad', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-047 Privacidad', 'capacidad', 'Seguridad', 'seguridad', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-047 Privacidad', 'capacidad', 'Ocultar datos personales en mensajes internos y registrar solo lo necesario.', 'ocultar datos personales en mensajes internos y registrar solo lo necesario', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 048 eventos de dominio', 'CAP-048 Eventos de dominio', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 048 eventos de dominio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-048 Eventos de dominio', 'capacidad', 'Eventos de dominio', 'eventos de dominio', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-048 Eventos de dominio', 'capacidad', 'Auditoria', 'auditoria', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-048 Eventos de dominio', 'capacidad', 'Registrar ReservaCreada, ReservaReprogramada, ReservaCancelada, PagoConfirmado y NotificacionEnviada.', 'registrar reservacreada reservareprogramada reservacancelada pagoconfirmado y notificacionenviada', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 049 calendario externo', 'CAP-049 Calendario externo', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 049 calendario externo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-049 Calendario externo', 'capacidad', 'Calendario externo', 'calendario externo', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-049 Calendario externo', 'capacidad', 'Integracion', 'integracion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-049 Calendario externo', 'capacidad', 'Sincronizar con calendario externo si aplica, manejando errores y duplicados.', 'sincronizar con calendario externo si aplica manejando errores y duplicados', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'cap 050 panel administrativo', 'CAP-050 Panel administrativo', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'cap 050 panel administrativo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-050 Panel administrativo', 'capacidad', 'Panel administrativo', 'panel administrativo', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-050 Panel administrativo', 'capacidad', 'Integracion', 'integracion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAP-050 Panel administrativo', 'capacidad', 'Permitir busqueda, filtros, cambios manuales y revision de trazabilidad.', 'permitir busqueda filtros cambios manuales y revision de trazabilidad', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'id casuistica', 'ID Casuistica', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'id casuistica'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_pre_reserva', 'Casuistica', 'casuistica', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_pre_reserva', 'Condicion a validar', 'condicion a validar', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_pre_reserva', 'Mensaje WhatsApp sugerido', 'mensaje whatsapp sugerido', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 001 cliente nuevo sin nombre', 'PRE-RES-001 Cliente nuevo sin nombre', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 001 cliente nuevo sin nombre'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-001 Cliente nuevo sin nombre', 'casuistica_pre_reserva', 'Cliente nuevo sin nombre', 'cliente nuevo sin nombre', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-001 Cliente nuevo sin nombre', 'casuistica_pre_reserva', 'El numero WhatsApp no tiene cliente asociado o falta nombre minimo.', 'el numero whatsapp no tiene cliente asociado o falta nombre minimo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-001 Cliente nuevo sin nombre', 'casuistica_pre_reserva', 'Para reservar necesito tu nombre. ¿Me lo indicas?', 'para reservar necesito tu nombre me lo indicas', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 002 telefono no validado', 'PRE-RES-002 Telefono no validado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 002 telefono no validado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-002 Telefono no validado', 'casuistica_pre_reserva', 'Telefono no validado', 'telefono no validado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-002 Telefono no validado', 'casuistica_pre_reserva', 'El telefono no coincide con formato local o no existe como canal confirmado.', 'el telefono no coincide con formato local o no existe como canal confirmado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-002 Telefono no validado', 'casuistica_pre_reserva', 'Confirmame si este es tu numero de contacto para la reserva.', 'confirmame si este es tu numero de contacto para la reserva', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 003 cliente duplicado', 'PRE-RES-003 Cliente duplicado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 003 cliente duplicado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-003 Cliente duplicado', 'casuistica_pre_reserva', 'Cliente duplicado', 'cliente duplicado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-003 Cliente duplicado', 'casuistica_pre_reserva', 'Existen dos fichas con el mismo telefono o documento.', 'existen dos fichas con el mismo telefono o documento', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-003 Cliente duplicado', 'casuistica_pre_reserva', 'Tengo que validar tus datos con recepcion para evitar duplicados.', 'tengo que validar tus datos con recepcion para evitar duplicados', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 004 cliente bloqueado', 'PRE-RES-004 Cliente bloqueado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 004 cliente bloqueado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-004 Cliente bloqueado', 'casuistica_pre_reserva', 'Cliente bloqueado', 'cliente bloqueado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-004 Cliente bloqueado', 'casuistica_pre_reserva', 'Cliente tiene bloqueo administrativo, deuda critica o restriccion manual.', 'cliente tiene bloqueo administrativo deuda critica o restriccion manual', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-004 Cliente bloqueado', 'casuistica_pre_reserva', 'Tu caso requiere revision de recepcion antes de agendar.', 'tu caso requiere revision de recepcion antes de agendar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 005 menor de edad', 'PRE-RES-005 Menor de edad', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 005 menor de edad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-005 Menor de edad', 'casuistica_pre_reserva', 'Menor de edad', 'menor de edad', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-005 Menor de edad', 'casuistica_pre_reserva', 'Servicio exige edad minima o tutor.', 'servicio exige edad minima o tutor', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-005 Menor de edad', 'casuistica_pre_reserva', 'Este servicio requiere confirmacion de un adulto responsable.', 'este servicio requiere confirmacion de un adulto responsable', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 006 servicio inactivo', 'PRE-RES-006 Servicio inactivo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 006 servicio inactivo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-006 Servicio inactivo', 'casuistica_pre_reserva', 'Servicio inactivo', 'servicio inactivo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-006 Servicio inactivo', 'casuistica_pre_reserva', 'El servicio no esta vigente o fue deshabilitado.', 'el servicio no esta vigente o fue deshabilitado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-006 Servicio inactivo', 'casuistica_pre_reserva', 'Ese servicio no esta disponible. Te puedo mostrar alternativas.', 'ese servicio no esta disponible te puedo mostrar alternativas', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 007 servicio no publicado', 'PRE-RES-007 Servicio no publicado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 007 servicio no publicado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-007 Servicio no publicado', 'casuistica_pre_reserva', 'Servicio no publicado', 'servicio no publicado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-007 Servicio no publicado', 'casuistica_pre_reserva', 'Servicio interno no debe ser reservado por cliente final.', 'servicio interno no debe ser reservado por cliente final', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-007 Servicio no publicado', 'casuistica_pre_reserva', 'Ese servicio se agenda solo por recepcion.', 'ese servicio se agenda solo por recepcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 008 servicio requiere evaluacion', 'PRE-RES-008 Servicio requiere evaluacion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 008 servicio requiere evaluacion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-008 Servicio requiere evaluacion', 'casuistica_pre_reserva', 'Servicio requiere evaluacion', 'servicio requiere evaluacion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-008 Servicio requiere evaluacion', 'casuistica_pre_reserva', 'Servicio marcado con evaluacion previa obligatoria.', 'servicio marcado con evaluacion previa obligatoria', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-008 Servicio requiere evaluacion', 'casuistica_pre_reserva', 'Primero debemos agendar una evaluacion previa.', 'primero debemos agendar una evaluacion previa', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 009 servicio requiere consentimiento', 'PRE-RES-009 Servicio requiere consentimiento', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 009 servicio requiere consentimiento'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-009 Servicio requiere consentimiento', 'casuistica_pre_reserva', 'Servicio requiere consentimiento', 'servicio requiere consentimiento', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-009 Servicio requiere consentimiento', 'casuistica_pre_reserva', 'Servicio necesita consentimiento informado previo.', 'servicio necesita consentimiento informado previo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-009 Servicio requiere consentimiento', 'casuistica_pre_reserva', 'Antes de reservar necesito que aceptes el consentimiento informado.', 'antes de reservar necesito que aceptes el consentimiento informado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 010 duracion no configurada', 'PRE-RES-010 Duracion no configurada', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 010 duracion no configurada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-010 Duracion no configurada', 'casuistica_pre_reserva', 'Duracion no configurada', 'duracion no configurada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-010 Duracion no configurada', 'casuistica_pre_reserva', 'El servicio no tiene duracion o tiene duracion cero.', 'el servicio no tiene duracion o tiene duracion cero', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-010 Duracion no configurada', 'casuistica_pre_reserva', 'Este servicio requiere configuracion antes de agendar.', 'este servicio requiere configuracion antes de agendar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 011 precio no configurado', 'PRE-RES-011 Precio no configurado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 011 precio no configurado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-011 Precio no configurado', 'casuistica_pre_reserva', 'Precio no configurado', 'precio no configurado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-011 Precio no configurado', 'casuistica_pre_reserva', 'Servicio exige pago/abono, pero no tiene precio valido.', 'servicio exige pago abono pero no tiene precio valido', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-011 Precio no configurado', 'casuistica_pre_reserva', 'Recepcion validara el valor antes de confirmar.', 'recepcion validara el valor antes de confirmar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 012 servicio no disponible en sucursal', 'PRE-RES-012 Servicio no disponible en sucursal', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 012 servicio no disponible en sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-012 Servicio no disponible en sucursal', 'casuistica_pre_reserva', 'Servicio no disponible en sucursal', 'servicio no disponible en sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-012 Servicio no disponible en sucursal', 'casuistica_pre_reserva', 'El servicio no esta habilitado para la sucursal seleccionada.', 'el servicio no esta habilitado para la sucursal seleccionada', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-012 Servicio no disponible en sucursal', 'casuistica_pre_reserva', 'Ese servicio no se realiza en esta sucursal. Te muestro opciones disponibles.', 'ese servicio no se realiza en esta sucursal te muestro opciones disponibles', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 013 sucursal inactiva', 'PRE-RES-013 Sucursal inactiva', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 013 sucursal inactiva'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-013 Sucursal inactiva', 'casuistica_pre_reserva', 'Sucursal inactiva', 'sucursal inactiva', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-013 Sucursal inactiva', 'casuistica_pre_reserva', 'La sucursal esta cerrada, inactiva o en mantencion.', 'la sucursal esta cerrada inactiva o en mantencion', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-013 Sucursal inactiva', 'casuistica_pre_reserva', 'Esta sucursal no esta disponible para agendar.', 'esta sucursal no esta disponible para agendar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 014 sucursal sin direccion', 'PRE-RES-014 Sucursal sin direccion', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 014 sucursal sin direccion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-014 Sucursal sin direccion', 'casuistica_pre_reserva', 'Sucursal sin direccion', 'sucursal sin direccion', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-014 Sucursal sin direccion', 'casuistica_pre_reserva', 'La sucursal activa no tiene direccion visible.', 'la sucursal activa no tiene direccion visible', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-014 Sucursal sin direccion', 'casuistica_pre_reserva', 'Confirmaremos la direccion exacta por recepcion.', 'confirmaremos la direccion exacta por recepcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 015 horario de sucursal cerrado', 'PRE-RES-015 Horario de sucursal cerrado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 015 horario de sucursal cerrado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-015 Horario de sucursal cerrado', 'casuistica_pre_reserva', 'Horario de sucursal cerrado', 'horario de sucursal cerrado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-015 Horario de sucursal cerrado', 'casuistica_pre_reserva', 'Fecha/hora solicitada cae fuera del horario de la sede.', 'fecha hora solicitada cae fuera del horario de la sede', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-015 Horario de sucursal cerrado', 'casuistica_pre_reserva', 'Ese horario esta fuera de atencion. Te muestro horarios disponibles.', 'ese horario esta fuera de atencion te muestro horarios disponibles', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 016 feriado o cierre especial', 'PRE-RES-016 Feriado o cierre especial', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 016 feriado o cierre especial'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-016 Feriado o cierre especial', 'casuistica_pre_reserva', 'Feriado o cierre especial', 'feriado o cierre especial', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-016 Feriado o cierre especial', 'casuistica_pre_reserva', 'La fecha cae en feriado nacional, local o cierre configurado.', 'la fecha cae en feriado nacional local o cierre configurado', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-016 Feriado o cierre especial', 'casuistica_pre_reserva', 'Ese dia la sucursal no atiende. Busquemos otra fecha.', 'ese dia la sucursal no atiende busquemos otra fecha', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 017 capacidad maxima sede', 'PRE-RES-017 Capacidad maxima sede', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 017 capacidad maxima sede'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-017 Capacidad maxima sede', 'casuistica_pre_reserva', 'Capacidad maxima sede', 'capacidad maxima sede', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-017 Capacidad maxima sede', 'casuistica_pre_reserva', 'La sede tiene limite de atenciones simultaneas.', 'la sede tiene limite de atenciones simultaneas', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-017 Capacidad maxima sede', 'casuistica_pre_reserva', 'Ese bloque ya esta completo en la sucursal.', 'ese bloque ya esta completo en la sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 018 sucursal diferente a preferida', 'PRE-RES-018 Sucursal diferente a preferida', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 018 sucursal diferente a preferida'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-018 Sucursal diferente a preferida', 'casuistica_pre_reserva', 'Sucursal diferente a preferida', 'sucursal diferente a preferida', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-018 Sucursal diferente a preferida', 'casuistica_pre_reserva', 'Cliente tiene preferencia historica por otra sede.', 'cliente tiene preferencia historica por otra sede', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-018 Sucursal diferente a preferida', 'casuistica_pre_reserva', 'Veo que antes agendaste en otra sucursal. ¿Confirmas esta sede?', 'veo que antes agendaste en otra sucursal confirmas esta sede', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 019 profesional inactivo', 'PRE-RES-019 Profesional inactivo', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 019 profesional inactivo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-019 Profesional inactivo', 'casuistica_pre_reserva', 'Profesional inactivo', 'profesional inactivo', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-019 Profesional inactivo', 'casuistica_pre_reserva', 'Profesional esta deshabilitado o sin contrato vigente.', 'profesional esta deshabilitado o sin contrato vigente', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-019 Profesional inactivo', 'casuistica_pre_reserva', 'Ese profesional no esta disponible. Puedo ofrecerte otra opcion.', 'ese profesional no esta disponible puedo ofrecerte otra opcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 020 profesional no atiende servicio', 'PRE-RES-020 Profesional no atiende servicio', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 020 profesional no atiende servicio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-020 Profesional no atiende servicio', 'casuistica_pre_reserva', 'Profesional no atiende servicio', 'profesional no atiende servicio', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-020 Profesional no atiende servicio', 'casuistica_pre_reserva', 'No tiene competencia, certificacion o permiso para el servicio.', 'no tiene competencia certificacion o permiso para el servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-020 Profesional no atiende servicio', 'casuistica_pre_reserva', 'Ese profesional no realiza este servicio. Te muestro quienes si pueden atenderte.', 'ese profesional no realiza este servicio te muestro quienes si pueden atenderte', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 021 profesional no atiende sucursal', 'PRE-RES-021 Profesional no atiende sucursal', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 021 profesional no atiende sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-021 Profesional no atiende sucursal', 'casuistica_pre_reserva', 'Profesional no atiende sucursal', 'profesional no atiende sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-021 Profesional no atiende sucursal', 'casuistica_pre_reserva', 'No esta asignado a la sede en esa fecha.', 'no esta asignado a la sede en esa fecha', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-021 Profesional no atiende sucursal', 'casuistica_pre_reserva', 'Ese profesional no atiende en esa sucursal ese dia.', 'ese profesional no atiende en esa sucursal ese dia', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 022 profesional fuera de jornada', 'PRE-RES-022 Profesional fuera de jornada', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 022 profesional fuera de jornada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-022 Profesional fuera de jornada', 'casuistica_pre_reserva', 'Profesional fuera de jornada', 'profesional fuera de jornada', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-022 Profesional fuera de jornada', 'casuistica_pre_reserva', 'Hora solicitada cae fuera de su turno.', 'hora solicitada cae fuera de su turno', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-022 Profesional fuera de jornada', 'casuistica_pre_reserva', 'Ese horario esta fuera de la jornada del profesional.', 'ese horario esta fuera de la jornada del profesional', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 023 profesional en pausa', 'PRE-RES-023 Profesional en pausa', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 023 profesional en pausa'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-023 Profesional en pausa', 'casuistica_pre_reserva', 'Profesional en pausa', 'profesional en pausa', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-023 Profesional en pausa', 'casuistica_pre_reserva', 'Hora solicitada cae en colacion, descanso, reunion, capacitacion o bloqueo.', 'hora solicitada cae en colacion descanso reunion capacitacion o bloqueo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-023 Profesional en pausa', 'casuistica_pre_reserva', 'Ese horario no esta disponible. Te muestro alternativas.', 'ese horario no esta disponible te muestro alternativas', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 024 profesional con ausencia', 'PRE-RES-024 Profesional con ausencia', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 024 profesional con ausencia'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-024 Profesional con ausencia', 'casuistica_pre_reserva', 'Profesional con ausencia', 'profesional con ausencia', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-024 Profesional con ausencia', 'casuistica_pre_reserva', 'Profesional tiene vacaciones, permiso o licencia.', 'profesional tiene vacaciones permiso o licencia', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-024 Profesional con ausencia', 'casuistica_pre_reserva', 'Ese profesional no atiende ese dia.', 'ese profesional no atiende ese dia', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 025 solapamiento profesional', 'PRE-RES-025 Solapamiento profesional', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 025 solapamiento profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-025 Solapamiento profesional', 'casuistica_pre_reserva', 'Solapamiento profesional', 'solapamiento profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-025 Solapamiento profesional', 'casuistica_pre_reserva', 'Ya existe cita, bloqueo o pre-reserva que se cruza.', 'ya existe cita bloqueo o pre reserva que se cruza', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-025 Solapamiento profesional', 'casuistica_pre_reserva', 'Ese horario acaba de ocuparse. Elige otro horario.', 'ese horario acaba de ocuparse elige otro horario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 026 tiempo de traslado', 'PRE-RES-026 Tiempo de traslado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 026 tiempo de traslado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-026 Tiempo de traslado', 'casuistica_pre_reserva', 'Tiempo de traslado', 'tiempo de traslado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-026 Tiempo de traslado', 'casuistica_pre_reserva', 'Profesional viene de otra sucursal y no alcanza a llegar.', 'profesional viene de otra sucursal y no alcanza a llegar', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-026 Tiempo de traslado', 'casuistica_pre_reserva', 'Por traslado entre sucursales, ese horario no esta disponible.', 'por traslado entre sucursales ese horario no esta disponible', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 027 fecha pasada', 'PRE-RES-027 Fecha pasada', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 027 fecha pasada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-027 Fecha pasada', 'casuistica_pre_reserva', 'Fecha pasada', 'fecha pasada', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-027 Fecha pasada', 'casuistica_pre_reserva', 'Fecha/hora solicitada es anterior al momento actual.', 'fecha hora solicitada es anterior al momento actual', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-027 Fecha pasada', 'casuistica_pre_reserva', 'No puedo agendar una hora en el pasado. Indica otra fecha.', 'no puedo agendar una hora en el pasado indica otra fecha', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 028 anticipacion minima', 'PRE-RES-028 Anticipacion minima', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 028 anticipacion minima'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-028 Anticipacion minima', 'casuistica_pre_reserva', 'Anticipacion minima', 'anticipacion minima', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-028 Anticipacion minima', 'casuistica_pre_reserva', 'La cita se solicita con menos anticipacion que la politica.', 'la cita se solicita con menos anticipacion que la politica', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-028 Anticipacion minima', 'casuistica_pre_reserva', 'Para este servicio se requiere reservar con mas anticipacion.', 'para este servicio se requiere reservar con mas anticipacion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 029 anticipacion maxima', 'PRE-RES-029 Anticipacion maxima', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 029 anticipacion maxima'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-029 Anticipacion maxima', 'casuistica_pre_reserva', 'Anticipacion maxima', 'anticipacion maxima', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-029 Anticipacion maxima', 'casuistica_pre_reserva', 'La fecha supera el rango publico permitido.', 'la fecha supera el rango publico permitido', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-029 Anticipacion maxima', 'casuistica_pre_reserva', 'Aun no tenemos agenda abierta para esa fecha.', 'aun no tenemos agenda abierta para esa fecha', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 030 duracion no cabe', 'PRE-RES-030 Duracion no cabe', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 030 duracion no cabe'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-030 Duracion no cabe', 'casuistica_pre_reserva', 'Duracion no cabe', 'duracion no cabe', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-030 Duracion no cabe', 'casuistica_pre_reserva', 'Hora inicio disponible, pero duracion + buffer excede jornada o bloqueo.', 'hora inicio disponible pero duracion buffer excede jornada o bloqueo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-030 Duracion no cabe', 'casuistica_pre_reserva', 'Ese horario no alcanza para completar el servicio.', 'ese horario no alcanza para completar el servicio', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 031 buffer previo', 'PRE-RES-031 Buffer previo', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 031 buffer previo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-031 Buffer previo', 'casuistica_pre_reserva', 'Buffer previo', 'buffer previo', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-031 Buffer previo', 'casuistica_pre_reserva', 'Servicio exige preparacion antes de iniciar.', 'servicio exige preparacion antes de iniciar', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-031 Buffer previo', 'casuistica_pre_reserva', 'Ese horario requiere preparacion y no esta disponible.', 'ese horario requiere preparacion y no esta disponible', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 032 buffer posterior', 'PRE-RES-032 Buffer posterior', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 032 buffer posterior'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-032 Buffer posterior', 'casuistica_pre_reserva', 'Buffer posterior', 'buffer posterior', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-032 Buffer posterior', 'casuistica_pre_reserva', 'Servicio exige limpieza o cierre posterior.', 'servicio exige limpieza o cierre posterior', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-032 Buffer posterior', 'casuistica_pre_reserva', 'Ese horario no permite dejar el tiempo de preparacion posterior.', 'ese horario no permite dejar el tiempo de preparacion posterior', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 033 horario fragmentado', 'PRE-RES-033 Horario fragmentado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 033 horario fragmentado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-033 Horario fragmentado', 'casuistica_pre_reserva', 'Horario fragmentado', 'horario fragmentado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-033 Horario fragmentado', 'casuistica_pre_reserva', 'Queda un espacio libre inutilizable si se agenda ese bloque.', 'queda un espacio libre inutilizable si se agenda ese bloque', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-033 Horario fragmentado', 'casuistica_pre_reserva', 'Te muestro horarios que optimizan mejor la agenda.', 'te muestro horarios que optimizan mejor la agenda', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 034 reserva simultanea', 'PRE-RES-034 Reserva simultanea', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 034 reserva simultanea'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-034 Reserva simultanea', 'casuistica_pre_reserva', 'Reserva simultanea', 'reserva simultanea', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-034 Reserva simultanea', 'casuistica_pre_reserva', 'Otro cliente intenta tomar el mismo slot al mismo tiempo.', 'otro cliente intenta tomar el mismo slot al mismo tiempo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-034 Reserva simultanea', 'casuistica_pre_reserva', 'Ese horario se tomo mientras confirmabamos. Elige otro.', 'ese horario se tomo mientras confirmabamos elige otro', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 035 idempotencia', 'PRE-RES-035 Idempotencia', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 035 idempotencia'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-035 Idempotencia', 'casuistica_pre_reserva', 'Idempotencia', 'idempotencia', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-035 Idempotencia', 'casuistica_pre_reserva', 'Cliente confirma dos veces el mismo horario.', 'cliente confirma dos veces el mismo horario', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-035 Idempotencia', 'casuistica_pre_reserva', 'Tu reserva ya fue registrada. Te envio el resumen.', 'tu reserva ya fue registrada te envio el resumen', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 036 cabina no disponible', 'PRE-RES-036 Cabina no disponible', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 036 cabina no disponible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-036 Cabina no disponible', 'casuistica_pre_reserva', 'Cabina no disponible', 'cabina no disponible', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-036 Cabina no disponible', 'casuistica_pre_reserva', 'Servicio requiere cabina/sala y esta ocupada.', 'servicio requiere cabina sala y esta ocupada', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-036 Cabina no disponible', 'casuistica_pre_reserva', 'No hay cabina disponible en ese horario.', 'no hay cabina disponible en ese horario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 037 equipo no disponible', 'PRE-RES-037 Equipo no disponible', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 037 equipo no disponible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-037 Equipo no disponible', 'casuistica_pre_reserva', 'Equipo no disponible', 'equipo no disponible', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-037 Equipo no disponible', 'casuistica_pre_reserva', 'Servicio requiere equipo o maquina asignable.', 'servicio requiere equipo o maquina asignable', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-037 Equipo no disponible', 'casuistica_pre_reserva', 'El equipo requerido no esta disponible en ese horario.', 'el equipo requerido no esta disponible en ese horario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 038 insumos insuficientes', 'PRE-RES-038 Insumos insuficientes', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 038 insumos insuficientes'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-038 Insumos insuficientes', 'casuistica_pre_reserva', 'Insumos insuficientes', 'insumos insuficientes', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-038 Insumos insuficientes', 'casuistica_pre_reserva', 'Servicio requiere stock minimo y no existe disponibilidad.', 'servicio requiere stock minimo y no existe disponibilidad', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-038 Insumos insuficientes', 'casuistica_pre_reserva', 'Recepcion debe validar insumos antes de confirmar.', 'recepcion debe validar insumos antes de confirmar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 039 abono requerido', 'PRE-RES-039 Abono requerido', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 039 abono requerido'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-039 Abono requerido', 'casuistica_pre_reserva', 'Abono requerido', 'abono requerido', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-039 Abono requerido', 'casuistica_pre_reserva', 'Servicio/sucursal/profesional requiere abono previo.', 'servicio sucursal profesional requiere abono previo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-039 Abono requerido', 'casuistica_pre_reserva', 'Para confirmar debes realizar el abono en el enlace.', 'para confirmar debes realizar el abono en el enlace', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 040 pago pendiente anterior', 'PRE-RES-040 Pago pendiente anterior', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 040 pago pendiente anterior'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-040 Pago pendiente anterior', 'casuistica_pre_reserva', 'Pago pendiente anterior', 'pago pendiente anterior', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-040 Pago pendiente anterior', 'casuistica_pre_reserva', 'Cliente tiene pago vencido o deuda relacionada.', 'cliente tiene pago vencido o deuda relacionada', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-040 Pago pendiente anterior', 'casuistica_pre_reserva', 'Antes de agendar debemos regularizar un pago pendiente.', 'antes de agendar debemos regularizar un pago pendiente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 041 monto inconsistente', 'PRE-RES-041 Monto inconsistente', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 041 monto inconsistente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-041 Monto inconsistente', 'casuistica_pre_reserva', 'Monto inconsistente', 'monto inconsistente', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-041 Monto inconsistente', 'casuistica_pre_reserva', 'Monto calculado no coincide con lista vigente o promocion.', 'monto calculado no coincide con lista vigente o promocion', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-041 Monto inconsistente', 'casuistica_pre_reserva', 'Validaremos el valor final antes de confirmar.', 'validaremos el valor final antes de confirmar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 042 cupon vencido', 'PRE-RES-042 Cupon vencido', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 042 cupon vencido'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-042 Cupon vencido', 'casuistica_pre_reserva', 'Cupon vencido', 'cupon vencido', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-042 Cupon vencido', 'casuistica_pre_reserva', 'Cliente intenta usar promocion expirada.', 'cliente intenta usar promocion expirada', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-042 Cupon vencido', 'casuistica_pre_reserva', 'Ese descuento ya no esta vigente.', 'ese descuento ya no esta vigente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 043 cupon no aplicable', 'PRE-RES-043 Cupon no aplicable', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 043 cupon no aplicable'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-043 Cupon no aplicable', 'casuistica_pre_reserva', 'Cupon no aplicable', 'cupon no aplicable', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-043 Cupon no aplicable', 'casuistica_pre_reserva', 'Promocion no aplica a sucursal, servicio, dia o profesional.', 'promocion no aplica a sucursal servicio dia o profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-043 Cupon no aplicable', 'casuistica_pre_reserva', 'Ese descuento no aplica para esta reserva.', 'ese descuento no aplica para esta reserva', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 044 cliente con inasistencias', 'PRE-RES-044 Cliente con inasistencias', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 044 cliente con inasistencias'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-044 Cliente con inasistencias', 'casuistica_pre_reserva', 'Cliente con inasistencias', 'cliente con inasistencias', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-044 Cliente con inasistencias', 'casuistica_pre_reserva', 'Cliente supera umbral de no asistencia.', 'cliente supera umbral de no asistencia', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-044 Cliente con inasistencias', 'casuistica_pre_reserva', 'Por historial de inasistencia, esta reserva requiere confirmacion especial.', 'por historial de inasistencia esta reserva requiere confirmacion especial', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 045 limite de reservas activas', 'PRE-RES-045 Limite de reservas activas', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 045 limite de reservas activas'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-045 Limite de reservas activas', 'casuistica_pre_reserva', 'Limite de reservas activas', 'limite de reservas activas', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-045 Limite de reservas activas', 'casuistica_pre_reserva', 'Cliente ya tiene maximo de reservas futuras permitidas.', 'cliente ya tiene maximo de reservas futuras permitidas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-045 Limite de reservas activas', 'casuistica_pre_reserva', 'Ya tienes una reserva activa. Podemos modificarla si quieres.', 'ya tienes una reserva activa podemos modificarla si quieres', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 046 restriccion medica de seguridad', 'PRE-RES-046 Restriccion medica/de seguridad', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 046 restriccion medica de seguridad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-046 Restriccion medica/de seguridad', 'casuistica_pre_reserva', 'Restriccion medica/de seguridad', 'restriccion medica de seguridad', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-046 Restriccion medica/de seguridad', 'casuistica_pre_reserva', 'Servicio declara contraindicaciones y cliente marca una alerta.', 'servicio declara contraindicaciones y cliente marca una alerta', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-046 Restriccion medica/de seguridad', 'casuistica_pre_reserva', 'Tu caso requiere evaluacion antes de agendar.', 'tu caso requiere evaluacion antes de agendar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 047 datos ambiguos', 'PRE-RES-047 Datos ambiguos', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 047 datos ambiguos'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-047 Datos ambiguos', 'casuistica_pre_reserva', 'Datos ambiguos', 'datos ambiguos', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-047 Datos ambiguos', 'casuistica_pre_reserva', 'Cliente dice manana, tarde, otra sede o mismo profesional sin claridad.', 'cliente dice manana tarde otra sede o mismo profesional sin claridad', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-047 Datos ambiguos', 'casuistica_pre_reserva', 'Para confirmar, ¿que fecha exacta prefieres?', 'para confirmar que fecha exacta prefieres', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 048 cambio de intencion', 'PRE-RES-048 Cambio de intencion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 048 cambio de intencion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-048 Cambio de intencion', 'casuistica_pre_reserva', 'Cambio de intencion', 'cambio de intencion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-048 Cambio de intencion', 'casuistica_pre_reserva', 'Cliente estaba reservando y luego pide cancelar/reprogramar.', 'cliente estaba reservando y luego pide cancelar reprogramar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-048 Cambio de intencion', 'casuistica_pre_reserva', 'Perfecto, cambio el flujo. ¿Quieres cancelar o reprogramar?', 'perfecto cambio el flujo quieres cancelar o reprogramar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 049 cliente molesto', 'PRE-RES-049 Cliente molesto', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 049 cliente molesto'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-049 Cliente molesto', 'casuistica_pre_reserva', 'Cliente molesto', 'cliente molesto', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-049 Cliente molesto', 'casuistica_pre_reserva', 'Cliente usa reclamo, amenaza o malestar.', 'cliente usa reclamo amenaza o malestar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-049 Cliente molesto', 'casuistica_pre_reserva', 'Te derivare con una persona para revisar tu caso.', 'te derivare con una persona para revisar tu caso', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 050 datos sensibles innecesarios', 'PRE-RES-050 Datos sensibles innecesarios', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 050 datos sensibles innecesarios'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-050 Datos sensibles innecesarios', 'casuistica_pre_reserva', 'Datos sensibles innecesarios', 'datos sensibles innecesarios', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-050 Datos sensibles innecesarios', 'casuistica_pre_reserva', 'El flujo intenta pedir antecedentes no requeridos.', 'el flujo intenta pedir antecedentes no requeridos', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-050 Datos sensibles innecesarios', 'casuistica_pre_reserva', 'Solo necesito los datos necesarios para la reserva.', 'solo necesito los datos necesarios para la reserva', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 051 agenda externa no responde', 'PRE-RES-051 Agenda externa no responde', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 051 agenda externa no responde'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-051 Agenda externa no responde', 'casuistica_pre_reserva', 'Agenda externa no responde', 'agenda externa no responde', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-051 Agenda externa no responde', 'casuistica_pre_reserva', 'Calendario o base de datos no disponible.', 'calendario o base de datos no disponible', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-051 Agenda externa no responde', 'casuistica_pre_reserva', 'Ahora no puedo validar disponibilidad. Dejo tu solicitud para revision.', 'ahora no puedo validar disponibilidad dejo tu solicitud para revision', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 052 error al guardar', 'PRE-RES-052 Error al guardar', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 052 error al guardar'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-052 Error al guardar', 'casuistica_pre_reserva', 'Error al guardar', 'error al guardar', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-052 Error al guardar', 'casuistica_pre_reserva', 'Falla transaccion al persistir reserva.', 'falla transaccion al persistir reserva', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-052 Error al guardar', 'casuistica_pre_reserva', 'No pude confirmar aun. Recepcion revisara tu solicitud.', 'no pude confirmar aun recepcion revisara tu solicitud', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 053 reloj del servidor', 'PRE-RES-053 Reloj del servidor', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 053 reloj del servidor'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-053 Reloj del servidor', 'casuistica_pre_reserva', 'Reloj del servidor', 'reloj del servidor', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-053 Reloj del servidor', 'casuistica_pre_reserva', 'Diferencia de hora afecta fecha/hora actual.', 'diferencia de hora afecta fecha hora actual', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-053 Reloj del servidor', 'casuistica_pre_reserva', 'Validare el horario segun la hora oficial de la sucursal.', 'validare el horario segun la hora oficial de la sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 054 consentimiento comunicaciones', 'PRE-RES-054 Consentimiento comunicaciones', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 054 consentimiento comunicaciones'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-054 Consentimiento comunicaciones', 'casuistica_pre_reserva', 'Consentimiento comunicaciones', 'consentimiento comunicaciones', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-054 Consentimiento comunicaciones', 'casuistica_pre_reserva', 'Cliente no ha aceptado recibir mensajes automatizados cuando aplica.', 'cliente no ha aceptado recibir mensajes automatizados cuando aplica', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-054 Consentimiento comunicaciones', 'casuistica_pre_reserva', '¿Autorizas recibir confirmaciones y recordatorios por WhatsApp?', 'autorizas recibir confirmaciones y recordatorios por whatsapp', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 055 resumen previo incompleto', 'PRE-RES-055 Resumen previo incompleto', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 055 resumen previo incompleto'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-055 Resumen previo incompleto', 'casuistica_pre_reserva', 'Resumen previo incompleto', 'resumen previo incompleto', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-055 Resumen previo incompleto', 'casuistica_pre_reserva', 'Falta servicio, fecha, hora, sucursal o profesional en el resumen antes de confirmar.', 'falta servicio fecha hora sucursal o profesional en el resumen antes de confirmar', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-055 Resumen previo incompleto', 'casuistica_pre_reserva', 'Antes de reservar necesito confirmar servicio, fecha, hora y sucursal.', 'antes de reservar necesito confirmar servicio fecha hora y sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pre res 056 cliente no confirma', 'PRE-RES-056 Cliente no confirma', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pre res 056 cliente no confirma'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-056 Cliente no confirma', 'casuistica_pre_reserva', 'Cliente no confirma', 'cliente no confirma', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-056 Cliente no confirma', 'casuistica_pre_reserva', 'Cliente no responde al resumen final.', 'cliente no responde al resumen final', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRE-RES-056 Cliente no confirma', 'casuistica_pre_reserva', 'Te mantengo el horario por unos minutos. Responde CONFIRMAR para agendar.', 'te mantengo el horario por unos minutos responde confirmar para agendar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'id casuistica', 'ID Casuistica', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'id casuistica'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_post_reserva', 'Casuistica', 'casuistica', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_post_reserva', 'Condicion a validar', 'condicion a validar', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_post_reserva', 'Mensaje WhatsApp sugerido', 'mensaje whatsapp sugerido', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 001 reserva guardada', 'POST-RES-001 Reserva guardada', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 001 reserva guardada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-001 Reserva guardada', 'casuistica_post_reserva', 'Reserva guardada', 'reserva guardada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-001 Reserva guardada', 'casuistica_post_reserva', 'La reserva debe existir con identificador unico y estado correcto.', 'la reserva debe existir con identificador unico y estado correcto', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-001 Reserva guardada', 'casuistica_post_reserva', 'Tu reserva quedo registrada con el codigo indicado.', 'tu reserva quedo registrada con el codigo indicado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 002 estado inicial correcto', 'POST-RES-002 Estado inicial correcto', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 002 estado inicial correcto'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-002 Estado inicial correcto', 'casuistica_post_reserva', 'Estado inicial correcto', 'estado inicial correcto', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-002 Estado inicial correcto', 'casuistica_post_reserva', 'Estado debe ser Confirmada, Pendiente de pago o Pendiente de recepcion segun politica.', 'estado debe ser confirmada pendiente de pago o pendiente de recepcion segun politica', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-002 Estado inicial correcto', 'casuistica_post_reserva', 'Tu reserva quedo en estado pendiente de confirmacion.', 'tu reserva quedo en estado pendiente de confirmacion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 003 no duplicidad', 'POST-RES-003 No duplicidad', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 003 no duplicidad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-003 No duplicidad', 'casuistica_post_reserva', 'No duplicidad', 'no duplicidad', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-003 No duplicidad', 'casuistica_post_reserva', 'No debe existir otra reserva igual por mismo cliente, servicio y horario.', 'no debe existir otra reserva igual por mismo cliente servicio y horario', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-003 No duplicidad', 'casuistica_post_reserva', 'Detecte una reserva ya creada. Te envio el resumen existente.', 'detecte una reserva ya creada te envio el resumen existente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 004 slot ocupado', 'POST-RES-004 Slot ocupado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 004 slot ocupado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-004 Slot ocupado', 'casuistica_post_reserva', 'Slot ocupado', 'slot ocupado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-004 Slot ocupado', 'casuistica_post_reserva', 'El cupo debe quedar bloqueado/ocupado despues de confirmar.', 'el cupo debe quedar bloqueado ocupado despues de confirmar', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-004 Slot ocupado', 'casuistica_post_reserva', 'El horario quedo reservado para ti.', 'el horario quedo reservado para ti', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 005 liberar bloqueo temporal', 'POST-RES-005 Liberar bloqueo temporal', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 005 liberar bloqueo temporal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-005 Liberar bloqueo temporal', 'casuistica_post_reserva', 'Liberar bloqueo temporal', 'liberar bloqueo temporal', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-005 Liberar bloqueo temporal', 'casuistica_post_reserva', 'El bloqueo temporal debe transformarse en reserva o eliminarse.', 'el bloqueo temporal debe transformarse en reserva o eliminarse', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-005 Liberar bloqueo temporal', 'casuistica_post_reserva', 'Reserva confirmada; el bloqueo temporal fue cerrado.', 'reserva confirmada el bloqueo temporal fue cerrado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 006 agenda profesional actualizada', 'POST-RES-006 Agenda profesional actualizada', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 006 agenda profesional actualizada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-006 Agenda profesional actualizada', 'casuistica_post_reserva', 'Agenda profesional actualizada', 'agenda profesional actualizada', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-006 Agenda profesional actualizada', 'casuistica_post_reserva', 'La reserva debe aparecer en calendario del profesional.', 'la reserva debe aparecer en calendario del profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-006 Agenda profesional actualizada', 'casuistica_post_reserva', 'El profesional recibira la informacion de tu cita.', 'el profesional recibira la informacion de tu cita', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 007 agenda sucursal actualizada', 'POST-RES-007 Agenda sucursal actualizada', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 007 agenda sucursal actualizada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-007 Agenda sucursal actualizada', 'casuistica_post_reserva', 'Agenda sucursal actualizada', 'agenda sucursal actualizada', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-007 Agenda sucursal actualizada', 'casuistica_post_reserva', 'La ocupacion de la sede debe reflejar la nueva reserva.', 'la ocupacion de la sede debe reflejar la nueva reserva', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-007 Agenda sucursal actualizada', 'casuistica_post_reserva', 'La sucursal quedo informada.', 'la sucursal quedo informada', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 008 cabina equipo asignado', 'POST-RES-008 Cabina/equipo asignado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 008 cabina equipo asignado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-008 Cabina/equipo asignado', 'casuistica_post_reserva', 'Cabina/equipo asignado', 'cabina equipo asignado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-008 Cabina/equipo asignado', 'casuistica_post_reserva', 'El recurso fisico debe quedar reservado durante duracion + buffers.', 'el recurso fisico debe quedar reservado durante duracion buffers', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-008 Cabina/equipo asignado', 'casuistica_post_reserva', 'El recurso necesario quedo reservado.', 'el recurso necesario quedo reservado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 009 link de pago creado', 'POST-RES-009 Link de pago creado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 009 link de pago creado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-009 Link de pago creado', 'casuistica_post_reserva', 'Link de pago creado', 'link de pago creado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-009 Link de pago creado', 'casuistica_post_reserva', 'Si aplica abono, debe existir enlace, monto, expiracion y reserva asociada.', 'si aplica abono debe existir enlace monto expiracion y reserva asociada', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-009 Link de pago creado', 'casuistica_post_reserva', 'Para confirmar el pago usa este enlace.', 'para confirmar el pago usa este enlace', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 010 estado de pago coherente', 'POST-RES-010 Estado de pago coherente', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 010 estado de pago coherente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-010 Estado de pago coherente', 'casuistica_post_reserva', 'Estado de pago coherente', 'estado de pago coherente', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-010 Estado de pago coherente', 'casuistica_post_reserva', 'Reserva confirmada no debe depender de pago pendiente si regla exige pago previo.', 'reserva confirmada no debe depender de pago pendiente si regla exige pago previo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-010 Estado de pago coherente', 'casuistica_post_reserva', 'Tu hora queda pendiente hasta validar el abono.', 'tu hora queda pendiente hasta validar el abono', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 011 mensaje al cliente', 'POST-RES-011 Mensaje al cliente', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 011 mensaje al cliente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-011 Mensaje al cliente', 'casuistica_post_reserva', 'Mensaje al cliente', 'mensaje al cliente', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-011 Mensaje al cliente', 'casuistica_post_reserva', 'Debe enviarse resumen por WhatsApp con datos criticos.', 'debe enviarse resumen por whatsapp con datos criticos', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-011 Mensaje al cliente', 'casuistica_post_reserva', 'Te envio el resumen de tu reserva.', 'te envio el resumen de tu reserva', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 012 mensaje al profesional', 'POST-RES-012 Mensaje al profesional', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 012 mensaje al profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-012 Mensaje al profesional', 'casuistica_post_reserva', 'Mensaje al profesional', 'mensaje al profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-012 Mensaje al profesional', 'casuistica_post_reserva', 'Debe notificarse si la politica lo requiere.', 'debe notificarse si la politica lo requiere', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-012 Mensaje al profesional', 'casuistica_post_reserva', 'El equipo quedo notificado.', 'el equipo quedo notificado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 013 recordatorio programado', 'POST-RES-013 Recordatorio programado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 013 recordatorio programado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-013 Recordatorio programado', 'casuistica_post_reserva', 'Recordatorio programado', 'recordatorio programado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-013 Recordatorio programado', 'casuistica_post_reserva', 'Debe agendar recordatorios segun fecha/hora y politicas.', 'debe agendar recordatorios segun fecha hora y politicas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-013 Recordatorio programado', 'casuistica_post_reserva', 'Te recordaremos antes de tu cita.', 'te recordaremos antes de tu cita', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 014 zona horaria visible', 'POST-RES-014 Zona horaria visible', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 014 zona horaria visible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-014 Zona horaria visible', 'casuistica_post_reserva', 'Zona horaria visible', 'zona horaria visible', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-014 Zona horaria visible', 'casuistica_post_reserva', 'Fecha y hora en mensaje deben coincidir con zona de sucursal.', 'fecha y hora en mensaje deben coincidir con zona de sucursal', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-014 Zona horaria visible', 'casuistica_post_reserva', 'Tu cita es el dia indicado a la hora local de la sucursal.', 'tu cita es el dia indicado a la hora local de la sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 015 evento creado', 'POST-RES-015 Evento creado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 015 evento creado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-015 Evento creado', 'casuistica_post_reserva', 'Evento creado', 'evento creado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-015 Evento creado', 'casuistica_post_reserva', 'Debe existir evento ReservaCreada con datos previos y finales.', 'debe existir evento reservacreada con datos previos y finales', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-015 Evento creado', 'casuistica_post_reserva', 'Operacion registrada correctamente.', 'operacion registrada correctamente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 016 conversacion vinculada', 'POST-RES-016 Conversacion vinculada', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 016 conversacion vinculada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-016 Conversacion vinculada', 'casuistica_post_reserva', 'Conversacion vinculada', 'conversacion vinculada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-016 Conversacion vinculada', 'casuistica_post_reserva', 'La reserva debe quedar enlazada al hilo WhatsApp.', 'la reserva debe quedar enlazada al hilo whatsapp', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-016 Conversacion vinculada', 'casuistica_post_reserva', 'Tu solicitud quedo asociada a esta conversacion.', 'tu solicitud quedo asociada a esta conversacion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 017 calendario externo sincronizado', 'POST-RES-017 Calendario externo sincronizado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 017 calendario externo sincronizado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-017 Calendario externo sincronizado', 'casuistica_post_reserva', 'Calendario externo sincronizado', 'calendario externo sincronizado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-017 Calendario externo sincronizado', 'casuistica_post_reserva', 'Si existe Google Calendar u otro calendario, crear evento externo.', 'si existe google calendar u otro calendario crear evento externo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-017 Calendario externo sincronizado', 'casuistica_post_reserva', 'La cita se sincronizara con el calendario.', 'la cita se sincronizara con el calendario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 018 falla calendario externo', 'POST-RES-018 Falla calendario externo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 018 falla calendario externo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-018 Falla calendario externo', 'casuistica_post_reserva', 'Falla calendario externo', 'falla calendario externo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-018 Falla calendario externo', 'casuistica_post_reserva', 'Error no debe eliminar reserva interna confirmada si negocio lo permite.', 'error no debe eliminar reserva interna confirmada si negocio lo permite', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-018 Falla calendario externo', 'casuistica_post_reserva', 'Tu reserva esta registrada; sincronizacion interna pendiente.', 'tu reserva esta registrada sincronizacion interna pendiente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 019 conversion registrada', 'POST-RES-019 Conversion registrada', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 019 conversion registrada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-019 Conversion registrada', 'casuistica_post_reserva', 'Conversion registrada', 'conversion registrada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-019 Conversion registrada', 'casuistica_post_reserva', 'Registrar origen WhatsApp, campana, sucursal, profesional y servicio.', 'registrar origen whatsapp campana sucursal profesional y servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-019 Conversion registrada', 'casuistica_post_reserva', 'Registro interno actualizado.', 'registro interno actualizado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 020 resumen humano legible', 'POST-RES-020 Resumen humano legible', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 020 resumen humano legible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-020 Resumen humano legible', 'casuistica_post_reserva', 'Resumen humano legible', 'resumen humano legible', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-020 Resumen humano legible', 'casuistica_post_reserva', 'El resumen debe permitir detectar errores antes de la cita.', 'el resumen debe permitir detectar errores antes de la cita', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-020 Resumen humano legible', 'casuistica_post_reserva', 'Resumen: servicio, fecha, hora, profesional y direccion.', 'resumen servicio fecha hora profesional y direccion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 021 promover espera', 'POST-RES-021 Promover espera', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 021 promover espera'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-021 Promover espera', 'casuistica_post_reserva', 'Promover espera', 'promover espera', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-021 Promover espera', 'casuistica_post_reserva', 'Si se libero un cupo por transformacion, avisar a lista de espera cuando aplique.', 'si se libero un cupo por transformacion avisar a lista de espera cuando aplique', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-021 Promover espera', 'casuistica_post_reserva', 'Tenemos un horario disponible si deseas tomarlo.', 'tenemos un horario disponible si deseas tomarlo', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 022 tarea si pendiente', 'POST-RES-022 Tarea si pendiente', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 022 tarea si pendiente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-022 Tarea si pendiente', 'casuistica_post_reserva', 'Tarea si pendiente', 'tarea si pendiente', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-022 Tarea si pendiente', 'casuistica_post_reserva', 'Si falta pago, evaluacion o confirmacion humana, crear tarea.', 'si falta pago evaluacion o confirmacion humana crear tarea', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-022 Tarea si pendiente', 'casuistica_post_reserva', 'Recepcion revisara el pendiente.', 'recepcion revisara el pendiente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 023 permisos aplicados', 'POST-RES-023 Permisos aplicados', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 023 permisos aplicados'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-023 Permisos aplicados', 'casuistica_post_reserva', 'Permisos aplicados', 'permisos aplicados', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-023 Permisos aplicados', 'casuistica_post_reserva', 'Actor que creo reserva debe tener permiso para hacerlo.', 'actor que creo reserva debe tener permiso para hacerlo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-023 Permisos aplicados', 'casuistica_post_reserva', 'La accion fue validada correctamente.', 'la accion fue validada correctamente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 024 campos obligatorios', 'POST-RES-024 Campos obligatorios', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 024 campos obligatorios'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-024 Campos obligatorios', 'casuistica_post_reserva', 'Campos obligatorios', 'campos obligatorios', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-024 Campos obligatorios', 'casuistica_post_reserva', 'Reserva no debe quedar sin cliente, servicio, sucursal, fecha, hora o estado.', 'reserva no debe quedar sin cliente servicio sucursal fecha hora o estado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-024 Campos obligatorios', 'casuistica_post_reserva', 'La reserva quedo completa.', 'la reserva quedo completa', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 025 precio congelado', 'POST-RES-025 Precio congelado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 025 precio congelado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-025 Precio congelado', 'casuistica_post_reserva', 'Precio congelado', 'precio congelado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-025 Precio congelado', 'casuistica_post_reserva', 'Si hay precio, guardar monto aplicado para evitar cambios posteriores no trazados.', 'si hay precio guardar monto aplicado para evitar cambios posteriores no trazados', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-025 Precio congelado', 'casuistica_post_reserva', 'El valor de la reserva quedo registrado.', 'el valor de la reserva quedo registrado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 026 politica congelada', 'POST-RES-026 Politica congelada', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 026 politica congelada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-026 Politica congelada', 'casuistica_post_reserva', 'Politica congelada', 'politica congelada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-026 Politica congelada', 'casuistica_post_reserva', 'Guardar politica aplicada al momento de reservar.', 'guardar politica aplicada al momento de reservar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-026 Politica congelada', 'casuistica_post_reserva', 'Las condiciones aplicadas quedaron registradas.', 'las condiciones aplicadas quedaron registradas', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'post res 027 siguiente accion clara', 'POST-RES-027 Siguiente accion clara', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'post res 027 siguiente accion clara'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-027 Siguiente accion clara', 'casuistica_post_reserva', 'Siguiente accion clara', 'siguiente accion clara', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-027 Siguiente accion clara', 'casuistica_post_reserva', 'Cliente debe saber si debe pagar, asistir, confirmar o esperar contacto.', 'cliente debe saber si debe pagar asistir confirmar o esperar contacto', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'POST-RES-027 Siguiente accion clara', 'casuistica_post_reserva', 'Tu siguiente paso es el indicado en el resumen.', 'tu siguiente paso es el indicado en el resumen', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'id casuistica', 'ID Casuistica', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'id casuistica'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_reprogramar', 'Casuistica', 'casuistica', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_reprogramar', 'Condicion a validar', 'condicion a validar', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_reprogramar', 'Mensaje WhatsApp sugerido', 'mensaje whatsapp sugerido', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 001 reserva no existe', 'REP-001 Reserva no existe', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 001 reserva no existe'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-001 Reserva no existe', 'casuistica_reprogramar', 'Reserva no existe', 'reserva no existe', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-001 Reserva no existe', 'casuistica_reprogramar', 'No se encuentra reserva por codigo, telefono o fecha.', 'no se encuentra reserva por codigo telefono o fecha', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-001 Reserva no existe', 'casuistica_reprogramar', 'No encuentro esa reserva. Enviame fecha o codigo.', 'no encuentro esa reserva enviame fecha o codigo', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 002 estado no reprogramable', 'REP-002 Estado no reprogramable', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 002 estado no reprogramable'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-002 Estado no reprogramable', 'casuistica_reprogramar', 'Estado no reprogramable', 'estado no reprogramable', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-002 Estado no reprogramable', 'casuistica_reprogramar', 'Reserva esta cancelada, atendida, vencida, no asistida o en disputa.', 'reserva esta cancelada atendida vencida no asistida o en disputa', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-002 Estado no reprogramable', 'casuistica_reprogramar', 'Esa reserva no permite reprogramacion automatica.', 'esa reserva no permite reprogramacion automatica', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 003 reserva pendiente de pago', 'REP-003 Reserva pendiente de pago', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 003 reserva pendiente de pago'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-003 Reserva pendiente de pago', 'casuistica_reprogramar', 'Reserva pendiente de pago', 'reserva pendiente de pago', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-003 Reserva pendiente de pago', 'casuistica_reprogramar', 'La reserva requiere pago y aun no esta confirmado.', 'la reserva requiere pago y aun no esta confirmado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-003 Reserva pendiente de pago', 'casuistica_reprogramar', 'Primero debemos validar el pago pendiente.', 'primero debemos validar el pago pendiente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 004 fuera de plazo', 'REP-004 Fuera de plazo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 004 fuera de plazo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-004 Fuera de plazo', 'casuistica_reprogramar', 'Fuera de plazo', 'fuera de plazo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-004 Fuera de plazo', 'casuistica_reprogramar', 'Cliente solicita cambio dentro de ventana prohibida.', 'cliente solicita cambio dentro de ventana prohibida', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-004 Fuera de plazo', 'casuistica_reprogramar', 'La reprogramacion esta fuera del plazo permitido.', 'la reprogramacion esta fuera del plazo permitido', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 005 exceso de reprogramaciones', 'REP-005 Exceso de reprogramaciones', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 005 exceso de reprogramaciones'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-005 Exceso de reprogramaciones', 'casuistica_reprogramar', 'Exceso de reprogramaciones', 'exceso de reprogramaciones', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-005 Exceso de reprogramaciones', 'casuistica_reprogramar', 'Reserva supera cantidad maxima de cambios.', 'reserva supera cantidad maxima de cambios', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-005 Exceso de reprogramaciones', 'casuistica_reprogramar', 'Esta reserva ya alcanzo el maximo de cambios permitidos.', 'esta reserva ya alcanzo el maximo de cambios permitidos', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 006 motivo obligatorio', 'REP-006 Motivo obligatorio', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 006 motivo obligatorio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-006 Motivo obligatorio', 'casuistica_reprogramar', 'Motivo obligatorio', 'motivo obligatorio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-006 Motivo obligatorio', 'casuistica_reprogramar', 'Negocio exige motivo para reprogramar.', 'negocio exige motivo para reprogramar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-006 Motivo obligatorio', 'casuistica_reprogramar', 'Indicanos el motivo para registrar el cambio.', 'indicanos el motivo para registrar el cambio', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 007 titular no coincide', 'REP-007 Titular no coincide', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 007 titular no coincide'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-007 Titular no coincide', 'casuistica_reprogramar', 'Titular no coincide', 'titular no coincide', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-007 Titular no coincide', 'casuistica_reprogramar', 'Quien solicita no coincide con titular o canal autorizado.', 'quien solicita no coincide con titular o canal autorizado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-007 Titular no coincide', 'casuistica_reprogramar', 'Necesito validar que eres el titular de la reserva.', 'necesito validar que eres el titular de la reserva', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 008 cliente bloqueado', 'REP-008 Cliente bloqueado', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 008 cliente bloqueado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-008 Cliente bloqueado', 'casuistica_reprogramar', 'Cliente bloqueado', 'cliente bloqueado', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-008 Cliente bloqueado', 'casuistica_reprogramar', 'Cliente quedo bloqueado despues de reservar.', 'cliente quedo bloqueado despues de reservar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-008 Cliente bloqueado', 'casuistica_reprogramar', 'Tu caso requiere revision de recepcion.', 'tu caso requiere revision de recepcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 009 nueva fecha pasada', 'REP-009 Nueva fecha pasada', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 009 nueva fecha pasada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-009 Nueva fecha pasada', 'casuistica_reprogramar', 'Nueva fecha pasada', 'nueva fecha pasada', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-009 Nueva fecha pasada', 'casuistica_reprogramar', 'Nueva fecha/hora es anterior al momento actual.', 'nueva fecha hora es anterior al momento actual', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-009 Nueva fecha pasada', 'casuistica_reprogramar', 'No puedo mover la cita a una fecha pasada.', 'no puedo mover la cita a una fecha pasada', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 010 nueva hora fuera de agenda', 'REP-010 Nueva hora fuera de agenda', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 010 nueva hora fuera de agenda'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-010 Nueva hora fuera de agenda', 'casuistica_reprogramar', 'Nueva hora fuera de agenda', 'nueva hora fuera de agenda', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-010 Nueva hora fuera de agenda', 'casuistica_reprogramar', 'Nueva fecha/hora cae fuera de agenda publicada.', 'nueva fecha hora cae fuera de agenda publicada', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-010 Nueva hora fuera de agenda', 'casuistica_reprogramar', 'Ese horario no esta disponible para reprogramar.', 'ese horario no esta disponible para reprogramar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 011 nuevo slot ocupado', 'REP-011 Nuevo slot ocupado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 011 nuevo slot ocupado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-011 Nuevo slot ocupado', 'casuistica_reprogramar', 'Nuevo slot ocupado', 'nuevo slot ocupado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-011 Nuevo slot ocupado', 'casuistica_reprogramar', 'Nuevo horario se cruza con otra reserva o bloqueo.', 'nuevo horario se cruza con otra reserva o bloqueo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-011 Nuevo slot ocupado', 'casuistica_reprogramar', 'Ese horario esta ocupado. Te muestro opciones.', 'ese horario esta ocupado te muestro opciones', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 012 duracion no cabe', 'REP-012 Duracion no cabe', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 012 duracion no cabe'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-012 Duracion no cabe', 'casuistica_reprogramar', 'Duracion no cabe', 'duracion no cabe', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-012 Duracion no cabe', 'casuistica_reprogramar', 'Servicio original no cabe en el nuevo horario.', 'servicio original no cabe en el nuevo horario', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-012 Duracion no cabe', 'casuistica_reprogramar', 'Ese horario no alcanza para completar el servicio.', 'ese horario no alcanza para completar el servicio', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 013 cambio de servicio', 'REP-013 Cambio de servicio', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 013 cambio de servicio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-013 Cambio de servicio', 'casuistica_reprogramar', 'Cambio de servicio', 'cambio de servicio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-013 Cambio de servicio', 'casuistica_reprogramar', 'Cliente ademas quiere cambiar servicio.', 'cliente ademas quiere cambiar servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-013 Cambio de servicio', 'casuistica_reprogramar', 'Confirmemos el nuevo servicio antes de mover la cita.', 'confirmemos el nuevo servicio antes de mover la cita', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 014 servicio ya no disponible', 'REP-014 Servicio ya no disponible', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 014 servicio ya no disponible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-014 Servicio ya no disponible', 'casuistica_reprogramar', 'Servicio ya no disponible', 'servicio ya no disponible', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-014 Servicio ya no disponible', 'casuistica_reprogramar', 'Servicio original fue desactivado desde la reserva.', 'servicio original fue desactivado desde la reserva', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-014 Servicio ya no disponible', 'casuistica_reprogramar', 'Ese servicio requiere revision antes de mover la cita.', 'ese servicio requiere revision antes de mover la cita', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 015 cambio de sucursal', 'REP-015 Cambio de sucursal', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 015 cambio de sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-015 Cambio de sucursal', 'casuistica_reprogramar', 'Cambio de sucursal', 'cambio de sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-015 Cambio de sucursal', 'casuistica_reprogramar', 'Cliente desea mover a otra sede.', 'cliente desea mover a otra sede', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-015 Cambio de sucursal', 'casuistica_reprogramar', 'Validare disponibilidad en la nueva sucursal.', 'validare disponibilidad en la nueva sucursal', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 016 nueva sede sin servicio', 'REP-016 Nueva sede sin servicio', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 016 nueva sede sin servicio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-016 Nueva sede sin servicio', 'casuistica_reprogramar', 'Nueva sede sin servicio', 'nueva sede sin servicio', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-016 Nueva sede sin servicio', 'casuistica_reprogramar', 'Servicio no existe en la sede destino.', 'servicio no existe en la sede destino', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-016 Nueva sede sin servicio', 'casuistica_reprogramar', 'Ese servicio no esta disponible en la sede seleccionada.', 'ese servicio no esta disponible en la sede seleccionada', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 017 traslado profesional', 'REP-017 Traslado profesional', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 017 traslado profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-017 Traslado profesional', 'casuistica_reprogramar', 'Traslado profesional', 'traslado profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-017 Traslado profesional', 'casuistica_reprogramar', 'Profesional original no puede atender en nueva sede o no alcanza traslado.', 'profesional original no puede atender en nueva sede o no alcanza traslado', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-017 Traslado profesional', 'casuistica_reprogramar', 'El profesional no atiende ese horario en esa sede.', 'el profesional no atiende ese horario en esa sede', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 018 cambio de profesional', 'REP-018 Cambio de profesional', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 018 cambio de profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-018 Cambio de profesional', 'casuistica_reprogramar', 'Cambio de profesional', 'cambio de profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-018 Cambio de profesional', 'casuistica_reprogramar', 'Cliente acepta o solicita otro profesional.', 'cliente acepta o solicita otro profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-018 Cambio de profesional', 'casuistica_reprogramar', '¿Confirmas cambiar de profesional?', 'confirmas cambiar de profesional', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 019 profesional original no disponible', 'REP-019 Profesional original no disponible', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 019 profesional original no disponible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-019 Profesional original no disponible', 'casuistica_reprogramar', 'Profesional original no disponible', 'profesional original no disponible', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-019 Profesional original no disponible', 'casuistica_reprogramar', 'El profesional original no esta libre en nuevo horario.', 'el profesional original no esta libre en nuevo horario', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-019 Profesional original no disponible', 'casuistica_reprogramar', 'Tu profesional no tiene ese horario. Te muestro alternativas.', 'tu profesional no tiene ese horario te muestro alternativas', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 020 recurso no disponible', 'REP-020 Recurso no disponible', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 020 recurso no disponible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-020 Recurso no disponible', 'casuistica_reprogramar', 'Recurso no disponible', 'recurso no disponible', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-020 Recurso no disponible', 'casuistica_reprogramar', 'Cabina/equipo no esta libre en nuevo horario.', 'cabina equipo no esta libre en nuevo horario', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-020 Recurso no disponible', 'casuistica_reprogramar', 'El recurso requerido no esta disponible.', 'el recurso requerido no esta disponible', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 021 diferencia a favor', 'REP-021 Diferencia a favor', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 021 diferencia a favor'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-021 Diferencia a favor', 'casuistica_reprogramar', 'Diferencia a favor', 'diferencia a favor', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-021 Diferencia a favor', 'casuistica_reprogramar', 'Nuevo horario/servicio/sede tiene menor valor.', 'nuevo horario servicio sede tiene menor valor', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-021 Diferencia a favor', 'casuistica_reprogramar', 'Existe una diferencia a favor que recepcion gestionara.', 'existe una diferencia a favor que recepcion gestionara', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 022 diferencia por pagar', 'REP-022 Diferencia por pagar', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 022 diferencia por pagar'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-022 Diferencia por pagar', 'casuistica_reprogramar', 'Diferencia por pagar', 'diferencia por pagar', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-022 Diferencia por pagar', 'casuistica_reprogramar', 'Nuevo horario/servicio/sede tiene mayor valor.', 'nuevo horario servicio sede tiene mayor valor', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-022 Diferencia por pagar', 'casuistica_reprogramar', 'Para confirmar el cambio debes pagar la diferencia.', 'para confirmar el cambio debes pagar la diferencia', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 023 abono no transferible', 'REP-023 Abono no transferible', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 023 abono no transferible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-023 Abono no transferible', 'casuistica_reprogramar', 'Abono no transferible', 'abono no transferible', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-023 Abono no transferible', 'casuistica_reprogramar', 'Abono no puede moverse segun politica.', 'abono no puede moverse segun politica', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-023 Abono no transferible', 'casuistica_reprogramar', 'Este abono requiere revision para reprogramar.', 'este abono requiere revision para reprogramar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 024 mantener original hasta confirmar nuevo', 'REP-024 Mantener original hasta confirmar nuevo', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 024 mantener original hasta confirmar nuevo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-024 Mantener original hasta confirmar nuevo', 'casuistica_reprogramar', 'Mantener original hasta confirmar nuevo', 'mantener original hasta confirmar nuevo', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-024 Mantener original hasta confirmar nuevo', 'casuistica_reprogramar', 'La cita original no debe liberarse antes de bloquear la nueva.', 'la cita original no debe liberarse antes de bloquear la nueva', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-024 Mantener original hasta confirmar nuevo', 'casuistica_reprogramar', 'Estoy validando el nuevo horario antes de liberar el anterior.', 'estoy validando el nuevo horario antes de liberar el anterior', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 025 cambio atomico', 'REP-025 Cambio atomico', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 025 cambio atomico'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-025 Cambio atomico', 'casuistica_reprogramar', 'Cambio atomico', 'cambio atomico', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-025 Cambio atomico', 'casuistica_reprogramar', 'Liberar original y ocupar nuevo debe ocurrir como una sola operacion.', 'liberar original y ocupar nuevo debe ocurrir como una sola operacion', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-025 Cambio atomico', 'casuistica_reprogramar', 'El cambio quedo confirmado correctamente.', 'el cambio quedo confirmado correctamente', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 026 resumen comparativo', 'REP-026 Resumen comparativo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 026 resumen comparativo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-026 Resumen comparativo', 'casuistica_reprogramar', 'Resumen comparativo', 'resumen comparativo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-026 Resumen comparativo', 'casuistica_reprogramar', 'Cliente debe ver antes y despues de la reprogramacion.', 'cliente debe ver antes y despues de la reprogramacion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-026 Resumen comparativo', 'casuistica_reprogramar', 'Confirmas cambiar de [fecha anterior] a [nueva fecha]?', 'confirmas cambiar de fecha anterior a nueva fecha', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 027 cancelar recordatorio anterior', 'REP-027 Cancelar recordatorio anterior', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 027 cancelar recordatorio anterior'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-027 Cancelar recordatorio anterior', 'casuistica_reprogramar', 'Cancelar recordatorio anterior', 'cancelar recordatorio anterior', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-027 Cancelar recordatorio anterior', 'casuistica_reprogramar', 'Recordatorios anteriores no deben enviarse despues del cambio.', 'recordatorios anteriores no deben enviarse despues del cambio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-027 Cancelar recordatorio anterior', 'casuistica_reprogramar', 'Actualice los recordatorios de tu nueva cita.', 'actualice los recordatorios de tu nueva cita', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 028 crear nuevos recordatorios', 'REP-028 Crear nuevos recordatorios', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 028 crear nuevos recordatorios'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-028 Crear nuevos recordatorios', 'casuistica_reprogramar', 'Crear nuevos recordatorios', 'crear nuevos recordatorios', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-028 Crear nuevos recordatorios', 'casuistica_reprogramar', 'Nueva fecha requiere recordatorios recalculados.', 'nueva fecha requiere recordatorios recalculados', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-028 Crear nuevos recordatorios', 'casuistica_reprogramar', 'Te recordaremos en la nueva fecha.', 'te recordaremos en la nueva fecha', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 029 evento reprogramacion', 'REP-029 Evento reprogramacion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 029 evento reprogramacion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-029 Evento reprogramacion', 'casuistica_reprogramar', 'Evento reprogramacion', 'evento reprogramacion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-029 Evento reprogramacion', 'casuistica_reprogramar', 'Debe guardar estado anterior y nuevo.', 'debe guardar estado anterior y nuevo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-029 Evento reprogramacion', 'casuistica_reprogramar', 'El cambio quedo registrado.', 'el cambio quedo registrado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'rep 030 actualizar calendario externo', 'REP-030 Actualizar calendario externo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'rep 030 actualizar calendario externo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-030 Actualizar calendario externo', 'casuistica_reprogramar', 'Actualizar calendario externo', 'actualizar calendario externo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-030 Actualizar calendario externo', 'casuistica_reprogramar', 'Evento externo debe moverse, no duplicarse.', 'evento externo debe moverse no duplicarse', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'REP-030 Actualizar calendario externo', 'casuistica_reprogramar', 'La cita fue actualizada en calendario.', 'la cita fue actualizada en calendario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'id casuistica', 'ID Casuistica', 60, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'id casuistica'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_cancelar', 'Casuistica', 'casuistica', 'es', 'CL', 'PREFERRED', 0.85, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_cancelar', 'Condicion a validar', 'condicion a validar', 'es', 'CL', 'SYNONYM', 0.8, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'ID Casuistica', 'casuistica_cancelar', 'Mensaje WhatsApp sugerido', 'mensaje whatsapp sugerido', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 60, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 001 reserva no existe', 'CAN-001 Reserva no existe', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 001 reserva no existe'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-001 Reserva no existe', 'casuistica_cancelar', 'Reserva no existe', 'reserva no existe', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-001 Reserva no existe', 'casuistica_cancelar', 'No se encuentra reserva por codigo, telefono o fecha.', 'no se encuentra reserva por codigo telefono o fecha', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-001 Reserva no existe', 'casuistica_cancelar', 'No encuentro esa reserva. Enviame el codigo o fecha.', 'no encuentro esa reserva enviame el codigo o fecha', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 002 estado no cancelable', 'CAN-002 Estado no cancelable', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 002 estado no cancelable'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-002 Estado no cancelable', 'casuistica_cancelar', 'Estado no cancelable', 'estado no cancelable', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-002 Estado no cancelable', 'casuistica_cancelar', 'Reserva ya esta cancelada, atendida, vencida o no asistida.', 'reserva ya esta cancelada atendida vencida o no asistida', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-002 Estado no cancelable', 'casuistica_cancelar', 'Esa reserva ya no permite cancelacion automatica.', 'esa reserva ya no permite cancelacion automatica', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 003 reserva de otro titular', 'CAN-003 Reserva de otro titular', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 003 reserva de otro titular'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-003 Reserva de otro titular', 'casuistica_cancelar', 'Reserva de otro titular', 'reserva de otro titular', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-003 Reserva de otro titular', 'casuistica_cancelar', 'El solicitante no coincide con titular o canal autorizado.', 'el solicitante no coincide con titular o canal autorizado', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-003 Reserva de otro titular', 'casuistica_cancelar', 'Necesito validar que eres el titular de la reserva.', 'necesito validar que eres el titular de la reserva', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 004 fuera de plazo', 'CAN-004 Fuera de plazo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 004 fuera de plazo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-004 Fuera de plazo', 'casuistica_cancelar', 'Fuera de plazo', 'fuera de plazo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-004 Fuera de plazo', 'casuistica_cancelar', 'Cancelacion cae dentro de ventana con penalizacion o no permitida.', 'cancelacion cae dentro de ventana con penalizacion o no permitida', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-004 Fuera de plazo', 'casuistica_cancelar', 'La cancelacion esta fuera del plazo permitido.', 'la cancelacion esta fuera del plazo permitido', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 005 motivo obligatorio', 'CAN-005 Motivo obligatorio', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 005 motivo obligatorio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-005 Motivo obligatorio', 'casuistica_cancelar', 'Motivo obligatorio', 'motivo obligatorio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-005 Motivo obligatorio', 'casuistica_cancelar', 'Negocio exige motivo de cancelacion.', 'negocio exige motivo de cancelacion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-005 Motivo obligatorio', 'casuistica_cancelar', 'Indicanos el motivo para registrar la cancelacion.', 'indicanos el motivo para registrar la cancelacion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 006 cancelaciones reiteradas', 'CAN-006 Cancelaciones reiteradas', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 006 cancelaciones reiteradas'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-006 Cancelaciones reiteradas', 'casuistica_cancelar', 'Cancelaciones reiteradas', 'cancelaciones reiteradas', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-006 Cancelaciones reiteradas', 'casuistica_cancelar', 'Cliente supera umbral de cancelaciones.', 'cliente supera umbral de cancelaciones', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-006 Cancelaciones reiteradas', 'casuistica_cancelar', 'Tu caso requiere revision por historial de cancelaciones.', 'tu caso requiere revision por historial de cancelaciones', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 007 pago no realizado', 'CAN-007 Pago no realizado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 007 pago no realizado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-007 Pago no realizado', 'casuistica_cancelar', 'Pago no realizado', 'pago no realizado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-007 Pago no realizado', 'casuistica_cancelar', 'Reserva pendiente de pago y cliente cancela.', 'reserva pendiente de pago y cliente cancela', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-007 Pago no realizado', 'casuistica_cancelar', 'Tu reserva pendiente fue cancelada.', 'tu reserva pendiente fue cancelada', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 008 pago confirmado', 'CAN-008 Pago confirmado', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 008 pago confirmado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-008 Pago confirmado', 'casuistica_cancelar', 'Pago confirmado', 'pago confirmado', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-008 Pago confirmado', 'casuistica_cancelar', 'Reserva pagada requiere devolucion, saldo a favor o penalizacion.', 'reserva pagada requiere devolucion saldo a favor o penalizacion', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-008 Pago confirmado', 'casuistica_cancelar', 'Validaremos la politica de devolucion de tu pago.', 'validaremos la politica de devolucion de tu pago', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 009 abono no reembolsable', 'CAN-009 Abono no reembolsable', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 009 abono no reembolsable'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-009 Abono no reembolsable', 'casuistica_cancelar', 'Abono no reembolsable', 'abono no reembolsable', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-009 Abono no reembolsable', 'casuistica_cancelar', 'Politica indica abono no reembolsable dentro de cierto plazo.', 'politica indica abono no reembolsable dentro de cierto plazo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-009 Abono no reembolsable', 'casuistica_cancelar', 'Esta cancelacion puede perder el abono. ¿Confirmas?', 'esta cancelacion puede perder el abono confirmas', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 010 devolucion parcial', 'CAN-010 Devolucion parcial', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 010 devolucion parcial'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-010 Devolucion parcial', 'casuistica_cancelar', 'Devolucion parcial', 'devolucion parcial', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-010 Devolucion parcial', 'casuistica_cancelar', 'Corresponde devolver solo parte por comision o plazo.', 'corresponde devolver solo parte por comision o plazo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-010 Devolucion parcial', 'casuistica_cancelar', 'Recepcion validara el monto de devolucion.', 'recepcion validara el monto de devolucion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 011 contracargo disputa', 'CAN-011 Contracargo/disputa', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 011 contracargo disputa'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-011 Contracargo/disputa', 'casuistica_cancelar', 'Contracargo/disputa', 'contracargo disputa', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-011 Contracargo/disputa', 'casuistica_cancelar', 'Pago esta en disputa o contracargo.', 'pago esta en disputa o contracargo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-011 Contracargo/disputa', 'casuistica_cancelar', 'Tu pago requiere revision administrativa.', 'tu pago requiere revision administrativa', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 012 liberar slot', 'CAN-012 Liberar slot', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 012 liberar slot'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-012 Liberar slot', 'casuistica_cancelar', 'Liberar slot', 'liberar slot', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-012 Liberar slot', 'casuistica_cancelar', 'Al cancelar debe liberarse profesional, recurso y capacidad.', 'al cancelar debe liberarse profesional recurso y capacidad', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-012 Liberar slot', 'casuistica_cancelar', 'El horario fue liberado.', 'el horario fue liberado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 013 no liberar si cancelacion falla', 'CAN-013 No liberar si cancelacion falla', 80, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 013 no liberar si cancelacion falla'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-013 No liberar si cancelacion falla', 'casuistica_cancelar', 'No liberar si cancelacion falla', 'no liberar si cancelacion falla', 'es', 'CL', 'PREFERRED', 0.85, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-013 No liberar si cancelacion falla', 'casuistica_cancelar', 'Si no se pudo cambiar estado, no liberar cupo.', 'si no se pudo cambiar estado no liberar cupo', 'es', 'CL', 'SYNONYM', 0.8, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-013 No liberar si cancelacion falla', 'casuistica_cancelar', 'No pude cancelar aun; recepcion revisara.', 'no pude cancelar aun recepcion revisara', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 80, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 014 avisar lista de espera', 'CAN-014 Avisar lista de espera', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 014 avisar lista de espera'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-014 Avisar lista de espera', 'casuistica_cancelar', 'Avisar lista de espera', 'avisar lista de espera', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-014 Avisar lista de espera', 'casuistica_cancelar', 'Al liberar un cupo, puede existir cliente esperando.', 'al liberar un cupo puede existir cliente esperando', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-014 Avisar lista de espera', 'casuistica_cancelar', 'Se aviso a lista de espera si corresponde.', 'se aviso a lista de espera si corresponde', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 015 confirmacion al cliente', 'CAN-015 Confirmacion al cliente', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 015 confirmacion al cliente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-015 Confirmacion al cliente', 'casuistica_cancelar', 'Confirmacion al cliente', 'confirmacion al cliente', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-015 Confirmacion al cliente', 'casuistica_cancelar', 'Cliente debe recibir confirmacion de cancelacion y efecto sobre pago.', 'cliente debe recibir confirmacion de cancelacion y efecto sobre pago', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-015 Confirmacion al cliente', 'casuistica_cancelar', 'Tu reserva fue cancelada. Te envio el detalle.', 'tu reserva fue cancelada te envio el detalle', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 016 aviso al profesional', 'CAN-016 Aviso al profesional', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 016 aviso al profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-016 Aviso al profesional', 'casuistica_cancelar', 'Aviso al profesional', 'aviso al profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-016 Aviso al profesional', 'casuistica_cancelar', 'Profesional debe ser notificado para evitar preparacion innecesaria.', 'profesional debe ser notificado para evitar preparacion innecesaria', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-016 Aviso al profesional', 'casuistica_cancelar', 'El profesional fue notificado.', 'el profesional fue notificado', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 017 anular recordatorios', 'CAN-017 Anular recordatorios', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 017 anular recordatorios'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-017 Anular recordatorios', 'casuistica_cancelar', 'Anular recordatorios', 'anular recordatorios', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-017 Anular recordatorios', 'casuistica_cancelar', 'Recordatorios futuros de la cita cancelada deben anularse.', 'recordatorios futuros de la cita cancelada deben anularse', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-017 Anular recordatorios', 'casuistica_cancelar', 'Se anularon los recordatorios de la cita.', 'se anularon los recordatorios de la cita', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 018 cancelar evento externo', 'CAN-018 Cancelar evento externo', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 018 cancelar evento externo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-018 Cancelar evento externo', 'casuistica_cancelar', 'Cancelar evento externo', 'cancelar evento externo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-018 Cancelar evento externo', 'casuistica_cancelar', 'Evento externo debe cancelarse o actualizarse.', 'evento externo debe cancelarse o actualizarse', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-018 Cancelar evento externo', 'casuistica_cancelar', 'La cancelacion se sincronizara con calendario.', 'la cancelacion se sincronizara con calendario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 019 evento de cancelacion', 'CAN-019 Evento de cancelacion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 019 evento de cancelacion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-019 Evento de cancelacion', 'casuistica_cancelar', 'Evento de cancelacion', 'evento de cancelacion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-019 Evento de cancelacion', 'casuistica_cancelar', 'Guardar quien cancelo, motivo, politica y estado anterior/nuevo.', 'guardar quien cancelo motivo politica y estado anterior nuevo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-019 Evento de cancelacion', 'casuistica_cancelar', 'La cancelacion quedo registrada.', 'la cancelacion quedo registrada', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 020 ofrecer reprogramar', 'CAN-020 Ofrecer reprogramar', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 020 ofrecer reprogramar'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-020 Ofrecer reprogramar', 'casuistica_cancelar', 'Ofrecer reprogramar', 'ofrecer reprogramar', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-020 Ofrecer reprogramar', 'casuistica_cancelar', 'Antes o despues de cancelar, se puede ofrecer reprogramacion si aplica.', 'antes o despues de cancelar se puede ofrecer reprogramacion si aplica', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-020 Ofrecer reprogramar', 'casuistica_cancelar', 'Si prefieres, tambien puedo ayudarte a reprogramar.', 'si prefieres tambien puedo ayudarte a reprogramar', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 021 cita cercana', 'CAN-021 Cita cercana', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 021 cita cercana'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-021 Cita cercana', 'casuistica_cancelar', 'Cita cercana', 'cita cercana', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-021 Cita cercana', 'casuistica_cancelar', 'Cancelacion muy cercana puede requerir llamada humana.', 'cancelacion muy cercana puede requerir llamada humana', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-021 Cita cercana', 'casuistica_cancelar', 'Por la cercania de la hora, te derivo con recepcion.', 'por la cercania de la hora te derivo con recepcion', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 022 servicio con preparacion', 'CAN-022 Servicio con preparacion', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 022 servicio con preparacion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-022 Servicio con preparacion', 'casuistica_cancelar', 'Servicio con preparacion', 'servicio con preparacion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-022 Servicio con preparacion', 'casuistica_cancelar', 'Si ya hubo preparacion de insumos, puede aplicar costo.', 'si ya hubo preparacion de insumos puede aplicar costo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-022 Servicio con preparacion', 'casuistica_cancelar', 'Este servicio requiere revision por preparacion previa.', 'este servicio requiere revision por preparacion previa', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 023 paquete o sesiones', 'CAN-023 Paquete o sesiones', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 023 paquete o sesiones'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-023 Paquete o sesiones', 'casuistica_cancelar', 'Paquete o sesiones', 'paquete o sesiones', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-023 Paquete o sesiones', 'casuistica_cancelar', 'Reserva pertenece a paquete de varias sesiones.', 'reserva pertenece a paquete de varias sesiones', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-023 Paquete o sesiones', 'casuistica_cancelar', 'Actualizaremos el estado de tu paquete.', 'actualizaremos el estado de tu paquete', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'can 024 reserva grupal', 'CAN-024 Reserva grupal', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'can 024 reserva grupal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-024 Reserva grupal', 'casuistica_cancelar', 'Reserva grupal', 'reserva grupal', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-024 Reserva grupal', 'casuistica_cancelar', 'Cancelacion afecta cupo grupal o total del grupo.', 'cancelacion afecta cupo grupal o total del grupo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'CAN-024 Reserva grupal', 'casuistica_cancelar', 'Cancelare solo tu cupo, salvo que indiques lo contrario.', 'cancelare solo tu cupo salvo que indiques lo contrario', 'es', 'CL', 'CONTEXTUAL', 0.7, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 001 debe estar activo y visible para agenda', 'PRO-001 Debe estar activo y visible para agenda.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 001 debe estar activo y visible para agenda'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-001 Debe estar activo y visible para agenda.', 'regla_profesional', 'Debe estar activo y visible para agenda.', 'debe estar activo y visible para agenda', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-001 Debe estar activo y visible para agenda.', 'regla_profesional', 'Excluir profesionales inactivos.', 'excluir profesionales inactivos', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 002 debe estar autorizado para atender en la fecha', 'PRO-002 Debe estar autorizado para atender en la fecha.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 002 debe estar autorizado para atender en la fecha'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-002 Debe estar autorizado para atender en la fecha.', 'regla_profesional', 'Debe estar autorizado para atender en la fecha.', 'debe estar autorizado para atender en la fecha', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-002 Debe estar autorizado para atender en la fecha.', 'regla_profesional', 'Bloquear si termino vigencia.', 'bloquear si termino vigencia', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 003 debe tener permiso certificacion para el servicio', 'PRO-003 Debe tener permiso/certificacion para el servicio.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 003 debe tener permiso certificacion para el servicio'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-003 Debe tener permiso/certificacion para el servicio.', 'regla_profesional', 'Debe tener permiso/certificacion para el servicio.', 'debe tener permiso certificacion para el servicio', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-003 Debe tener permiso/certificacion para el servicio.', 'regla_profesional', 'Filtrar por servicio.', 'filtrar por servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 004 algunos servicios requieren nivel senior o especialidad', 'PRO-004 Algunos servicios requieren nivel senior o especialidad.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 004 algunos servicios requieren nivel senior o especialidad'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-004 Algunos servicios requieren nivel senior o especialidad.', 'regla_profesional', 'Algunos servicios requieren nivel senior o especialidad.', 'algunos servicios requieren nivel senior o especialidad', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-004 Algunos servicios requieren nivel senior o especialidad.', 'regla_profesional', 'Asignar solo profesionales compatibles.', 'asignar solo profesionales compatibles', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 005 debe atender en la sucursal seleccionada', 'PRO-005 Debe atender en la sucursal seleccionada.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 005 debe atender en la sucursal seleccionada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-005 Debe atender en la sucursal seleccionada.', 'regla_profesional', 'Debe atender en la sucursal seleccionada.', 'debe atender en la sucursal seleccionada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-005 Debe atender en la sucursal seleccionada.', 'regla_profesional', 'Filtrar por sede y fecha.', 'filtrar por sede y fecha', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 006 debe considerarse sede por tramo horario', 'PRO-006 Debe considerarse sede por tramo horario.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 006 debe considerarse sede por tramo horario'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-006 Debe considerarse sede por tramo horario.', 'regla_profesional', 'Debe considerarse sede por tramo horario.', 'debe considerarse sede por tramo horario', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-006 Debe considerarse sede por tramo horario.', 'regla_profesional', 'Validar calendario de rotacion.', 'validar calendario de rotacion', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 007 la cita debe caer dentro del turno', 'PRO-007 La cita debe caer dentro del turno.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 007 la cita debe caer dentro del turno'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-007 La cita debe caer dentro del turno.', 'regla_profesional', 'La cita debe caer dentro del turno.', 'la cita debe caer dentro del turno', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-007 La cita debe caer dentro del turno.', 'regla_profesional', 'Generar slots dentro de turno.', 'generar slots dentro de turno', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 008 no agendar sobre descansos', 'PRO-008 No agendar sobre descansos.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 008 no agendar sobre descansos'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-008 No agendar sobre descansos.', 'regla_profesional', 'No agendar sobre descansos.', 'no agendar sobre descansos', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-008 No agendar sobre descansos.', 'regla_profesional', 'Bloquear pausas.', 'bloquear pausas', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 009 vacaciones permisos licencias capacitaciones y reuniones bloquean agenda', 'PRO-009 Vacaciones, permisos, licencias, capacitaciones y reuniones bloquean agenda.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 009 vacaciones permisos licencias capacitaciones y reuniones bloquean agenda'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-009 Vacaciones, permisos, licencias, capacitaciones y reuniones bloquean agenda.', 'regla_profesional', 'Vacaciones, permisos, licencias, capacitaciones y reuniones bloquean agenda.', 'vacaciones permisos licencias capacitaciones y reuniones bloquean agenda', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-009 Vacaciones, permisos, licencias, capacitaciones y reuniones bloquean agenda.', 'regla_profesional', 'Excluir dia/tramo.', 'excluir dia tramo', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 010 no puede tener dos citas al mismo tiempo', 'PRO-010 No puede tener dos citas al mismo tiempo.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 010 no puede tener dos citas al mismo tiempo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-010 No puede tener dos citas al mismo tiempo.', 'regla_profesional', 'No puede tener dos citas al mismo tiempo.', 'no puede tener dos citas al mismo tiempo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-010 No puede tener dos citas al mismo tiempo.', 'regla_profesional', 'Bloqueo atomico.', 'bloqueo atomico', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 011 debe existir margen para cierre limpieza o preparacion', 'PRO-011 Debe existir margen para cierre, limpieza o preparacion.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 011 debe existir margen para cierre limpieza o preparacion'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-011 Debe existir margen para cierre, limpieza o preparacion.', 'regla_profesional', 'Debe existir margen para cierre, limpieza o preparacion.', 'debe existir margen para cierre limpieza o preparacion', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-011 Debe existir margen para cierre, limpieza o preparacion.', 'regla_profesional', 'Agregar buffer por servicio/profesional.', 'agregar buffer por servicio profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 012 si cambia de sede debe existir tiempo suficiente', 'PRO-012 Si cambia de sede, debe existir tiempo suficiente.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 012 si cambia de sede debe existir tiempo suficiente'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-012 Si cambia de sede, debe existir tiempo suficiente.', 'regla_profesional', 'Si cambia de sede, debe existir tiempo suficiente.', 'si cambia de sede debe existir tiempo suficiente', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-012 Si cambia de sede, debe existir tiempo suficiente.', 'regla_profesional', 'Agregar buffer de traslado.', 'agregar buffer de traslado', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 013 si cliente pide uno especifico no reasignar sin confirmar', 'PRO-013 Si cliente pide uno especifico, no reasignar sin confirmar.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 013 si cliente pide uno especifico no reasignar sin confirmar'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-013 Si cliente pide uno especifico, no reasignar sin confirmar.', 'regla_profesional', 'Si cliente pide uno especifico, no reasignar sin confirmar.', 'si cliente pide uno especifico no reasignar sin confirmar', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-013 Si cliente pide uno especifico, no reasignar sin confirmar.', 'regla_profesional', 'Pedir confirmacion para cambio.', 'pedir confirmacion para cambio', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 014 si cliente no elige asignar por disponibilidad carga y competencia', 'PRO-014 Si cliente no elige, asignar por disponibilidad, carga y competencia.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 014 si cliente no elige asignar por disponibilidad carga y competencia'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-014 Si cliente no elige, asignar por disponibilidad, carga y competencia.', 'regla_profesional', 'Si cliente no elige, asignar por disponibilidad, carga y competencia.', 'si cliente no elige asignar por disponibilidad carga y competencia', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-014 Si cliente no elige, asignar por disponibilidad, carga y competencia.', 'regla_profesional', 'Ordenar candidatos.', 'ordenar candidatos', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 015 profesional puede tener limite de citas o minutos por dia', 'PRO-015 Profesional puede tener limite de citas o minutos por dia.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 015 profesional puede tener limite de citas o minutos por dia'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-015 Profesional puede tener limite de citas o minutos por dia.', 'regla_profesional', 'Profesional puede tener limite de citas o minutos por dia.', 'profesional puede tener limite de citas o minutos por dia', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-015 Profesional puede tener limite de citas o minutos por dia.', 'regla_profesional', 'Bloquear al superar limite.', 'bloquear al superar limite', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 016 algunos servicios tienen limite por dia por desgaste o equipo', 'PRO-016 Algunos servicios tienen limite por dia por desgaste o equipo.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 016 algunos servicios tienen limite por dia por desgaste o equipo'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-016 Algunos servicios tienen limite por dia por desgaste o equipo.', 'regla_profesional', 'Algunos servicios tienen limite por dia por desgaste o equipo.', 'algunos servicios tienen limite por dia por desgaste o equipo', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-016 Algunos servicios tienen limite por dia por desgaste o equipo.', 'regla_profesional', 'Controlar contador.', 'controlar contador', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 017 si cambia profesional puede cambiar precio comision', 'PRO-017 Si cambia profesional puede cambiar precio/comision.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 017 si cambia profesional puede cambiar precio comision'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-017 Si cambia profesional puede cambiar precio/comision.', 'regla_profesional', 'Si cambia profesional puede cambiar precio/comision.', 'si cambia profesional puede cambiar precio comision', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-017 Si cambia profesional puede cambiar precio/comision.', 'regla_profesional', 'Recalcular y auditar.', 'recalcular y auditar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 018 cliente solo debe ver datos publicos del profesional', 'PRO-018 Cliente solo debe ver datos publicos del profesional.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 018 cliente solo debe ver datos publicos del profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-018 Cliente solo debe ver datos publicos del profesional.', 'regla_profesional', 'Cliente solo debe ver datos publicos del profesional.', 'cliente solo debe ver datos publicos del profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-018 Cliente solo debe ver datos publicos del profesional.', 'regla_profesional', 'Ocultar datos internos.', 'ocultar datos internos', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 019 si profesional se ausenta sistema debe permitir reasignacion controlada', 'PRO-019 Si profesional se ausenta, sistema debe permitir reasignacion controlada.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 019 si profesional se ausenta sistema debe permitir reasignacion controlada'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-019 Si profesional se ausenta, sistema debe permitir reasignacion controlada.', 'regla_profesional', 'Si profesional se ausenta, sistema debe permitir reasignacion controlada.', 'si profesional se ausenta sistema debe permitir reasignacion controlada', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-019 Si profesional se ausenta, sistema debe permitir reasignacion controlada.', 'regla_profesional', 'Ofrecer alternativas y notificar.', 'ofrecer alternativas y notificar', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'pro 020 algunos negocios requieren aceptacion del profesional', 'PRO-020 Algunos negocios requieren aceptacion del profesional.', 70, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'pro 020 algunos negocios requieren aceptacion del profesional'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-020 Algunos negocios requieren aceptacion del profesional.', 'regla_profesional', 'Algunos negocios requieren aceptacion del profesional.', 'algunos negocios requieren aceptacion del profesional', 'es', 'CL', 'PREFERRED', 0.85, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'PRO-020 Algunos negocios requieren aceptacion del profesional.', 'regla_profesional', 'Estado pendiente profesional.', 'estado pendiente profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 70, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 001 crear bloques segun intervalo configurable 5 10 15 20 30 o 60 minutos', 'MOT-001 Crear bloques segun intervalo configurable: 5, 10, 15, 20, 30 o 60 minutos.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 001 crear bloques segun intervalo configurable 5 10 15 20 30 o 60 minutos'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-001 Crear bloques segun intervalo configurable: 5, 10, 15, 20, 30 o 60 minutos.', 'regla_motor_disponibilidad', 'Crear bloques segun intervalo configurable: 5, 10, 15, 20, 30 o 60 minutos.', 'crear bloques segun intervalo configurable 5 10 15 20 30 o 60 minutos', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-001 Crear bloques segun intervalo configurable: 5, 10, 15, 20, 30 o 60 minutos.', 'regla_motor_disponibilidad', 'Horario de sucursal/profesional', 'horario de sucursal profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 002 cada slot debe cubrir duracion total del servicio seleccionado', 'MOT-002 Cada slot debe cubrir duracion total del servicio seleccionado.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 002 cada slot debe cubrir duracion total del servicio seleccionado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-002 Cada slot debe cubrir duracion total del servicio seleccionado.', 'regla_motor_disponibilidad', 'Cada slot debe cubrir duracion total del servicio seleccionado.', 'cada slot debe cubrir duracion total del servicio seleccionado', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-002 Cada slot debe cubrir duracion total del servicio seleccionado.', 'regla_motor_disponibilidad', 'Servicio', 'servicio', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 003 agregar minutos antes y despues segun servicio profesional recurso o sucursal', 'MOT-003 Agregar minutos antes y despues segun servicio, profesional, recurso o sucursal.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 003 agregar minutos antes y despues segun servicio profesional recurso o sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-003 Agregar minutos antes y despues segun servicio, profesional, recurso o sucursal.', 'regla_motor_disponibilidad', 'Agregar minutos antes y despues segun servicio, profesional, recurso o sucursal.', 'agregar minutos antes y despues segun servicio profesional recurso o sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-003 Agregar minutos antes y despues segun servicio, profesional, recurso o sucursal.', 'regla_motor_disponibilidad', 'Servicio/profesional/recurso', 'servicio profesional recurso', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 004 intersectar disponibilidad del profesional con apertura de sucursal', 'MOT-004 Intersectar disponibilidad del profesional con apertura de sucursal.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 004 intersectar disponibilidad del profesional con apertura de sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-004 Intersectar disponibilidad del profesional con apertura de sucursal.', 'regla_motor_disponibilidad', 'Intersectar disponibilidad del profesional con apertura de sucursal.', 'intersectar disponibilidad del profesional con apertura de sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-004 Intersectar disponibilidad del profesional con apertura de sucursal.', 'regla_motor_disponibilidad', 'Sucursal + profesional', 'sucursal profesional', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 005 excluir bloqueos administrativos reuniones mantencion feriados y pausas', 'MOT-005 Excluir bloqueos administrativos, reuniones, mantencion, feriados y pausas.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 005 excluir bloqueos administrativos reuniones mantencion feriados y pausas'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-005 Excluir bloqueos administrativos, reuniones, mantencion, feriados y pausas.', 'regla_motor_disponibilidad', 'Excluir bloqueos administrativos, reuniones, mantencion, feriados y pausas.', 'excluir bloqueos administrativos reuniones mantencion feriados y pausas', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-005 Excluir bloqueos administrativos, reuniones, mantencion, feriados y pausas.', 'regla_motor_disponibilidad', 'Calendarios', 'calendarios', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 006 validar inicio fin existente y fin inicio existente para detectar choque', 'MOT-006 Validar inicio < fin_existente y fin > inicio_existente para detectar choque.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 006 validar inicio fin existente y fin inicio existente para detectar choque'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-006 Validar inicio < fin_existente y fin > inicio_existente para detectar choque.', 'regla_motor_disponibilidad', 'Validar inicio < fin_existente y fin > inicio_existente para detectar choque.', 'validar inicio fin existente y fin inicio existente para detectar choque', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-006 Validar inicio < fin_existente y fin > inicio_existente para detectar choque.', 'regla_motor_disponibilidad', 'Reserva candidata', 'reserva candidata', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 007 asignar cabina equipo compatible disponible durante todo el bloque', 'MOT-007 Asignar cabina/equipo compatible disponible durante todo el bloque.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 007 asignar cabina equipo compatible disponible durante todo el bloque'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-007 Asignar cabina/equipo compatible disponible durante todo el bloque.', 'regla_motor_disponibilidad', 'Asignar cabina/equipo compatible disponible durante todo el bloque.', 'asignar cabina equipo compatible disponible durante todo el bloque', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-007 Asignar cabina/equipo compatible disponible durante todo el bloque.', 'regla_motor_disponibilidad', 'Servicio + sede', 'servicio sede', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 008 controlar limite por sede profesional sala o servicio grupal', 'MOT-008 Controlar limite por sede, profesional, sala o servicio grupal.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 008 controlar limite por sede profesional sala o servicio grupal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-008 Controlar limite por sede, profesional, sala o servicio grupal.', 'regla_motor_disponibilidad', 'Controlar limite por sede, profesional, sala o servicio grupal.', 'controlar limite por sede profesional sala o servicio grupal', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-008 Controlar limite por sede, profesional, sala o servicio grupal.', 'regla_motor_disponibilidad', 'Configuracion capacidad', 'configuracion capacidad', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 009 crear hold con expiracion antes de pedir confirmacion pago', 'MOT-009 Crear hold con expiracion antes de pedir confirmacion/pago.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 009 crear hold con expiracion antes de pedir confirmacion pago'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-009 Crear hold con expiracion antes de pedir confirmacion/pago.', 'regla_motor_disponibilidad', 'Crear hold con expiracion antes de pedir confirmacion/pago.', 'crear hold con expiracion antes de pedir confirmacion pago', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-009 Crear hold con expiracion antes de pedir confirmacion/pago.', 'regla_motor_disponibilidad', 'Cliente + slot', 'cliente slot', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 010 convertir hold en reserva con bloqueo de fila clave unica', 'MOT-010 Convertir hold en reserva con bloqueo de fila/clave unica.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 010 convertir hold en reserva con bloqueo de fila clave unica'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-010 Convertir hold en reserva con bloqueo de fila/clave unica.', 'regla_motor_disponibilidad', 'Convertir hold en reserva con bloqueo de fila/clave unica.', 'convertir hold en reserva con bloqueo de fila clave unica', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-010 Convertir hold en reserva con bloqueo de fila/clave unica.', 'regla_motor_disponibilidad', 'Hold + datos reserva', 'hold datos reserva', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 011 usar conversacion cliente slot operacion para evitar duplicados', 'MOT-011 Usar conversacion + cliente + slot + operacion para evitar duplicados.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 011 usar conversacion cliente slot operacion para evitar duplicados'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-011 Usar conversacion + cliente + slot + operacion para evitar duplicados.', 'regla_motor_disponibilidad', 'Usar conversacion + cliente + slot + operacion para evitar duplicados.', 'usar conversacion cliente slot operacion para evitar duplicados', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-011 Usar conversacion + cliente + slot + operacion para evitar duplicados.', 'regla_motor_disponibilidad', 'Mensaje entrante', 'mensaje entrante', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 012 guardar en formato normalizado y mostrar en hora local de sucursal', 'MOT-012 Guardar en formato normalizado y mostrar en hora local de sucursal.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 012 guardar en formato normalizado y mostrar en hora local de sucursal'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-012 Guardar en formato normalizado y mostrar en hora local de sucursal.', 'regla_motor_disponibilidad', 'Guardar en formato normalizado y mostrar en hora local de sucursal.', 'guardar en formato normalizado y mostrar en hora local de sucursal', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-012 Guardar en formato normalizado y mostrar en hora local de sucursal.', 'regla_motor_disponibilidad', 'Fecha/hora', 'fecha hora', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 013 regla de profesional sucursal debe ganar a regla global si ambas aplican', 'MOT-013 Regla de profesional/sucursal debe ganar a regla global si ambas aplican.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 013 regla de profesional sucursal debe ganar a regla global si ambas aplican'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-013 Regla de profesional/sucursal debe ganar a regla global si ambas aplican.', 'regla_motor_disponibilidad', 'Regla de profesional/sucursal debe ganar a regla global si ambas aplican.', 'regla de profesional sucursal debe ganar a regla global si ambas aplican', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-013 Regla de profesional/sucursal debe ganar a regla global si ambas aplican.', 'regla_motor_disponibilidad', 'Reglas', 'reglas', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 014 cuando se cancela ofrecer cupo al siguiente cliente elegible', 'MOT-014 Cuando se cancela, ofrecer cupo al siguiente cliente elegible.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 014 cuando se cancela ofrecer cupo al siguiente cliente elegible'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-014 Cuando se cancela, ofrecer cupo al siguiente cliente elegible.', 'regla_motor_disponibilidad', 'Cuando se cancela, ofrecer cupo al siguiente cliente elegible.', 'cuando se cancela ofrecer cupo al siguiente cliente elegible', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-014 Cuando se cancela, ofrecer cupo al siguiente cliente elegible.', 'regla_motor_disponibilidad', 'Slot liberado', 'slot liberado', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_canonical_entity (business_id, entity_type, reference_type, reference_id, canonical_name, display_name, priority, created_at, updated_at)
    values (v_business, 'OTHER', null, null, 'mot 015 guardar por que un slot fue descartado', 'MOT-015 Guardar por que un slot fue descartado.', 65, now(), now())
    on conflict (business_id, entity_type, canonical_name) do update set display_name = excluded.display_name, priority = excluded.priority, updated_at = now();

    select id into v_canonical_id
    from ai_canonical_entity
    where business_id = v_business and entity_type = 'OTHER' and canonical_name = 'mot 015 guardar por que un slot fue descartado'
    limit 1;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-015 Guardar por que un slot fue descartado.', 'regla_motor_disponibilidad', 'Guardar por que un slot fue descartado.', 'guardar por que un slot fue descartado', 'es', 'CL', 'PREFERRED', 0.85, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

    insert into ai_entity_alias (id, business_id, entity_value, entity_key, alias, normalized_alias, language, country_code, alias_type, confidence_base, active, priority, canonical_entity_id, created_at, updated_at)
    values (gen_random_uuid(), v_business, 'MOT-015 Guardar por que un slot fue descartado.', 'regla_motor_disponibilidad', 'Motor disponibilidad', 'motor disponibilidad', 'es', 'CL', 'SYNONYM', 0.8, true, 65, v_canonical_id, now(), now())
    on conflict (business_id, entity_key, alias) do nothing;

end $$;
