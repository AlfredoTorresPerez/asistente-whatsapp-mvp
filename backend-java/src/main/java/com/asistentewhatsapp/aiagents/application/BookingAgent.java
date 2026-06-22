package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BookingAgent extends AbstractAgentHandler {

    private final AiBusinessKnowledgeService knowledgeService;
    private final BusinessLocationJdbcRepository businessLocationJdbcRepository;
    private final TransactionalAgendaBookingService transactionalAgendaBookingService;

    public BookingAgent(
            AiBusinessKnowledgeService knowledgeService,
            BusinessLocationJdbcRepository businessLocationJdbcRepository,
            TransactionalAgendaBookingService transactionalAgendaBookingService) {
        this.knowledgeService = knowledgeService;
        this.businessLocationJdbcRepository = businessLocationJdbcRepository;
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

        List<String> missing = new ArrayList<>();
        List<BusinessLocationRecord> activeLocations = businessLocationJdbcRepository.findActive(request.businessId());
        String service = value(entities, "servicio_o_producto");
        String extractedLocation = firstNonBlank(value(entities, "sede"), value(entities, "sede_id"));
        TransactionalAgendaBookingService.ResolvedLocation effectiveLocation = transactionalAgendaBookingService.resolveEffectiveLocation(
                request.businessId(),
                request.messageBody(),
                extractedLocation,
                request.selectedLocationId(),
                request.selectedLocationName(),
                traceId,
                request.conversationId());
        String location = effectiveLocation.location() == null ? "" : effectiveLocation.location().name();
        AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, request.conversationId(), null, "BookingAgent",
                "locationId=" + (effectiveLocation.location() == null ? "" : effectiveLocation.location().id())
                        + " locationName=" + location
                        + " source=" + effectiveLocation.source()
                        + " messageLocation=" + extractedLocation
                        + " conversationLocation=" + request.selectedLocationName());
        if (effectiveLocation.location() != null) {
            entities.put("sede", effectiveLocation.location().name());
            entities.put("sede_id", effectiveLocation.location().id().toString());
            entities.put("sede_fuente", effectiveLocation.source());
        } else {
            entities.put("sede_fuente", "MISSING");
        }
        String date = firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa"));
        String time = value(entities, "hora");

        boolean invalidDateOrTime = hasInvalidTime(normalizedMessage);
        boolean explicitlyUnconfiguredService = normalizedMessage.contains("servicio no configurado");
        if (explicitlyUnconfiguredService) {
            missing.add("servicio_configurado");
        } else if (!has(entities, "servicio_o_producto") || "depilación".equalsIgnoreCase(service) || "depilacion".equalsIgnoreCase(service)) {
            missing.add("motivo_o_servicio");
        }
        if (activeLocations.size() > 1 && location.isBlank()) {
            missing.add("sede");
        }
        if (invalidDateOrTime) {
            missing.add("fecha_u_hora_valida");
        } else {
            if (!has(entities, "fecha") && !has(entities, "fecha_relativa")) {
                missing.add("fecha_deseada");
            }
            if (!has(entities, "hora")) {
                missing.add("horario_preferido");
            }
        }
        String nextAction = missing.contains("motivo_o_servicio") || missing.contains("servicio_configurado") ? "ASK_SERVICE"
                : missing.contains("sede") ? "ASK_LOCATION"
                : missing.contains("fecha_deseada") ? "ASK_DATE"
                : missing.contains("horario_preferido") || missing.contains("fecha_u_hora_valida") ? "ASK_TIME"
                : "VALIDATE_AVAILABILITY";
        AiTraceLogger.info("BOOKING_REQUIRED_DATA_CHECK", traceId, request.conversationId(), null, "BookingAgent",
                "hasService=" + !missing.contains("motivo_o_servicio")
                        + " hasLocation=" + !missing.contains("sede")
                        + " hasDate=" + !missing.contains("fecha_deseada")
                        + " hasTime=" + !missing.contains("horario_preferido")
                        + " missing=" + missing
                        + " nextAction=" + nextAction);

        String response;
        if (missing.contains("motivo_o_servicio") || missing.contains("servicio_configurado")) {
            response = WhatsAppMessageFormatter.askService();
        } else if (missing.contains("sede")) {
            response = WhatsAppMessageFormatter.askLocation();
        } else if (missing.contains("fecha_deseada")) {
            response = WhatsAppMessageFormatter.askDate();
        } else if (missing.contains("horario_preferido")) {
            response = askTimeWithContext(service, date);
        } else {
            response = transactionalAgendaBookingService.createTemporaryBookingLink(
                            request.businessId(),
                            request.customerId(),
                            request.conversationId(),
                            firstNonBlank(value(entities, "cliente"), request.customerDisplayName()),
                            firstNonBlank(value(entities, "telefono"), request.customerPhone()),
                            request.messageBody(),
                            service,
                            location,
                            date,
                            time,
                            false,
                            request.dryRun(),
                            traceId,
                            request.conversationId())
                    .orElseGet(() -> knowledgeService.bookingCompleteResponse(request.businessId(), service, date, time));
        }
        AiTraceLogger.info("WHATSAPP_MESSAGE_FORMATTED", traceId, request.conversationId(), null, "BookingAgent",
                "type=" + formattedMessageType(response)
                        + " containsLink=" + response.contains("/reservas/confirmar/")
                        + " messageLength=" + response.length());
        AiTraceLogger.info("AI_FINAL_RESPONSE", traceId, request.conversationId(), null, "BookingAgent",
                "intent=" + intent.primaryIntent()
                        + " missing=" + missing
                        + " containsLink=" + response.contains("/reservas/confirmar/")
                        + " " + LogSanitizer.responseSummary(response));
        return result(request, intent, type(), entities, missing, response, false, null);
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

    private String formattedMessageType(String response) {
        if (response == null) {
            return "UNKNOWN";
        }
        if (response.contains("*Reserva temporal creada*")) return "TEMPORARY_BOOKING";
        if (response.contains("*Vista previa de reserva*")) return "BOOKING_PREVIEW";
        if (response.contains("*Horario no disponible*")) return "NO_AVAILABILITY";
        if (response.contains("*Reenvío de enlace de confirmación*")) return "CONFIRMATION_LINK_RESEND";
        if (response.contains("*El enlace de confirmación venció*")) return "CONFIRMATION_LINK_EXPIRED";
        if (response.contains("*Solicitud de cancelación*")) return "CANCELLATION_REQUEST";
        if (response.contains("*Te derivaré con una persona del equipo*")) return "SENSITIVE_CASE";
        if (response.contains("¿Qué servicio quieres agendar?")) return "ASK_SERVICE";
        if (response.contains("¿En qué sucursal prefieres atenderte?")) return "ASK_LOCATION";
        if (response.contains("¿Qué día te gustaría agendar?")) return "ASK_DATE";
        if (response.contains("¿A qué hora prefieres asistir?")) return "ASK_TIME";
        return "GENERAL_BOOKING";
    }

    private String bookingDateTimeText(String date, String time) {
        if (date != null && !date.isBlank() && time != null && !time.isBlank()) {
            return " para " + date + " a las " + time;
        }
        if (date != null && !date.isBlank()) {
            return " para " + date;
        }
        if (time != null && !time.isBlank()) {
            return " a las " + time;
        }
        return "";
    }

    private String displayService(String service) {
        if (service == null || service.isBlank()) {
            return "el servicio";
        }
        return service.toLowerCase(java.util.Locale.ROOT);
    }

    private String askTimeWithContext(String service, String date) {
        if ((service == null || service.isBlank()) && (date == null || date.isBlank())) {
            return WhatsAppMessageFormatter.askTime();
        }
        StringBuilder builder = new StringBuilder("Perfecto 😊");
        if (service != null && !service.isBlank()) {
            builder.append(" Tengo ").append(displayService(service));
        }
        if (date != null && !date.isBlank()) {
            builder.append(service == null || service.isBlank() ? " Para " : " para ").append(date.trim());
        }
        return builder.append(". ¿Qué horario prefieres?").toString();
    }

    private String renderLocationOptions(List<BusinessLocationRecord> locations) {
        return locations.stream()
                .map(BusinessLocationRecord::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse("sede principal");
    }

    private boolean hasInvalidTime(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        return java.util.regex.Pattern.compile("\\b(?:(?:2[4-9]|[3-9][0-9])\\s*(?::| )\\s*[0-9]{2}|(?:[01]?\\d|2[0-3])\\s*(?::| )\\s*(?:[6-9]\\d))\\b")
                .matcher(normalizedMessage)
                .find();
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
