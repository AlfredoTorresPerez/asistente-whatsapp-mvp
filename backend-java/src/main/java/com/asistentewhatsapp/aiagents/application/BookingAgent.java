package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BookingAgent extends AbstractAgentHandler {

    private final AiBusinessKnowledgeService knowledgeService;
    private final TransactionalAgendaBookingService transactionalAgendaBookingService;

    public BookingAgent(
            AiBusinessKnowledgeService knowledgeService,
            TransactionalAgendaBookingService transactionalAgendaBookingService) {
        this.knowledgeService = knowledgeService;
        this.transactionalAgendaBookingService = transactionalAgendaBookingService;
    }

    @Override
    public AgentType type() {
        return AgentType.BOOKING;
    }

    @Override
    public AgentRoutingResult handle(
            AgentConversationRequest request,
            IntentDetectionResult intent,
            Map<String, String> entities,
            List<String> missingData) {
        String traceId = AiTraceLogger.traceId(request);
        String normalizedMessage = TextNormalizer.normalize(request.messageBody());
        synchronizeBookingIntentEntity(intent, entities);
        AiTraceLogger.info("BOOKING_AGENT_STARTED", traceId, request.conversationId(), null, "BookingAgent",
                "intent=" + intent.primaryIntent() + " normalizedMessage=" + normalizedMessage
                        + " entities=" + AiTraceLogger.summarizeMap(entities));

        if (containsAny(normalizedMessage, "enlace expiro", "link expiro", "enlace vencio", "link vencio", "no funciona el enlace", "no funciona el link", "me dice expirado")) {
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    missing("reserva_temporal_vigente"),
                    WhatsAppMessageFormatter.confirmationLinkExpired(),
                    false,
                    null);
        }

        if (containsAny(normalizedMessage, "no me llego el link", "no me llego el enlace", "reenviar", "reenvia", "mandame el link", "mandame el enlace", "no recibi la confirmacion")) {
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    missing("reserva_temporal_vigente"),
                    "🔁 *Reenvío de enlace de confirmación*\n\nRevisaré si tienes una reserva temporal vigente para reenviar el enlace de confirmación.\n\nSi no la encuentro, te pediré los datos mínimos para crear una nueva reserva.",
                    false,
                    null);
        }

        if (intent.primaryIntent() == AgentIntent.BOOKING_CHANGE) {
            String response = transactionalAgendaBookingService.handleRescheduleBookingFromWhatsApp(
                    request.businessId(),
                    request.customerId(),
                    request.conversationId(),
                    request.customerPhone(),
                    request.messageBody(),
                    entities,
                    traceId,
                    request.conversationId());
            if (response == null || response.isBlank()) {
                response = WhatsAppMessageFormatter.rescheduleRequest();
            }
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    bookingFlowMissingData(entities),
                    response,
                    false,
                    null);
        }

        if (intent.primaryIntent() == AgentIntent.BOOKING_CANCEL) {
            String response = transactionalAgendaBookingService.handleCancelBookingFromWhatsApp(
                    request.businessId(),
                    request.customerId(),
                    request.conversationId(),
                    request.customerPhone(),
                    request.messageBody(),
                    entities,
                    traceId,
                    request.conversationId());
            if (response == null || response.isBlank()) {
                response = WhatsAppMessageFormatter.cancellationRequest();
            }
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    bookingFlowMissingData(entities),
                    response,
                    false,
                    null);
        }

        if (intent.primaryIntent() == AgentIntent.BOOKING_STATUS) {
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    missing("reserva_temporal_o_confirmada"),
                    knowledgeService.renderRule(request.businessId(), "AI_BOOKING_STATUS_IDENTIFY_RESPONSE", Map.of()),
                    false,
                    null);
        }

        TransactionalAgendaBookingService.BookingLinkResult linkResult = transactionalAgendaBookingService.generateBookingLink(
                request.businessId(), request.customerPhone(), request.conversationId(), request.customerId());
        String response = WhatsAppMessageFormatter.bookingLink(linkResult.url(), linkResult.isKnownCustomer());
        AiTraceLogger.info("BOOKING_LINK_GENERATED", traceId, request.conversationId(), null, "BookingAgent",
                "isKnownCustomer=" + linkResult.isKnownCustomer()
                        + " url=" + linkResult.url());
        AiTraceLogger.info("AI_FINAL_RESPONSE", traceId, request.conversationId(), null, "BookingAgent",
                "intent=" + intent.primaryIntent()
                        + " containsLink=" + response.contains("/reservar")
                        + " " + LogSanitizer.responseSummary(response));
        return result(request, intent, type(), entities, List.of(), response, false, null);
    }


    private void synchronizeBookingIntentEntity(IntentDetectionResult intent, Map<String, String> entities) {
        if (intent.primaryIntent() == AgentIntent.BOOKING_CANCEL) {
            entities.put("intencion", "cancelar_reserva");
            return;
        }
        if (intent.primaryIntent() == AgentIntent.BOOKING_CHANGE) {
            entities.put("intencion", "reprogramar_reserva");
        }
    }

    private List<String> bookingFlowMissingData(Map<String, String> entities) {
        String pendingAction = value(entities, "accion_pendiente");
        if ("CANCEL_CONFIRMATION".equals(pendingAction)) {
            return missing("confirmacion_cancelacion");
        }
        if ("CANCEL_SELECT".equals(pendingAction) || "RESCHEDULE_SELECT".equals(pendingAction)) {
            return missing("seleccion_reserva");
        }
        if ("RESCHEDULE_WAIT_NEW_DATE_TIME".equals(pendingAction)) {
            return missing("nueva_fecha_u_horario");
        }
        return List.of();
    }



    private boolean containsAny(String normalized, String... values) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        for (String value : values) {
            if (normalized.contains(TextNormalizer.normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private String value(Map<String, String> entities, String key) {
        String value = entities.get(key);
        return value == null ? "" : value.trim();
    }

}
