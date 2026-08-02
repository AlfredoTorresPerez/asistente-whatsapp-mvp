-- =============================================================================
-- V105: Fix deteccion KNOWLEDGE_QUERY detectada con casuisticas CAP-031 (no-show)
-- y CAP-023 (lista de espera) del Excel.
--
-- Fallos verificados con POST /api/v1/ai/preview:
--   1. "que pasa si no asisto a mi cita" -> AMBIGUOUS (debiera KNOWLEDGE_QUERY)
--      Causa: ni KNOWLEDGE_WORDS (Java) ni el catalogo BD tienen "no asisto".
--   2. "como funciona la lista de espera" -> COMMERCIAL_INQUIRY (debiera
--      KNOWLEDGE_QUERY). Causa: HELP_WORDS "como funciona" gana, pero el
--      catalogo BD se evalua ANTES (linea ~239 IntentDetectorService), asi que
--      una expresion mas larga con la frase completa lo captura primero.
-- =============================================================================

do $$
declare
    v_knowledge uuid;
begin
    select id into v_knowledge from ai_intent where code = 'KNOWLEDGE_QUERY' and business_id is null;

    insert into ai_intent_expression
        (intent_id, business_id, expression_original, expression_normalized, expression_type, priority, confidence_base, language, country_code, active)
    select t.intent_id, null, t.expression_original, lower(t.expression_original), t.expression_type, t.priority, t.confidence_base, 'es', 'CL', true
    from (values
        -- No-show / inasistencia (CAP-031): solo formulaciones interrogativas,
        -- las afirmativas ("no puedo asistir") siguen siendo BOOKING_CANCEL.
        (v_knowledge, 'que pasa si no asisto a mi cita',  'COMPLETE_PHRASE', 100, 0.90),
        (v_knowledge, 'que pasa si no asisto',            'COMPLETE_PHRASE', 100, 0.88),
        (v_knowledge, 'que pasa si no puedo asistir',     'COMPLETE_PHRASE', 100, 0.88),
        -- FAQ lista de espera (CAP-023): gana por longitud sobre "como funciona"
        (v_knowledge, 'como funciona la lista de espera', 'COMPLETE_PHRASE', 100, 0.90)
    ) as t(intent_id, expression_original, expression_type, priority, confidence_base)
    on conflict (intent_id, expression_normalized) where business_id is null
    do update set expression_type = excluded.expression_type,
                  priority = excluded.priority,
                  confidence_base = excluded.confidence_base,
                  updated_at = now();

end $$;
