-- =============================================================================
-- V104: Correccion de deteccion de intencion detectada con casuisticas del
-- Excel agenda_digital_whatsapp_casuisticas.xlsx
--
-- Fallos corregidos (verificados con POST /api/v1/ai/preview):
--   1. "quiero mover mi reserva de las 16:00" -> BOOKING_STATUS (debiera CHANGE)
--      Causa: "mi reserva" (STATUS, len 10) gana por longitud sobre "mover"
--      (RESCHEDULE, len 5) en el orden priority desc, length desc.
--   2. "quiero anular mi reserva" -> BOOKING_STATUS (debiera CANCEL)
--      Misma causa: "mi reserva" (STATUS) gana sobre "anular" (CANCEL).
--   3. "tienen hora para el jueves" -> BOOKING_REQUEST (debiera AVAILABILITY)
--      Causa: "tienen hora" estaba clasificada como BOOKING_CREATE.
--   4. "hay disponibilidad para depilacion esta semana" -> BOOKING_REQUEST
--      Causa: "hay disponibilidad" estaba clasificada como BOOKING_CREATE.
--   5. "tienen cupo para dos personas" -> AMBIGUOUS (debiera AVAILABILITY)
--      Causa: no existia la expresion.
--   6. "a que horas tienen libre el sabado" -> BOOKING_REQUEST (debiera AVAIL)
--      Causa: "que horas tienen" estaba clasificada como BOOKING_CREATE.
-- =============================================================================

do $$
declare
    v_create uuid;
    v_availability uuid;
    v_reschedule uuid;
    v_cancel uuid;
begin
    select id into v_create       from ai_intent where code = 'BOOKING_CREATE'       and business_id is null;
    select id into v_availability from ai_intent where code = 'BOOKING_AVAILABILITY' and business_id is null;
    select id into v_reschedule   from ai_intent where code = 'BOOKING_RESCHEDULE'   and business_id is null;
    select id into v_cancel       from ai_intent where code = 'BOOKING_CANCEL'       and business_id is null;

    -- -------------------------------------------------------------------------
    -- A) Reclasificar expresiones de consulta de disponibilidad que estaban
    --    como BOOKING_CREATE -> BOOKING_AVAILABILITY
    -- -------------------------------------------------------------------------
    update ai_intent_expression
    set intent_id = v_availability, updated_at = now()
    where intent_id = v_create
      and expression_normalized in (
          'hay disponibilidad',
          'que horas tienen',
          'tienen hora',
          'tendran hora',
          'hay cupo para',
          'cupo disponible para'
      );

    -- "hay cupo" (REGIONALISM) queda como BOOKING_CREATE: en el flujo demo el
    -- cliente que dice "hay cupo" quiere agendar; "tienen cupo" en cambio es
    -- consulta. Se agrega la variante de consulta explicita en B.

    -- -------------------------------------------------------------------------
    -- B) Nuevas expresiones (frases largas para ganar por longitud sobre
    --    "mi reserva" / "mover" / "anular")
    -- -------------------------------------------------------------------------
    insert into ai_intent_expression
        (intent_id, business_id, expression_original, expression_normalized, expression_type, priority, confidence_base, language, country_code, active)
    select t.intent_id, null, t.expression_original, lower(t.expression_original), t.expression_type, t.priority, t.confidence_base, 'es', 'CL', true
    from (values
        -- RESCHEDULE: superan a "mi reserva" (STATUS) por longitud
        (v_reschedule, 'quiero mover mi reserva',      'COMPLETE_PHRASE', 100, 0.90),
        (v_reschedule, 'mover mi reserva',             'COMPLETE_PHRASE', 100, 0.88),
        (v_reschedule, 'cambiar mi reserva para',      'COMPLETE_PHRASE', 100, 0.88),
        (v_reschedule, 'reprogramar mi reserva',       'COMPLETE_PHRASE', 100, 0.90),
        (v_reschedule, 'mover mi cita',                'COMPLETE_PHRASE', 100, 0.88),
        (v_reschedule, 'reagendar mi reserva',         'COMPLETE_PHRASE', 100, 0.88),
        (v_reschedule, 'mover la reserva',             'COMPLETE_PHRASE', 100, 0.88),
        -- CANCEL: superan a "mi reserva" (STATUS) por longitud
        (v_cancel,     'quiero anular mi reserva',     'COMPLETE_PHRASE', 100, 0.90),
        (v_cancel,     'anular mi reserva',            'COMPLETE_PHRASE', 100, 0.88),
        (v_cancel,     'anular mi cita',               'COMPLETE_PHRASE', 100, 0.88),
        (v_cancel,     'cancelar mi reserva',          'COMPLETE_PHRASE', 100, 0.90),
        (v_cancel,     'suspender mi reserva',         'COMPLETE_PHRASE', 100, 0.88),
        -- AVAILABILITY: consultas de cupo/horario
        (v_availability, 'tienen cupo',                'COMPLETE_PHRASE', 100, 0.85),
        (v_availability, 'tienen cupo para',           'COMPLETE_PHRASE', 100, 0.88),
        (v_availability, 'hay cupo para dos',          'COMPLETE_PHRASE', 100, 0.88),
        (v_availability, 'tienen horas libres',        'COMPLETE_PHRASE', 100, 0.85),
        (v_availability, 'a que horas tienen',         'COMPLETE_PHRASE', 100, 0.88),
        (v_availability, 'que horas tienen libre',     'COMPLETE_PHRASE', 100, 0.88),
        (v_availability, 'horas libres',               'COMPLETE_PHRASE', 100, 0.85),
        (v_availability, 'tienen libre',               'COMPLETE_PHRASE', 100, 0.85)
    ) as t(intent_id, expression_original, expression_type, priority, confidence_base)
    on conflict (intent_id, expression_normalized) where business_id is null
    do update set expression_type = excluded.expression_type,
                  priority = excluded.priority,
                  confidence_base = excluded.confidence_base,
                  updated_at = now();

end $$;
