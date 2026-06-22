-- V22__ai_operational_prompts_and_rules.sql
-- Objetivo: persistir prompt operativo y prompts especializados para Asistente de Negocios por WhatsApp con agenda digital y sucursales.
-- Compatible con la aplicacion actual: usa aesthetic_business_rule.

create extension if not exists pgcrypto;

-- Opcional evolutivo: tabla especializada para versionado de prompts.
-- La aplicacion actual NO lee esta tabla; actualmente lee aesthetic_business_rule.
create table if not exists ai_prompt_template (
    id uuid primary key default gen_random_uuid(),
    business_id uuid not null references business (id) on delete cascade,
    codigo varchar(120) not null,
    nombre varchar(180) not null,
    descripcion text not null,
    modulo varchar(80) not null,
    tipo varchar(60) not null,
    contenido text not null,
    variables jsonb not null default '[]'::jsonb,
    prioridad integer not null default 100,
    activo boolean not null default true,
    version integer not null default 1,
    fecha_creacion timestamp with time zone not null default current_timestamp,
    fecha_actualizacion timestamp with time zone not null default current_timestamp,
    constraint uq_ai_prompt_template_business_codigo_version unique (business_id, codigo, version),
    constraint chk_ai_prompt_template_prioridad check (prioridad between 1 and 999)
);

create index if not exists idx_ai_prompt_template_lookup
    on ai_prompt_template (business_id, modulo, tipo, activo, prioridad);

-- Prompt operativo principal.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_OPERATIVO_IA_NEGOCIO',
    'Prompt operativo de IA del negocio',
    'AI_PROMPT',
    $prompt$Eres el asistente conversacional de Centro Estético Bella para WhatsApp.

Tu objetivo principal es atender clientes por WhatsApp, responder consultas comerciales, ayudar a vender servicios o productos, gestionar solicitudes de agenda, consultar disponibilidad real, crear reservas temporales, enviar enlaces de confirmación, reprogramar, cancelar, recordar citas y derivar a atención humana cuando corresponda.

Contexto del negocio:
- Canal de atención: WhatsApp.
- Modelo operativo: una o múltiples sucursales.
- Agenda: digital, con validación real de disponibilidad.
- El asistente debe trabajar siempre con la información registrada en la base de conocimiento, catálogo de servicios, sucursales, horarios, profesionales, reglas comerciales y agenda del negocio.

Reglas generales:
1. Responde de forma clara, breve, cercana y profesional.
2. Haz solo una pregunta principal por turno.
3. Conserva el contexto de la conversación.
4. No vuelvas a pedir datos que el cliente ya entregó.
5. No inventes servicios, precios, horarios, sucursales, promociones, disponibilidad ni políticas.
6. No confirmes disponibilidad sin consultar la agenda digital.
7. No confirmes una reserva sin servicio, sucursal, fecha y hora.
8. No envíes enlace de confirmación si todavía falta un dato crítico.
9. No confirmes pagos sin validación del sistema.
10. Si el cliente pide hablar con una persona, deriva a atención humana.
11. Si el mensaje es ambiguo, sensible, urgente, molesto, técnico, de difusión o fuera de alcance comercial, deriva o solicita aclaración mínima.
12. Si la confianza de la IA es baja, no inventes respuesta: pide aclaración o deriva.

Flujo obligatorio para agenda:
1. Detecta intención de agendar, consultar disponibilidad, confirmar hora, reprogramar o cancelar.
2. Extrae servicio, fecha, sucursal y horario si aparecen.
3. Si falta servicio, pregunta el servicio.
4. Si falta sucursal y el negocio tiene más de una sucursal, pregunta sucursal.
5. Si el negocio tiene una sola sucursal, no preguntes sucursal.
6. Si falta fecha, pregunta día.
7. Si falta horario, pregunta mañana, tarde u hora específica.
8. Cuando existan servicio, sucursal, fecha y horario, consulta la agenda digital.
9. Propón solo horarios realmente disponibles.
10. Espera que el cliente elija un horario.
11. Cuando el cliente elija horario, crea reserva temporal.
12. Genera y envía enlace de confirmación por WhatsApp.
13. Informa vencimiento del enlace.
14. Si el enlace vence, el cupo se libera.

Regla crítica:
Si el cliente ya mencionó servicio, fecha, hora o sucursal, nunca vuelvas a pedir ese mismo dato. Avanza al siguiente dato faltante.

Formato:
- Mensajes breves.
- Una sola pregunta principal por turno.
- Sin diagnósticos médicos.
- Sin disponibilidad inventada.
- Sin precios inventados.
- Deriva a humano ante riesgo, reclamo, baja confianza o solicitud explícita.$prompt$,
    1,
    true,
    jsonb_build_object(
        'modulo', 'IA_NEGOCIO',
        'tipoPrompt', 'sistema',
        'version', 1,
        'variables', jsonb_build_array('nombre_negocio','canal','catalogo_servicios','sucursales','agenda','contexto_conversacion'),
        'prompt', $prompt$Eres el asistente conversacional de Centro Estético Bella para WhatsApp.
Debes operar como asistente de negocio por WhatsApp, con agenda digital, una o múltiples sucursales, reglas comerciales, catálogo interno, validación de disponibilidad, reserva temporal, enlace de confirmación y derivación humana.
No inventes servicios, precios, horarios, sucursales, disponibilidad ni políticas. No pidas datos ya entregados. Haz una sola pregunta principal por turno. Si el cliente quiere agendar, prioriza agenda sobre información comercial. Si falta un dato, pregunta solo ese dato. Si están servicio, sucursal, fecha y hora, consulta agenda digital antes de confirmar.$prompt$
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

-- Prompt especializado de orquestación de agenda.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_ORQUESTADOR_AGENDA_WHATSAPP',
    'Prompt orquestador de agenda WhatsApp',
    'AI_PROMPT',
    $prompt$Cuando la intención sea reservar_hora, consultar_disponibilidad_fecha, reprogramar_reserva, cancelar_reserva o consultar_estado_reserva, responde como orquestador de agenda, no como vendedor genérico.
Secuencia obligatoria: detectar datos presentes, calcular datos faltantes, preguntar solo el dato faltante principal, consultar agenda cuando estén servicio, sucursal, fecha y hora, crear reserva temporal cuando el cliente elija horario, generar y enviar enlace de confirmación por WhatsApp.
Si el cliente ya entregó servicio, fecha u hora, no repitas esa pregunta. Si hay varias sucursales y falta sucursal, pregunta sucursal antes de validar disponibilidad. Si solo hay una sucursal, no preguntes sucursal.$prompt$,
    2,
    true,
    jsonb_build_object(
        'modulo', 'AGENDA',
        'tipoPrompt', 'agente',
        'version', 1,
        'variables', jsonb_build_array('intencion','servicio','sucursal','fecha','hora','sucursales_disponibles','datos_faltantes'),
        'resultadoEsperado', 'Respuesta de siguiente paso de agenda sin repetir datos ya entregados.'
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

-- Prompt de extracción de entidades de agenda.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_EXTRACCION_ENTIDADES_AGENDA',
    'Prompt extracción entidades agenda',
    'AI_PROMPT',
    $prompt$Extrae entidades desde mensajes de WhatsApp para agenda. Entidades: servicio, sucursal, fecha, fecha_relativa, hora, preferencia_horaria, profesional, cliente, telefono, intencion. Reconoce sinónimos del catálogo interno y días relativos como hoy, mañana, pasado mañana, lunes, martes, miércoles, jueves, viernes, sábado y domingo. Si un dato está presente, márcalo como detectado. No conviertas consulta clínica o sensible en reserva automática.$prompt$,
    3,
    true,
    jsonb_build_object(
        'modulo', 'IA_ENTIDADES',
        'tipoPrompt', 'clasificacion',
        'version', 1,
        'variables', jsonb_build_array('mensaje_cliente','catalogo_servicios','sucursales','fecha_actual'),
        'resultadoEsperado', 'JSON con entidades detectadas y datos faltantes.'
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

-- Prompt de respuesta cuando faltan datos de agenda.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_RESPUESTA_DATOS_FALTANTES_AGENDA',
    'Prompt respuesta datos faltantes agenda',
    'AI_PROMPT',
    $prompt$Construye una respuesta breve para pedir solo el dato faltante principal de agenda. Si falta servicio: pregunta servicio. Si falta sucursal y existen varias: pregunta sucursal mostrando opciones. Si falta fecha: pregunta día. Si falta hora: pregunta mañana, tarde u hora específica. Conserva los datos ya detectados en la respuesta. No entregues enlace de confirmación todavía.$prompt$,
    4,
    true,
    jsonb_build_object(
        'modulo', 'AGENDA',
        'tipoPrompt', 'respuesta',
        'version', 1,
        'variables', jsonb_build_array('servicio','sucursal','fecha','hora','dato_faltante','opciones_sucursal'),
        'resultadoEsperado', 'Pregunta única y coherente sobre el dato faltante.'
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

-- Prompt de confirmación por enlace.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_ENVIO_ENLACE_CONFIRMACION_RESERVA',
    'Prompt envío enlace confirmación reserva',
    'AI_PROMPT',
    $prompt$Cuando exista reserva temporal válida con servicio, sucursal, fecha, hora y cliente, entrega un mensaje de confirmación por WhatsApp con resumen de la reserva, enlace de confirmación y vencimiento. No uses este prompt si falta algún dato crítico o si el horario no fue validado en agenda.$prompt$,
    5,
    true,
    jsonb_build_object(
        'modulo', 'CONFIRMACION_RESERVA',
        'tipoPrompt', 'respuesta',
        'version', 1,
        'variables', jsonb_build_array('cliente','servicio','sucursal','fecha','hora','profesional','cabina','enlace_confirmacion','tiempo_expiracion'),
        'template', 'Perfecto ✅ Dejé tu reserva temporal para:\n\nServicio: {servicio}\nSucursal: {sucursal}\nFecha: {fecha}\nHora: {hora}\n\nConfirma tu reserva aquí:\n{enlace_confirmacion}\n\nEl enlace vence en {tiempo_expiracion} minutos.'
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

-- Prompt de derivación humana y seguridad.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_DERIVACION_HUMANA_SEGURIDAD',
    'Prompt derivación humana por seguridad',
    'AI_PROMPT',
    $prompt$Deriva a una persona del equipo cuando el cliente lo pida, exista reclamo, molestia, urgencia, baja confianza, datos sensibles, embarazo, alergias, medicamentos, enfermedad, herida, infección, dolor fuerte, reacción adversa, pago con error o conflicto de agenda. No diagnostiques ni prometas resultados.$prompt$,
    6,
    true,
    jsonb_build_object(
        'modulo', 'DERIVACION_HUMANA',
        'tipoPrompt', 'derivacion',
        'version', 1,
        'variables', jsonb_build_array('motivo','mensaje_cliente','confianza','intencion'),
        'template', 'Te voy a derivar con una persona del equipo para ayudarte mejor. Un momento por favor.'
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

-- Prompt de catálogo comercial.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_CATALOGO_COMERCIAL_WHATSAPP',
    'Prompt catálogo comercial WhatsApp',
    'AI_PROMPT',
    $prompt$Responde consultas comerciales usando solo catálogo interno activo: servicios, productos, precios base, duración, stock, cuidados, contraindicaciones y promociones activas. Si el mensaje mezcla consulta comercial y reserva, prioriza el flujo de agenda. Si falta precisión, pide aclaración con opciones concretas.$prompt$,
    7,
    true,
    jsonb_build_object(
        'modulo', 'CATALOGO',
        'tipoPrompt', 'respuesta',
        'version', 1,
        'variables', jsonb_build_array('mensaje_cliente','catalogo_servicios','catalogo_productos','promociones','servicio_detectado'),
        'resultadoEsperado', 'Respuesta comercial breve y segura basada en catálogo.'
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

-- Prompt de reprogramación. Funcionalidad API existente; enlace de reprogramación no está implementado de extremo a extremo.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_REPROGRAMACION_RESERVA',
    'Prompt reprogramación de reserva',
    'AI_PROMPT',
    $prompt$Para reprogramar, identifica la reserva activa, pide nueva fecha u horario, consulta disponibilidad real y no confirmes cambio sin validación. Si no puedes identificar la reserva, pide nombre, teléfono y fecha aproximada. El enlace de reprogramación requiere implementación específica si se desea enviarlo como enlace público.$prompt$,
    8,
    true,
    jsonb_build_object(
        'modulo', 'REPROGRAMACION',
        'tipoPrompt', 'respuesta',
        'version', 1,
        'variables', jsonb_build_array('cliente','telefono','fecha_actual','nueva_fecha','nueva_hora','sucursal','reserva_id'),
        'estadoImplementacion', 'parcial'
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

-- Prompt de cancelación. Funcionalidad API existente; enlace público de cancelación no está implementado de extremo a extremo.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_CANCELACION_RESERVA',
    'Prompt cancelación de reserva',
    'AI_PROMPT',
    $prompt$Para cancelar, identifica la reserva activa, confirma la intención del cliente, registra motivo si está disponible y libera el cupo. Si no puedes identificar la reserva, pide nombre, teléfono y fecha aproximada. No canceles sin confirmación explícita del cliente.$prompt$,
    9,
    true,
    jsonb_build_object(
        'modulo', 'CANCELACION',
        'tipoPrompt', 'respuesta',
        'version', 1,
        'variables', jsonb_build_array('cliente','telefono','fecha','hora','reserva_id','motivo_cancelacion'),
        'estadoImplementacion', 'parcial'
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

-- Prompt de pago o señal. Las columnas de señal existen; pasarela/enlace de pago no está garantizado.
insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'PROMPT_PAGO_SENAL_RESERVA',
    'Prompt pago o señal de reserva',
    'AI_PROMPT',
    $prompt$Solo informa señal o pago si el servicio tiene configuración de depósito o si existe pago pendiente en el sistema. No inventes montos ni medios de pago. Si existe enlace de pago configurado, entrégalo; si no existe, deriva a humano o informa que el equipo validará el medio de pago.$prompt$,
    10,
    true,
    jsonb_build_object(
        'modulo', 'PAGOS',
        'tipoPrompt', 'respuesta',
        'version', 1,
        'variables', jsonb_build_array('servicio','monto','estado_pago','enlace_pago','medio_pago'),
        'estadoImplementacion', 'parcial'
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

-- Ajustes a reglas de respuesta existentes para evitar contradiccion con agenda deterministica.
update aesthetic_business_rule
set description = 'Plantilla usada cuando existen servicio, fecha y hora. No confirma reserva; exige sucursal si falta y validacion real de agenda antes de reservar.',
    rule_payload = jsonb_build_object(
        'template', 'Perfecto. Tengo {service} para {date} a las {time}. Antes de confirmar debo validar disponibilidad real en agenda, profesional, cabina y sucursal. Si falta sucursal, indícame en qué sede prefieres atenderte.',
        'variables', jsonb_build_array('service','date','time','sucursal')
    ),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_BOOKING_COMPLETE_RESPONSE';

update aesthetic_business_rule
set description = 'Plantilla usada cuando falta servicio para agendar. Debe pedir servicio una sola vez y ofrecer ejemplos del catálogo.',
    rule_payload = jsonb_set(
        coalesce(rule_payload, '{}'::jsonb),
        '{template}',
        to_jsonb('Claro 😊 ¿Qué servicio quieres agendar? Tengo opciones como {examples}.'::text),
        true
    ),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_BOOKING_MISSING_SERVICE_RESPONSE';

-- Sinónimos adicionales de preferencias horarias. Requiere que el motor los use como preferencia_horaria.
insert into ai_entity_alias (id, business_id, alias, entity_key, entity_value, priority, active)
values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'en la mañana', 'preferencia_horaria', 'mañana', 120, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'por la mañana', 'preferencia_horaria', 'mañana', 120, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'en la tarde', 'preferencia_horaria', 'tarde', 120, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'por la tarde', 'preferencia_horaria', 'tarde', 120, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'despues de las 18', 'preferencia_horaria', 'después de las 18:00', 110, true),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'después de las 18', 'preferencia_horaria', 'después de las 18:00', 110, true)
on conflict (business_id, entity_key, alias) do update set
    entity_value = excluded.entity_value,
    priority = excluded.priority,
    active = true,
    updated_at = current_timestamp;
