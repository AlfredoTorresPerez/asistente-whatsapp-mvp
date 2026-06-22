-- V23__complete_specialized_ai_agents_for_business.sql
-- Objetivo: completar agentes especializados, reglas deterministicas, alias y respuestas para IA de negocio WhatsApp multisucursal.
-- Mantiene todo lo incorporado en V22 y agrega cobertura para reenvio de enlace, expiracion, reprogramacion,
-- cancelacion, derivacion humana, casos sensibles, ubicacion, pago/senal, sinonimos y evaluacion de calidad.

create extension if not exists pgcrypto;

-- Alias de entidades e intenciones. Los alias de intencion se dejan persistidos para trazabilidad y uso por motor IA/reglas.
insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    -- Servicios faciales ampliados
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza cutis', 'servicio_o_producto', 'Limpieza facial profunda', 355, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'limpieza de cutis', 'servicio_o_producto', 'Limpieza facial profunda', 355, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'higiene de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 340, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'tratamiento de rostro', 'servicio_o_producto', 'Limpieza facial profunda', 330, true),
    -- Fechas y preferencias
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'esta semana', 'fecha_relativa', 'esta semana', 125, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'proxima semana', 'fecha_relativa', 'próxima semana', 125, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'próxima semana', 'fecha_relativa', 'próxima semana', 125, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'la otra semana', 'fecha_relativa', 'próxima semana', 125, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'mañana en la tarde', 'fecha_relativa', 'mañana', 126, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'manana en la tarde', 'fecha_relativa', 'mañana', 126, true),
    -- Intenciones criticas
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'no me llego el link', 'intencion', 'reenviar_enlace_confirmacion', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'no me llegó el link', 'intencion', 'reenviar_enlace_confirmacion', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'no me llego el enlace', 'intencion', 'reenviar_enlace_confirmacion', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'reenviar enlace', 'intencion', 'reenviar_enlace_confirmacion', 490, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'el enlace expiro', 'intencion', 'enlace_expirado', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'el enlace expiró', 'intencion', 'enlace_expirado', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'el link vencio', 'intencion', 'enlace_expirado', 490, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'quiero cambiar mi hora', 'intencion', 'reprogramar_reserva', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'quiero reprogramar', 'intencion', 'reprogramar_reserva', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'necesito cancelar mi cita', 'intencion', 'cancelar_reserva', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'cancelar mi reserva', 'intencion', 'cancelar_reserva', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'quiero hablar con una persona', 'intencion', 'solicitar_humano', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'quiero hablar con alguien', 'intencion', 'solicitar_humano', 490, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'tuve una reaccion', 'intencion', 'caso_sensible_post_tratamiento', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'tuve una reacción', 'intencion', 'caso_sensible_post_tratamiento', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'donde queda', 'intencion', 'consultar_ubicacion', 470, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'dónde queda', 'intencion', 'consultar_ubicacion', 470, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'tengo que pagar una señal', 'intencion', 'consultar_pago_senal', 500, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'tengo que pagar una senal', 'intencion', 'consultar_pago_senal', 500, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = true,
    updated_at = current_timestamp;

-- Reglas/prompt especializados persistidos.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'PROMPT_V23_ORQUESTADOR_AGENTES_ESPECIALIZADOS', 'Prompt V23 orquestador de agentes especializados', 'AI_PROMPT',
     $p$Orquesta mensajes de WhatsApp de Centro Estético Bella con prioridad determinística: caso_sensible_post_tratamiento > solicitar_humano > cancelar_reserva > reprogramar_reserva > reenviar_enlace_confirmacion > enlace_expirado > consultar_pago_senal > consultar_ubicacion > reservar_hora > consultar_disponibilidad > consultar_precio > consultar_servicios. Nunca conviertas cancelar, cambiar, humano, link, enlace, expiró, pago, señal, ubicación o dirección en reserva nueva.$p$, 1, true,
     jsonb_build_object('modulo','IA_ORQUESTADOR','tipoPrompt','sistema','version',23,'variables',jsonb_build_array('mensaje_cliente','contexto_conversacion','intencion','entidades'),'resultadoEsperado','Agente correcto con prioridad operativa antes de generar respuesta.')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'PROMPT_V23_EXTRACTOR_ENTIDADES_AGENDA', 'Prompt V23 extractor entidades agenda', 'AI_PROMPT',
     $p$Extrae servicio, sucursal, fecha, preferencia_horaria, hora_exacta, profesional, cliente, telefono, reserva, enlace, pago y caso_sensible. Mapea limpieza de rostro, higiene facial, limpieza de cutis y facial a Limpieza facial profunda. Si el cliente dice Providencia, Maipú, Santiago Centro o Sede Principal, conserva esa sucursal y no la reemplaces. En sábado en la mañana: fecha=sábado y preferencia_horaria=mañana; no confundir con mañana como día siguiente.$p$, 2, true,
     jsonb_build_object('modulo','IA_ENTIDADES','tipoPrompt','clasificacion','version',23,'variables',jsonb_build_array('mensaje_cliente','catalogo_servicios','sucursales','fecha_actual'),'resultadoEsperado','Entidades normalizadas y datos faltantes.')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_LINK_RESEND_RESPONSE', 'Respuesta reenvío enlace confirmación', 'AI_RESPONSE',
     'Respuesta cuando el cliente solicita reenviar enlace de confirmación.', 20, true,
     jsonb_build_object('template','Claro. Revisaré si tienes una reserva temporal vigente para reenviar el enlace de confirmación. Si no la encuentro, te pediré los datos mínimos para crear una nueva reserva.')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_LINK_EXPIRED_RESPONSE', 'Respuesta enlace expirado', 'AI_RESPONSE',
     'Respuesta cuando el cliente indica que el enlace expiró o venció.', 21, true,
     jsonb_build_object('template','Tu enlace anterior ya expiró y el cupo pudo haber sido liberado. Puedo ayudarte a buscar un nuevo horario disponible. ¿Quieres que revise opciones para la misma reserva?')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_CHANGE_V23_RESPONSE', 'Respuesta reprogramación V23', 'AI_RESPONSE',
     'Respuesta cuando el cliente solicita cambiar o reprogramar cita.', 22, true,
     jsonb_build_object('template','Claro, puedo ayudarte a reprogramar tu cita. ¿Qué día u horario prefieres?')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_BOOKING_CANCEL_V23_RESPONSE', 'Respuesta cancelación V23', 'AI_RESPONSE',
     'Respuesta cuando el cliente solicita cancelar una cita.', 23, true,
     jsonb_build_object('template','Entiendo. Puedo ayudarte a cancelar tu reserva. Para hacerlo de forma segura, necesito identificar la cita. ¿Me confirmas la sucursal o el horario de la reserva?')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_HUMAN_HANDOFF_V23_RESPONSE', 'Respuesta derivación humana V23', 'AI_RESPONSE',
     'Respuesta cuando el cliente pide hablar con una persona.', 24, true,
     jsonb_build_object('template','Te voy a derivar con una persona del equipo para ayudarte mejor. Un momento por favor.')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_SENSITIVE_POST_TREATMENT_V23_RESPONSE', 'Respuesta caso sensible post tratamiento V23', 'SAFETY',
     'Respuesta segura ante reacción, irritación, alergia, quemadura o dolor posterior a tratamiento.', 5, true,
     jsonb_build_object('template','Lamento que hayas tenido esa reacción. Te voy a derivar con una persona del equipo para ayudarte de inmediato. Si tienes molestias importantes o síntomas intensos, consulta con un profesional de salud.')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_PAYMENT_SIGNAL_V23_RESPONSE', 'Respuesta pago o señal V23', 'PAYMENT',
     'Respuesta sobre pago, abono o señal sin inventar montos.', 25, true,
     jsonb_build_object('template','Para responder sobre señal o pago debo revisar la regla configurada del servicio o reserva. No voy a inventar montos. ¿Qué servicio quieres reservar?')),

    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'AI_LOCATION_QUERY_V23_RESPONSE', 'Respuesta ubicación de sucursal V23', 'AI_RESPONSE',
     'Respuesta para consultas de dirección o ubicación de sucursal.', 26, true,
     jsonb_build_object('template','La sucursal {sucursal} está ubicada en: {direccion}'))
on conflict (business_id, code) do update set
    name = excluded.name,
    rule_type = excluded.rule_type,
    description = excluded.description,
    priority = excluded.priority,
    active = true,
    rule_payload = excluded.rule_payload,
    updated_at = current_timestamp;

-- Ajuste de reglas existentes para coherencia con orquestador V23.
update aesthetic_business_rule
set description = 'Datos mínimos para reservar: servicio, sucursal, fecha y hora. Si falta un dato, preguntar solo ese dato. No confirmar sin validar agenda.',
    rule_payload = jsonb_build_object(
        'datosMinimos', jsonb_build_array('servicio','sucursal','fecha','hora'),
        'reglaCritica', 'No pedir datos ya entregados y no confirmar disponibilidad sin agenda.'
    ),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'RESERVA_DATOS_MINIMOS';
