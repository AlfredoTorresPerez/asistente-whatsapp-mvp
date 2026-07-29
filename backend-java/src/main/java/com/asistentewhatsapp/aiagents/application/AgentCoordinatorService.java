package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentCoordinatorService {

	private static final Pattern NUMERIC_OPTION_PATTERN = Pattern
			.compile("^(?:opcion|opci[oó]n)?\\s*([1-3])\\s*\\.?\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern BOOKING_OPTION_LINE_PATTERN = Pattern.compile(
			"\\b([1-3])\\.\\s*(?:([a-záéíóúñ]+)\\s+a\\s+las\\s+)?([01]?\\d|2[0-3])(?::([0-5]\\d))?(?:\\s+con\\s+[^\\r\\n]+)?\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern SERVICE_OPTION_LINE_PATTERN = Pattern.compile("^\\s*([1-3])\\.\\s+(.+)$",
			Pattern.MULTILINE);
	private static final Set<String> AFFIRMATIVE_WORDS = Set.of("si", "sí", "ok", "okay", "dale", "claro", "simon",
			"sep", "yes", "por supuesto", "obvio");
	private static final Set<String> TRANSIENT_CONTEXT_KEYS = Set.of("trace_id", "intencion", "nombre",
			"ultimo_mensaje_cliente", "ultima_respuesta_ia", "timestamp_ultimo_turno", "ultimo_dato_solicitado");
	private static final Set<String> BOOKING_CONTEXT_KEYS = Set.of("servicio_o_producto", "servicio_codigo", "sede",
			"sede_id", "fecha", "fecha_relativa", "hora", "tramo_horario", "profesional", "preferencia_horaria",
			"opcion_agenda_seleccionada");

	private final AiAgentProperties properties;
	private final IntentDetectorService intentDetectorService;
	private final EntityExtractionService entityExtractionService;
	private final AgentRegistry agentRegistry;
	private final AiAgentJdbcRepository aiAgentJdbcRepository;

	public AgentCoordinatorService(AiAgentProperties properties, IntentDetectorService intentDetectorService,
			EntityExtractionService entityExtractionService, AgentRegistry agentRegistry,
			AiAgentJdbcRepository aiAgentJdbcRepository) {
		this.properties = properties;
		this.intentDetectorService = intentDetectorService;
		this.entityExtractionService = entityExtractionService;
		this.agentRegistry = agentRegistry;
		this.aiAgentJdbcRepository = aiAgentJdbcRepository;
	}

	@Transactional
	public Optional<AgentRoutingResult> route(AgentConversationRequest request) {
		String traceId = AiTraceLogger.traceId(request);
		AiTraceLogger.info("AI_ROUTE_STARTED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"channelAccountId=" + request.channelAccountId() + " customerId=" + request.customerId()
						+ " phoneMasked=" + AiTraceLogger.maskPhone(request.customerPhone()) + " "
						+ LogSanitizer.messageSummary("message", request.messageBody()));
		if (!properties.enabled() || isNonActionableMessage(request.messageBody())) {
			AiTraceLogger.warn("AI_ROUTE_SKIPPED", traceId, request.conversationId(), null, "AgentCoordinatorService",
					"enabled=" + properties.enabled() + " nonActionable=true");
			return Optional.empty();
		}

		Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext = aiAgentJdbcRepository
				.findConversationContext(request.businessId(), request.conversationId());
		AiTraceLogger.info("CONVERSATION_CONTEXT_LOADED", traceId, request.conversationId(), null,
				"AgentCoordinatorService",
				"hasPreviousContext=" + previousContext.isPresent() + " previousAgent="
						+ previousContext.map(AiAgentJdbcRepository.ConversationContextSnapshot::activeAgent)
								.orElse(null)
						+ " previousIntent=" + previousContext
								.map(AiAgentJdbcRepository.ConversationContextSnapshot::primaryIntent).orElse(null));

		IntentDetectionResult intent = intentDetectorService.detect(request);
		AiTraceLogger.info("INTENT_DETECTED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"intent=" + intent.primaryIntent() + " secondary=" + intent.secondaryIntent() + " confidence="
						+ intent.confidence() + " urgency=" + intent.urgency() + " reason=" + intent.handoffReason());
		AiTraceLogger.debug("INTENT_DETECTED_DEBUG", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"fullIntent=" + intent.toString());
		Map<String, String> currentEntities = entityExtractionService.extract(request);
		Map<String, String> entities = mergeEntities(previousContext, currentEntities, intent);
		enrichTurnContext(entities, request);
		entities.put("trace_id", traceId);
		resolveNumericBookingOption(previousContext, request.messageBody(), entities, traceId,
				request.conversationId());
		AiTraceLogger.info("ENTITIES_MERGED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"entities=" + AiTraceLogger.summarizeMap(entities));
		AiTraceLogger.debug("ENTITIES_DEBUG", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"extractedEntities=" + AiTraceLogger.summarizeMap(currentEntities));
		IntentDetectionResult resolvedIntent = resolveContextAwareIntent(intent, previousContext, entities);
		if (resolvedIntent != intent) {
			AiTraceLogger.info("INTENT_CONTEXT_RESOLVED", traceId, request.conversationId(), null,
					"AgentCoordinatorService", "original=" + intent.primaryIntent() + " resolved="
							+ resolvedIntent.primaryIntent() + " confidence=" + resolvedIntent.confidence());
		}

		AgentHandler handler = agentRegistry.resolve(resolvedIntent);
		AiTraceLogger.info("AGENT_SELECTED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"agent=" + handler.type() + " intent=" + resolvedIntent.primaryIntent());
		AgentRoutingResult result = handler.handle(request, resolvedIntent, entities, List.of());
		enrichResultContext(result);
		AiTraceLogger.info("AI_FINAL_RESPONSE", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"agent=" + result.agentType() + " intent=" + result.primaryIntent() + " confidence="
						+ result.confidence() + " missing=" + result.missingData() + " containsLink="
						+ containsLink(result.responseToCustomer()) + " "
						+ LogSanitizer.responseSummary(result.responseToCustomer()));

		if (properties.auditEnabled()) {
			aiAgentJdbcRepository.upsertConversationContext(result);
			aiAgentJdbcRepository.insertDecisionLog(result);
			aiAgentJdbcRepository.incrementMetric(result);
			if (result.requiresHuman()) {
				aiAgentJdbcRepository.insertHumanHandoff(result);
			}
		}

		return Optional.of(result);
	}

	public Optional<AgentRoutingResult> preview(AgentConversationRequest request) {
		String traceId = AiTraceLogger.traceId(request);
		AiTraceLogger.info("AI_PREVIEW_STARTED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"customerId=" + request.customerId() + " "
						+ LogSanitizer.messageSummary("message", request.messageBody()));
		if (!properties.enabled() || isNonActionableMessage(request.messageBody())) {
			AiTraceLogger.warn("AI_PREVIEW_SKIPPED", traceId, request.conversationId(), null, "AgentCoordinatorService",
					"enabled=" + properties.enabled() + " nonActionable=true");
			return Optional.empty();
		}

		Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext = aiAgentJdbcRepository
				.findConversationContext(request.businessId(), request.conversationId());
		AiTraceLogger.info("CONVERSATION_CONTEXT_LOADED", traceId, request.conversationId(), null,
				"AgentCoordinatorService",
				"hasPreviousContext=" + previousContext.isPresent() + " previousAgent="
						+ previousContext.map(AiAgentJdbcRepository.ConversationContextSnapshot::activeAgent)
								.orElse(null)
						+ " previousIntent=" + previousContext
								.map(AiAgentJdbcRepository.ConversationContextSnapshot::primaryIntent).orElse(null));

		IntentDetectionResult intent = intentDetectorService.detect(request);
		AiTraceLogger.info("INTENT_DETECTED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"intent=" + intent.primaryIntent() + " secondary=" + intent.secondaryIntent() + " confidence="
						+ intent.confidence() + " urgency=" + intent.urgency() + " reason=" + intent.handoffReason());
		AiTraceLogger.debug("INTENT_DETECTED_DEBUG", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"fullIntent=" + intent.toString());
		Map<String, String> currentEntities = entityExtractionService.extract(request);
		Map<String, String> entities = mergeEntities(previousContext, currentEntities, intent);
		enrichTurnContext(entities, request);
		entities.put("trace_id", traceId);
		resolveNumericBookingOption(previousContext, request.messageBody(), entities, traceId,
				request.conversationId());
		AiTraceLogger.info("ENTITIES_MERGED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"entities=" + AiTraceLogger.summarizeMap(entities));
		AiTraceLogger.debug("ENTITIES_DEBUG", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"extractedEntities=" + AiTraceLogger.summarizeMap(currentEntities));
		IntentDetectionResult resolvedIntent = resolveContextAwareIntent(intent, previousContext, entities);
		AgentHandler handler = agentRegistry.resolve(resolvedIntent);
		AiTraceLogger.info("AGENT_SELECTED", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"agent=" + handler.type() + " intent=" + resolvedIntent.primaryIntent());
		AgentRoutingResult result = handler.handle(request, resolvedIntent, entities, List.of());
		enrichResultContext(result);
		AiTraceLogger.info("AI_FINAL_RESPONSE", traceId, request.conversationId(), null, "AgentCoordinatorService",
				"agent=" + result.agentType() + " intent=" + result.primaryIntent() + " confidence="
						+ result.confidence() + " missing=" + result.missingData() + " containsLink="
						+ containsLink(result.responseToCustomer()) + " "
						+ LogSanitizer.responseSummary(result.responseToCustomer()));
		if (properties.auditEnabled()) {
			aiAgentJdbcRepository.upsertConversationContext(result);
		}
		return Optional.of(result);
	}

	private void resolveNumericBookingOption(
			Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext, String messageBody,
			Map<String, String> entities, String traceId, java.util.UUID conversationId) {
		if (previousContext.isEmpty() || messageBody == null) {
			return;
		}
		AiAgentJdbcRepository.ConversationContextSnapshot context = previousContext.get();
		if (context.activeAgent() != AgentType.BOOKING || !canSelectPreviousBookingOption(context)) {
			return;
		}
		Matcher selected = NUMERIC_OPTION_PATTERN.matcher(TextNormalizer.normalize(messageBody));
		if (!selected.matches()) {
			return;
		}
		int option = Integer.parseInt(selected.group(1));
		String lastResponse = context.extractedData().get("ultima_respuesta_ia");
		if (lastResponse == null || lastResponse.isBlank()) {
			return;
		}
		Matcher optionLine = BOOKING_OPTION_LINE_PATTERN.matcher(lastResponse);
		while (optionLine.find()) {
			int candidate = Integer.parseInt(optionLine.group(1));
			if (candidate == option) {
				context.extractedData().forEach((key, value) -> {
					if (value != null && !value.isBlank() && BOOKING_CONTEXT_KEYS.contains(key)) {
						entities.putIfAbsent(key, value);
					}
				});
				String weekday = optionLine.group(2);
				String hour = optionLine.group(3);
				String minute = optionLine.group(4) == null ? "00" : optionLine.group(4);
				String normalizedTime = String.format(java.util.Locale.ROOT, "%02d:%02d", Integer.parseInt(hour),
						Integer.parseInt(minute));
				if (weekday != null && !weekday.isBlank()) {
					entities.put("fecha_relativa", weekday.toLowerCase(java.util.Locale.ROOT));
				}
				entities.put("hora", normalizedTime);
				entities.put("opcion_agenda_seleccionada", String.valueOf(option));
				AiTraceLogger.info("BOOKING_OPTION_SELECTED", traceId, conversationId, null, "AgentCoordinatorService",
						"option=" + option + " resolvedDate=" + entities.get("fecha_relativa") + " resolvedTime="
								+ normalizedTime);
				return;
			}
		}
		List<String> missing = context.missingData() == null ? List.of() : context.missingData();
		if (missing.contains("motivo_o_servicio")) {
			Matcher serviceLine = SERVICE_OPTION_LINE_PATTERN.matcher(lastResponse);
			while (serviceLine.find()) {
				int candidate = Integer.parseInt(serviceLine.group(1));
				if (candidate == option) {
					String serviceName = serviceLine.group(2).trim();
					entities.putIfAbsent("servicio_o_producto", serviceName);
					context.extractedData().forEach((key, value) -> {
						if (value != null && !value.isBlank() && BOOKING_CONTEXT_KEYS.contains(key)) {
							entities.putIfAbsent(key, value);
						}
					});
					entities.put("opcion_agenda_seleccionada", String.valueOf(option));
					AiTraceLogger.info("BOOKING_SERVICE_SELECTED", traceId, conversationId, null,
							"AgentCoordinatorService", "option=" + option + " service=" + serviceName);
					return;
				}
			}
		}
		AiTraceLogger.warn("BOOKING_OPTION_NOT_RESOLVED", traceId, conversationId, null, "AgentCoordinatorService",
				"option=" + option + " reason=NO_MATCHING_PREVIOUS_OPTION");
	}

	private boolean canSelectPreviousBookingOption(AiAgentJdbcRepository.ConversationContextSnapshot context) {
		List<String> missing = context.missingData() == null ? List.of() : context.missingData();
		if (missing.contains("horario_preferido") || missing.contains("motivo_o_servicio")) {
			return true;
		}
		String lastResponse = context.extractedData() == null ? "" : context.extractedData().get("ultima_respuesta_ia");
		return lastResponse != null && lastResponse.contains("\n1.")
				&& (lastResponse.toLowerCase(java.util.Locale.ROOT).contains("opciones disponibles")
						|| lastResponse.toLowerCase(java.util.Locale.ROOT).contains("cuál prefieres")
						|| context.primaryIntent() == AgentIntent.AVAILABILITY_QUERY);
	}

	private void enrichTurnContext(Map<String, String> entities, AgentConversationRequest request) {
		if (request.messageBody() != null && !request.messageBody().isBlank()) {
			entities.put("ultimo_mensaje_cliente", request.messageBody().trim());
		}
		if (request.occurredAt() != null) {
			entities.put("timestamp_ultimo_turno", request.occurredAt().toString());
		}
	}

	private void enrichResultContext(AgentRoutingResult result) {
		if (result == null || result.extractedData() == null) {
			return;
		}
		if (result.responseToCustomer() != null && !result.responseToCustomer().isBlank()) {
			result.extractedData().put("ultima_respuesta_ia", result.responseToCustomer());
		}
		if (result.missingData() != null && !result.missingData().isEmpty()) {
			result.extractedData().put("ultimo_dato_solicitado", String.join(",", result.missingData()));
		} else {
			result.extractedData().remove("ultimo_dato_solicitado");
		}
	}

	private boolean containsLink(String response) {
		return response != null && (response.contains("/reservar") || response.contains("/reservas/confirmar/"));
	}

	private Map<String, String> mergeEntities(
			Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext,
			Map<String, String> currentEntities, IntentDetectionResult currentIntent) {
		Map<String, String> merged = new LinkedHashMap<>();
		boolean reuseBookingContext = shouldReuseBookingContext(previousContext, currentIntent, currentEntities);
		previousContext.ifPresent(context -> context.extractedData().forEach((key, value) -> {
			if (value == null || value.isBlank() || TRANSIENT_CONTEXT_KEYS.contains(key)) {
				return;
			}
			if (BOOKING_CONTEXT_KEYS.contains(key) && !reuseBookingContext) {
				return;
			}
			merged.put(key, value);
		}));
		merged.putAll(currentEntities);
		return merged;
	}

	private boolean shouldReuseBookingContext(
			Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext,
			IntentDetectionResult currentIntent, Map<String, String> currentEntities) {
		if (previousContext.isEmpty() || currentIntent == null || currentEntities == null) {
			return false;
		}
		AiAgentJdbcRepository.ConversationContextSnapshot context = previousContext.get();
		if (context.activeAgent() != AgentType.BOOKING || context.primaryIntent() == null) {
			return false;
		}
		if (!isBookingContinuationIntent(currentIntent.primaryIntent())) {
			return false;
		}
		if (hasConflictingService(context.extractedData(), currentEntities)) {
			return false;
		}

		List<String> missing = context.missingData() == null ? List.of() : context.missingData();
		boolean answersRequestedSlot = (missing.contains("motivo_o_servicio")
				&& has(currentEntities, "servicio_o_producto"))
				|| (missing.contains("sucursal") && has(currentEntities, "sede"))
				|| (missing.contains("fecha_deseada")
						&& (has(currentEntities, "fecha") || has(currentEntities, "fecha_relativa")))
				|| (missing.contains("horario_preferido")
						&& (has(currentEntities, "hora") || has(currentEntities, "tramo_horario")))
				|| (missing.contains("profesional") && has(currentEntities, "profesional"));
		boolean addsBookingSlot = has(currentEntities, "servicio_o_producto") || has(currentEntities, "sede")
				|| has(currentEntities, "fecha") || has(currentEntities, "fecha_relativa")
				|| has(currentEntities, "hora") || has(currentEntities, "tramo_horario");
		boolean asksAvailabilityForPendingTime = currentIntent.primaryIntent() == AgentIntent.AVAILABILITY_QUERY
				&& missing.contains("horario_preferido") && has(context.extractedData(), "servicio_o_producto")
				&& has(context.extractedData(), "sede")
				&& (has(context.extractedData(), "fecha") || has(context.extractedData(), "fecha_relativa"));
		return answersRequestedSlot || addsBookingSlot || asksAvailabilityForPendingTime;
	}

	private boolean isBookingContinuationIntent(AgentIntent intent) {
		return intent == AgentIntent.AMBIGUOUS || intent == AgentIntent.GREETING
				|| intent == AgentIntent.BOOKING_REQUEST || intent == AgentIntent.COMMERCIAL_AND_BOOKING
				|| intent == AgentIntent.SERVICE_INFORMATION || intent == AgentIntent.SERVICE_RECOMMENDATION
				|| intent == AgentIntent.AVAILABILITY_QUERY || intent == AgentIntent.LOCATION_QUERY;
	}

	private boolean hasConflictingService(Map<String, String> previousEntities, Map<String, String> currentEntities) {
		String previousService = value(previousEntities, "servicio_o_producto");
		String currentService = value(currentEntities, "servicio_o_producto");
		return !previousService.isBlank() && !currentService.isBlank()
				&& !TextNormalizer.normalize(previousService).equals(TextNormalizer.normalize(currentService));
	}

	private boolean has(Map<String, String> entities, String key) {
		return entities != null && value(entities, key) != null && !value(entities, key).isBlank();
	}

	private String value(Map<String, String> entities, String key) {
		if (entities == null) {
			return "";
		}
		String value = entities.get(key);
		return value == null ? "" : value.trim();
	}

	private IntentDetectionResult resolveContextAwareIntent(IntentDetectionResult current,
			Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext, Map<String, String> entities) {
		if (has(entities, "opcion_agenda_seleccionada")) {
			return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.83, "bajo", false, null);
		}
		if (continuesPreviousAvailabilityQuery(current, previousContext, entities)) {
			return new IntentDetectionResult(AgentIntent.AVAILABILITY_QUERY, null, 0.83, "bajo", false, null);
		}
		if (current.primaryIntent() == AgentIntent.BOOKING_CANCEL
				|| current.primaryIntent() == AgentIntent.BOOKING_CHANGE
				|| current.primaryIntent() == AgentIntent.BOOKING_STATUS
				|| current.primaryIntent() == AgentIntent.AVAILABILITY_QUERY
				|| current.primaryIntent() == AgentIntent.SERVICE_RECOMMENDATION
				|| current.primaryIntent() == AgentIntent.SERVICE_INFORMATION
				|| current.primaryIntent() == AgentIntent.PROFESSIONAL_QUERY
				|| current.primaryIntent() == AgentIntent.LOCATION_QUERY
				|| current.primaryIntent() == AgentIntent.BUSINESS_HOURS_QUERY
				|| current.primaryIntent() == AgentIntent.HUMAN_REQUEST
				|| current.primaryIntent() == AgentIntent.PRICE_REQUEST
				|| current.primaryIntent() == AgentIntent.WAITLIST_QUERY) {
			return current;
		}
		if (previousContext.isPresent()) {
			AiAgentJdbcRepository.ConversationContextSnapshot context = previousContext.get();
			String pendingAction = context.extractedData().getOrDefault("accion_pendiente", "");
			if (!pendingAction.isBlank() && context.activeAgent() == AgentType.BOOKING
					&& (context.primaryIntent() == AgentIntent.BOOKING_CANCEL
							|| context.primaryIntent() == AgentIntent.BOOKING_CHANGE)) {
				return new IntentDetectionResult(context.primaryIntent(), context.secondaryIntent(), 0.86, "medio",
						false, null);
			}
			boolean previousWasBooking = context.primaryIntent() == AgentIntent.BOOKING_REQUEST
					|| context.primaryIntent() == AgentIntent.BOOKING_CHANGE
					|| context.primaryIntent() == AgentIntent.BOOKING_CANCEL
					|| context.primaryIntent() == AgentIntent.COMMERCIAL_AND_BOOKING;
			boolean bookingWasWaitingForService = context.missingData().contains("motivo_o_servicio");
			boolean bookingWasWaitingForDate = context.missingData().contains("fecha_deseada");
			boolean bookingWasWaitingForTime = context.missingData().contains("horario_preferido");
			boolean currentHasService = entities.containsKey("servicio_o_producto");
			boolean currentHasDate = entities.containsKey("fecha") || entities.containsKey("fecha_relativa");
			boolean currentHasTime = entities.containsKey("hora");

			if (previousWasBooking && ((bookingWasWaitingForService && currentHasService)
					|| (bookingWasWaitingForDate && currentHasDate) || (bookingWasWaitingForTime && currentHasTime)
					|| (currentHasService && (currentHasDate || currentHasTime)))) {
				return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.83, "bajo", false, null);
			}

			if (current.primaryIntent() == AgentIntent.GREETING && previousWasBooking && hasOpenBookingData(context)) {
				return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.7, "bajo", false, null);
			}
		}

		if (current.primaryIntent() != AgentIntent.AMBIGUOUS && current.primaryIntent() != AgentIntent.GREETING) {
			return current;
		}
		if (entities.containsKey("servicio_o_producto")) {
			AgentIntent previous = previousContext.map(AiAgentJdbcRepository.ConversationContextSnapshot::primaryIntent)
					.orElse(AgentIntent.COMMERCIAL_INQUIRY);
			AgentIntent resolved = switch (previous) {
				case BOOKING_CHANGE -> AgentIntent.BOOKING_CHANGE;
				case BOOKING_CANCEL -> AgentIntent.BOOKING_CANCEL;
				case BOOKING_REQUEST, BOOKING_STATUS, COMMERCIAL_AND_BOOKING -> AgentIntent.BOOKING_REQUEST;
				default -> AgentIntent.COMMERCIAL_INQUIRY;
			};
			return new IntentDetectionResult(resolved, null, 0.78, "bajo", false, null);
		}
		if (entities.containsKey("nombre") && previousContext.isPresent()) {
			AiAgentJdbcRepository.ConversationContextSnapshot context = previousContext.get();
			if (context.activeAgent() == AgentType.SALES && context.primaryIntent() != null) {
				return new IntentDetectionResult(context.primaryIntent(), context.secondaryIntent(), 0.72, "bajo",
						false, null);
			}
			if (context.activeAgent() == AgentType.BOOKING && context.primaryIntent() != null) {
				return new IntentDetectionResult(context.primaryIntent(), context.secondaryIntent(), 0.72, "bajo",
						false, null);
			}
		}
		if (current.primaryIntent() == AgentIntent.AMBIGUOUS && previousContext.isPresent()) {
			AiAgentJdbcRepository.ConversationContextSnapshot context = previousContext.get();
			if (hasOpenBookingData(context)) {
				String message = value(entities, "ultimo_mensaje_cliente").toLowerCase(java.util.Locale.ROOT);
				if (matchesCancelFollowUp(message)) {
					return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.82, "medio", false, null);
				}
				if (matchesStatusFollowUp(message)) {
					return new IntentDetectionResult(AgentIntent.BOOKING_STATUS, null, 0.82, "medio", false, null);
				}
				if (matchesContinueBooking(message)) {
					return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.78, "bajo", false, null);
				}
				if (isAffirmative(message)) {
					return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.78, "bajo", false, null);
				}
			}
		}
		return current;
	}

	private boolean matchesCancelFollowUp(String message) {
		if (message == null || message.isBlank())
			return false;
		String n = TextNormalizer.normalize(message);
		return n.contains("no voy a poder ir") || n.contains("no puedo ir") || n.contains("no voy a ir")
				|| n.contains("no pude asistir") || n.contains("no poder asistir");
	}

	private boolean matchesStatusFollowUp(String message) {
		if (message == null || message.isBlank())
			return false;
		String n = TextNormalizer.normalize(message);
		return n.contains("ya pague") || n.contains("ya pagué") || n.contains("esta listo") || n.contains("está listo")
				|| n.contains("todavia sirve") || n.contains("todavía sirve") || n.contains("quiero confirmar")
				|| n.equals("listo") || n.contains("confirmar mi cita") || n.contains("confirmar mi reserva")
				|| n.contains("confirmar mi hora");
	}

	private boolean matchesContinueBooking(String message) {
		if (message == null || message.isBlank())
			return false;
		String n = TextNormalizer.normalize(message);
		return n.contains("la misma de la otra vez") || n.contains("el tratamiento anterior")
				|| n.contains("a la misma hora") || n.contains("con ella") || n.contains("con el")
				|| n.contains("no quiero ese") || n.contains("no quiero esa") || n.contains("quiero otra opcion")
				|| n.contains("quiero otra opción") || n.contains("la de la otra vez") || n.contains("el anterior")
				|| n.contains("lo mismo de antes") || n.contains("la misma hora") || n.contains("quiero lo mismo");
	}

	private boolean isAffirmative(String message) {
		if (message == null || message.isBlank()) {
			return false;
		}
		String normalized = TextNormalizer.normalize(message).trim().toLowerCase(java.util.Locale.ROOT);
		if (AFFIRMATIVE_WORDS.contains(normalized)) {
			return true;
		}
		String firstWord = normalized.split("\\s+")[0];
		return AFFIRMATIVE_WORDS.contains(firstWord);
	}

	private boolean continuesPreviousAvailabilityQuery(IntentDetectionResult current,
			Optional<AiAgentJdbcRepository.ConversationContextSnapshot> previousContext, Map<String, String> entities) {
		if (current == null || previousContext.isEmpty() || !has(entities, "servicio_o_producto")) {
			return false;
		}
		AiAgentJdbcRepository.ConversationContextSnapshot context = previousContext.get();
		List<String> missing = context.missingData() == null ? List.of() : context.missingData();
		return context.activeAgent() == AgentType.BOOKING && context.primaryIntent() == AgentIntent.AVAILABILITY_QUERY
				&& missing.contains("servicio_o_producto")
				&& (current.primaryIntent() == AgentIntent.SERVICE_INFORMATION
						|| current.primaryIntent() == AgentIntent.SERVICE_RECOMMENDATION
						|| current.primaryIntent() == AgentIntent.AMBIGUOUS);
	}

	private boolean hasOpenBookingData(AiAgentJdbcRepository.ConversationContextSnapshot context) {
		return context.extractedData().containsKey("servicio_o_producto")
				|| context.extractedData().containsKey("fecha") || context.extractedData().containsKey("fecha_relativa")
				|| context.extractedData().containsKey("hora") || context.missingData().contains("motivo_o_servicio")
				|| context.missingData().contains("fecha_deseada")
				|| context.missingData().contains("horario_preferido");
	}

	private boolean isNonActionableMessage(String value) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
		return normalized.equals("mensaje recibido sin texto") || normalized.equals("mensaje recibido sin texto.");
	}

	public boolean autoReplyEnabled() {
		return properties.enabled() && properties.autoReplyEnabled();
	}
}
