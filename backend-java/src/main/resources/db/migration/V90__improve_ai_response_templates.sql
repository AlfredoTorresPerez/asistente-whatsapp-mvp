-- V90__improve_ai_response_templates.sql
-- Objetivo: mejorar templates de respuesta IA para consultas generales de catálogo y orientación.

-- 1. Respuesta para cuando el cliente pregunta por servicios sin especificar uno
--    Cambia de "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás buscando exactamente?"
--    a una respuesta más útil que guía al cliente con categorías
update aesthetic_business_rule
set rule_payload = jsonb_build_object(
    'template', 'Claro, tenemos varios servicios disponibles. ¿Te interesa alguno en particular? Por ejemplo: limpieza facial, masajes, depilación, manicure, pedicure. ¿Sobre cuál quieres información?',
    'labels', '["limpieza facial","masajes","depilación","manicure","pedicure"]'
),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_SALES_MISSING_SERVICE_RESPONSE';

-- 2. Respuesta genérica de respaldo - más acogedora que "¿Qué necesitas revisar hoy?"
update aesthetic_business_rule
set rule_payload = jsonb_build_object(
    'template', '¡Hola! Soy el asistente virtual de Centro Estético Bella. Puedo ayudarte con información de servicios, precios, disponibilidad, agendar horas, reprogramar o cancelar citas. ¿En qué te puedo ayudar hoy?'
),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_GENERIC_NEXT_STEP';

-- 3. Respuesta para venta siguiente paso - más natural
update aesthetic_business_rule
set rule_payload = jsonb_build_object(
    'template', 'Sobre {service}, puedo darte información de precio, duración y disponibilidad. ¿Qué prefieres revisar primero?'
),
    updated_at = current_timestamp
where business_id = '11111111-1111-1111-1111-111111111111'
  and code = 'AI_SALES_NEXT_STEP_RESPONSE';
