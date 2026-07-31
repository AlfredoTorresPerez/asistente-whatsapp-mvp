package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.bookings.api.PublicBookingConfirmationResponse;
import com.asistentewhatsapp.bookings.application.BookingConfirmationService;
import com.asistentewhatsapp.bookings.application.BookingStateMachine;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationLinkRecord;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingAgent extends AbstractAgentHandler {

	private final AiBusinessKnowledgeService knowledgeService;
	private final TransactionalAgendaBookingService transactionalAgendaBookingService;
	private final BookingConfirmationJdbcRepository confirmationRepository;
	private final BookingConfirmationService bookingConfirmationService;
	private final CompleteAgendaJdbcRepository agendaRepository;

	public BookingAgent(AiBusinessKnowledgeService knowledgeService,
			TransactionalAgendaBookingService transactionalAgendaBookingService,
			BookingConfirmationJdbcRepository confirmationRepository,
			BookingConfirmationService bookingConfirmationService, CompleteAgendaJdbcRepository agendaRepository) {
		this.knowledgeService = knowledgeService;
		this.transactionalAgendaBookingService = transactionalAgendaBookingService;
		this.confirmationRepository = confirmationRepository;
		this.bookingConfirmationService = bookingConfirmationService;
		this.agendaRepository = agendaRepository;
	}

	@Override
	public AgentType type() {
		return AgentType.BOOKING;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		String traceId = AiTraceLogger.traceId(request);
		String normalizedMessage = TextNormalizer.normalize(request.messageBody());
		synchronizeBookingIntentEntity(intent, entities);
		AiTraceLogger.info("BOOKING_AGENT_STARTED", traceId, request.conversationId(), null, "BookingAgent",
				"intent=" + intent.primaryIntent() + " normalizedMessage=" + normalizedMessage + " entities="
						+ AiTraceLogger.summarizeMap(entities));

		if (containsAny(normalizedMessage, "enlace expiro", "link expiro", "enlace vencio", "link vencio",
				"no funciona el enlace", "no funciona el link", "me dice expirado")) {
			return result(request, intent, type(), entities, missing("reserva_temporal_vigente"),
					WhatsAppMessageFormatter.confirmationLinkExpired(), false, null);
		}

		if (containsAny(normalizedMessage, "no me llego el link", "no me llego el enlace", "reenviar", "reenvia",
				"mandame el link", "mandame el enlace", "no recibi la confirmacion")) {
			return result(request, intent, type(), entities, missing("reserva_temporal_vigente"),
					"🔁 *Reenvío de enlace de confirmación*\n\nRevisaré si tienes una reserva temporal vigente para reenviar el enlace de confirmación.\n\nSi no la encuentro, te pediré los datos mínimos para crear una nueva reserva.",
					false, null);
		}

		if (intent.primaryIntent() == AgentIntent.WAITLIST_QUERY) {
			return result(request, intent, type(), entities, missing("servicio_o_producto"),
					"Puedo revisar la lista de espera. ¿Para qué servicio o tratamiento necesitas anotarte?", false,
					null);
		}

		if (intent.primaryIntent() == AgentIntent.BOOKING_CHANGE) {
			String response = transactionalAgendaBookingService.handleRescheduleBookingFromWhatsApp(
					request.businessId(), request.customerId(), request.conversationId(), request.customerPhone(),
					request.messageBody(), entities, traceId, request.conversationId());
			if (response == null || response.isBlank()) {
				response = WhatsAppMessageFormatter.rescheduleRequest();
			}
			return result(request, intent, type(), entities, bookingFlowMissingData(entities), response, false, null);
		}

		if (intent.primaryIntent() == AgentIntent.BOOKING_CANCEL) {
			if (containsAny(normalizedMessage, "otra persona", "de otro", "para otro")) {
				return result(request, intent, type(), entities, List.of(),
						"Por tratarse de una reserva de otra persona, te derivaré con una persona del equipo para que pueda ayudarte directamente.",
						true, null);
			}
			String response = transactionalAgendaBookingService.handleCancelBookingFromWhatsApp(request.businessId(),
					request.customerId(), request.conversationId(), request.customerPhone(), request.messageBody(),
					entities, traceId, request.conversationId());
			if (response == null || response.isBlank()) {
				response = WhatsAppMessageFormatter.cancellationRequest();
			}
			return result(request, intent, type(), entities, bookingFlowMissingData(entities), response, false, null);
		}

		if (intent.primaryIntent() == AgentIntent.BOOKING_STATUS) {
			String statusResponse;
			if (containsAny(normalizedMessage, "pendiente de recepcion", "pendiente de recepción",
					"esperando confirmacion", "esperando confirmación")) {
				statusResponse = "Puedo verificar el estado de tu reserva. Para identificarla, indícame tu nombre, teléfono o la fecha de atención.";
			} else {
				statusResponse = knowledgeService.renderRule(request.businessId(),
						"AI_BOOKING_STATUS_IDENTIFY_RESPONSE", Map.of());
			}
			return result(request, intent, type(), entities, missing("reserva_temporal_o_confirmada"), statusResponse,
					false, null);
		}

		if (intent.primaryIntent() == AgentIntent.AVAILABILITY_QUERY) {
			return handleAvailabilityQuery(request, intent, entities, traceId);
		}

		if (intent.primaryIntent() == AgentIntent.PROFESSIONAL_QUERY
				&& !isExplicitBookingLinkRequest(normalizedMessage)) {
			return handleProfessionalQuery(request, intent, entities);
		}

		if (!isExplicitBookingLinkRequest(normalizedMessage)) {
			return handleBookingRequest(request, intent, entities, traceId);
		}

		TransactionalAgendaBookingService.BookingLinkResult linkResult = transactionalAgendaBookingService
				.generateBookingLink(request.businessId(), request.customerPhone(), request.conversationId(),
						request.customerId());
		String response = WhatsAppMessageFormatter.bookingLink(linkResult.url(), linkResult.isKnownCustomer());
		AiTraceLogger.info("BOOKING_LINK_GENERATED", traceId, request.conversationId(), null, "BookingAgent",
				"isKnownCustomer=" + linkResult.isKnownCustomer() + " url=" + linkResult.url());
		AiTraceLogger.info("AI_FINAL_RESPONSE", traceId, request.conversationId(), null, "BookingAgent",
				"intent=" + intent.primaryIntent() + " containsLink=" + response.contains("/reservar") + " "
						+ LogSanitizer.responseSummary(response));
		return result(request, intent, type(), entities, List.of(), response, false, null);
	}

	private AgentRoutingResult handleAvailabilityQuery(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, String traceId) {
		List<String> missingData = availabilityMissingData(entities);
		if (!missingData.isEmpty()) {
			String response = availabilityMissingDataResponse(entities, missingData);
			traceLinkDecision(request, traceId, intent, entities, "ASK_MISSING_DATA", "missing=" + missingData);
			return result(request, intent, type(), entities, missingData, response, false, null);
		}

		TransactionalAgendaBookingService.BookingLinkResult linkResult = transactionalAgendaBookingService
				.generateBookingLink(request.businessId(), request.customerPhone(), request.conversationId(),
						request.customerId());
		Optional<String> response = transactionalAgendaBookingService.checkAvailability(request.businessId(),
				request.messageBody(), value(entities, "servicio_o_producto"), value(entities, "sede"),
				firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa")), value(entities, "hora"),
				value(entities, "tramo_horario"), linkResult.url(), traceId, request.conversationId());
		String body = response
				.orElseGet(() -> availabilityMissingDataResponse(entities, availabilityMissingData(entities)));
		List<String> nextMissingData = containsNumberedOptions(body) ? missing("horario_preferido") : List.of();
		traceLinkDecision(request, traceId, intent, entities, "LOOKUP_AVAILABILITY",
				"containsLink=" + containsLink(body));
		return result(request, intent, type(), entities, nextMissingData, body, false, null);
	}

	private AgentRoutingResult handleProfessionalQuery(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities) {
		String professional = value(entities, "profesional");
		String response = professional.isBlank()
				? "Puedo revisar profesionales disponibles. ¿Con qué profesional o para qué servicio necesitas atención?"
				: "Puedo revisar disponibilidad con " + professional + ". ¿Qué servicio y qué día necesitas consultar?";
		return result(request, intent, type(), entities, missing("servicio_o_producto", "fecha_deseada"), response,
				false, null);
	}

	private AgentRoutingResult handleBookingRequest(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, String traceId) {
		List<String> missingData = bookingMissingData(entities);
		if (!missingData.isEmpty()) {
			if (missingData.size() == 1 && missingData.contains("horario_preferido")
					&& has(entities, "tramo_horario")) {
				TransactionalAgendaBookingService.BookingLinkResult linkResult = transactionalAgendaBookingService
						.generateBookingLink(request.businessId(), request.customerPhone(), request.conversationId(),
								request.customerId());
				Optional<String> availability = transactionalAgendaBookingService.checkAvailability(
						request.businessId(), request.messageBody(), value(entities, "servicio_o_producto"),
						value(entities, "sede"),
						firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa")),
						value(entities, "hora"), value(entities, "tramo_horario"), linkResult.url(), traceId,
						request.conversationId());
				if (availability.isPresent()) {
					traceLinkDecision(request, traceId, intent, entities, "LOOKUP_AVAILABILITY_BEFORE_BOOKING",
							"containsLink=" + containsLink(availability.get()));
					return result(request, intent, type(), entities, missing("horario_preferido"), availability.get(),
							false, null);
				}
			}
			String response = bookingMissingDataResponse(request.businessId(), entities, missingData);
			traceLinkDecision(request, traceId, intent, entities, "ASK_MISSING_DATA", "missing=" + missingData);
			return result(request, intent, type(), entities, missingData, response, false, null);
		}

		Optional<String> chatConfirmation = handleChatBookingConfirmation(request, entities, traceId);
		if (chatConfirmation.isPresent()) {
			return result(request, intent, type(), entities, List.of(), chatConfirmation.get(), false, null);
		}

		Optional<String> transactionalResponse = transactionalAgendaBookingService.createTemporaryBookingLink(
				request.businessId(), request.customerId(), request.conversationId(), request.customerDisplayName(),
				request.customerPhone(), request.messageBody(), value(entities, "servicio_o_producto"),
				value(entities, "sede"), firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa")),
				value(entities, "hora"), false, request.dryRun(), traceId, request.conversationId());
		String response = transactionalResponse.orElse(
				"Tengo los datos principales, pero no pude validar la disponibilidad en agenda. ¿Quieres que lo intente nuevamente o te derive con una persona?");
		traceLinkDecision(request, traceId, intent, entities, "CREATE_TEMPORARY_BOOKING",
				"containsLink=" + containsLink(response));
		return result(request, intent, type(), entities, List.of(), response, false, null);
	}

	private List<String> availabilityMissingData(Map<String, String> entities) {
		if (!has(entities, "servicio_o_producto")) {
			return missing("servicio_o_producto");
		}
		if (!has(entities, "sede")) {
			return missing("sucursal");
		}
		if (!has(entities, "fecha") && !has(entities, "fecha_relativa")) {
			return missing("fecha_deseada");
		}
		return List.of();
	}

	private List<String> bookingMissingData(Map<String, String> entities) {
		java.util.ArrayList<String> missing = new java.util.ArrayList<>();
		if (!has(entities, "servicio_o_producto")) {
			missing.add("motivo_o_servicio");
		}
		if (!has(entities, "sede")) {
			missing.add("sucursal");
		}
		if (!has(entities, "fecha") && !has(entities, "fecha_relativa")) {
			missing.add("fecha_deseada");
		}
		if (!missing.isEmpty()) {
			return missing;
		}
		if (!has(entities, "hora")) {
			missing.add("horario_preferido");
		}
		return missing;
	}

	private String availabilityMissingDataResponse(Map<String, String> entities, List<String> missingData) {
		String service = safe(value(entities, "servicio_o_producto"), null);
		String location = safe(value(entities, "sede"), null);
		String date = firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa"));
		if (missingData.contains("servicio_o_producto")) {
			String suffix = locationAndDateSuffix(entities);
			return "Sí, puedo revisarlo" + suffix + ". ¿Para qué servicio necesitas una hora?";
		}
		if (missingData.contains("sucursal")) {
			String prefix = service != null ? service + " " : "";
			return "Perfecto, reviso " + prefix + ". ¿En qué sucursal quieres consultar disponibilidad?";
		}
		if (date == null || date.isBlank()) {
			return "Perfecto, reviso " + safe(service, "ese servicio") + " en " + safe(location, "la sucursal")
					+ ". ¿Para qué día quieres consultar?";
		}
		return "Perfecto, reviso " + safe(service, "ese servicio") + " en " + safe(location, "la sucursal") + " para "
				+ date + ". ¿Qué horario prefieres?";
	}

	private String bookingMissingDataResponse(UUID businessId, Map<String, String> entities, List<String> missingData) {
		String service = value(entities, "servicio_o_producto");
		String professional = value(entities, "profesional");
		if (missingData.contains("motivo_o_servicio")) {
			if (!professional.isBlank()) {
				return "Claro, puedo revisar horarios con " + professional + ". ¿Qué servicio y qué día necesitas?";
			}
			return knowledgeService.bookingMissingServiceResponse(businessId);
		}
		if (missingData.contains("sucursal") && missingData.contains("fecha_deseada")) {
			return "Claro. Ya tengo " + safe(service, "el servicio")
					+ ". ¿En qué sucursal y para qué día te gustaría reservar?";
		}
		if (missingData.contains("sucursal")) {
			return "Claro. Ya tengo " + safe(service, "el servicio") + " para "
					+ firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa"))
					+ ". ¿En qué sucursal te gustaría reservar?";
		}
		if (missingData.contains("fecha_deseada")) {
			return "Claro. Ya tengo " + safe(service, "el servicio") + " en "
					+ safe(value(entities, "sede"), "la sucursal") + ". ¿Para qué día te gustaría reservar?";
		}
		return "Perfecto, ya tengo " + safe(service, "el servicio") + " en "
				+ safe(value(entities, "sede"), "la sucursal") + " para "
				+ firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa"))
				+ ". ¿Qué horario prefieres?";
	}

	private String locationAndDateSuffix(Map<String, String> entities) {
		String location = value(entities, "sede");
		String date = firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa"));
		if (!location.isBlank() && !date.isBlank()) {
			return " para " + date + " en " + location;
		}
		if (!location.isBlank()) {
			return " en " + location;
		}
		if (!date.isBlank()) {
			return " para " + date;
		}
		return "";
	}

	private boolean isExplicitBookingLinkRequest(String normalizedMessage) {
		return containsAny(normalizedMessage, "link para reservar", "enlace para reservar",
				"mandame el link de reserva", "mandame el enlace de reserva", "enviame el link de reserva",
				"enviame el enlace de reserva");
	}

	private boolean containsLink(String response) {
		return response != null && (response.contains("/reservar") || response.contains("/reservas/confirmar/"));
	}

	private boolean containsNumberedOptions(String response) {
		return response != null && response.contains("\n1.");
	}

	private void traceLinkDecision(AgentConversationRequest request, String traceId, IntentDetectionResult intent,
			Map<String, String> entities, String action, String reason) {
		AiTraceLogger.info("CONVERSATIONAL_ACTION_SELECTED", traceId, request.conversationId(), null, "BookingAgent",
				"intent=" + intent.primaryIntent() + " secondary=" + intent.secondaryIntent() + " action=" + action
						+ " entities=" + AiTraceLogger.summarizeMap(entities) + " linkPolicy=" + reason);
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

	private String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first.trim() : (second == null ? "" : second.trim());
	}

	private String safe(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	private Optional<String> handleChatBookingConfirmation(AgentConversationRequest request,
			Map<String, String> entities, String traceId) {
		String hora = value(entities, "hora");
		if (hora.isBlank()) {
			return Optional.empty();
		}
		Optional<ConfirmationLinkRecord> pending = confirmationRepository
				.findLatestActionableByConversation(request.businessId(), request.conversationId());
		if (pending.isEmpty()) {
			return Optional.empty();
		}
		ConfirmationLinkRecord link = pending.get();
		Optional<ZoneId> zone = resolveBookingZone(request.businessId(), link);
		if (zone.isEmpty()) {
			return Optional.empty();
		}
		if (!matchesRequestedSlot(link, hora, entities, zone.get())) {
			return Optional.empty();
		}
		String token = tokenFromUrl(link.confirmationUrl());
		if (token.isBlank()) {
			return Optional.empty();
		}
		if (BookingStateMachine.CONFIRMED.equals(BookingStateMachine.canonical(link.bookingStatus()))) {
			AiTraceLogger.info("CHAT_BOOKING_ALREADY_CONFIRMED", traceId, request.conversationId(), link.bookingId(),
					"BookingAgent", "bookingId=" + link.bookingId() + " status=" + link.bookingStatus());
			return Optional.of(chatBookingStatusResponse(link, zone.get(), true));
		}
		try {
			PublicBookingConfirmationResponse confirmed = bookingConfirmationService.confirm(token);
			AiTraceLogger.info("CHAT_BOOKING_CONFIRMED", traceId, request.conversationId(), link.bookingId(),
					"BookingAgent", "bookingId=" + link.bookingId() + " status=" + confirmed.bookingStatus()
							+ " source=CHAT_CONFIRMATION");
			return Optional.of(chatBookingStatusResponse(link, zone.get(), false));
		} catch (RuntimeException exception) {
			AiTraceLogger.warn("CHAT_BOOKING_CONFIRMATION_FAILED", traceId, request.conversationId(), link.bookingId(),
					"BookingAgent", "errorType=" + exception.getClass().getSimpleName() + " functionalMessage="
							+ LogSanitizer.sanitizeFreeText(exception.getMessage()));
			return Optional.of(
					"⚠️ No pude confirmar la reserva por este medio.\n\nPuedes tocar el enlace de confirmación que te envié para confirmarla, o te derivo con una persona del equipo.");
		}
	}

	private boolean matchesRequestedSlot(ConfirmationLinkRecord link, String hora, Map<String, String> entities,
			ZoneId zone) {
		LocalTime requestedTime = parseHora(hora);
		if (requestedTime == null) {
			return false;
		}
		LocalTime slotTime = link.startsAt().atZoneSameInstant(zone).toLocalTime();
		if (!requestedTime.equals(slotTime)) {
			return false;
		}
		LocalDate slotDate = link.startsAt().atZoneSameInstant(zone).toLocalDate();
		String fecha = firstNonBlank(value(entities, "fecha"), value(entities, "fecha_relativa"));
		if (fecha.isBlank()) {
			return false;
		}
		if ("hoy".equalsIgnoreCase(fecha.trim()) || "el dia de hoy".equalsIgnoreCase(fecha.trim())) {
			return slotDate.equals(LocalDate.now(zone));
		}
		LocalDate requestedDate = parseFecha(fecha);
		return requestedDate != null && requestedDate.equals(slotDate);
	}

	private Optional<ZoneId> resolveBookingZone(UUID businessId, ConfirmationLinkRecord link) {
		if (link.locationId() == null) {
			return Optional.empty();
		}
		try {
			CompleteAgendaJdbcRepository.LocationRecord location = agendaRepository.findLocation(businessId,
					link.locationId());
			if (location == null || location.timezone() == null || location.timezone().isBlank()) {
				return Optional.empty();
			}
			return Optional.of(ZoneId.of(location.timezone()));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	private LocalTime parseHora(String hora) {
		String normalized = hora.trim().toLowerCase(java.util.Locale.ROOT).replace("hrs", "").replace(" horas", "")
				.replace("am", "").replace("pm", "").trim();
		try {
			return LocalTime.parse(normalized);
		} catch (RuntimeException first) {
			try {
				return LocalTime.of(Integer.parseInt(normalized), 0);
			} catch (RuntimeException second) {
				return null;
			}
		}
	}

	private LocalDate parseFecha(String fecha) {
		String normalized = fecha.trim();
		try {
			return LocalDate.parse(normalized);
		} catch (RuntimeException first) {
			try {
				return LocalDate.parse(normalized, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
			} catch (RuntimeException second) {
				return null;
			}
		}
	}

	private String tokenFromUrl(String confirmationUrl) {
		if (confirmationUrl == null || confirmationUrl.isBlank()) {
			return "";
		}
		int index = confirmationUrl.lastIndexOf('/');
		return index >= 0 && index + 1 < confirmationUrl.length() ? confirmationUrl.substring(index + 1).trim() : "";
	}

	private String chatBookingStatusResponse(ConfirmationLinkRecord link, ZoneId zone, boolean alreadyConfirmed) {
		String fecha = link.startsAt().atZoneSameInstant(zone).toLocalDate()
				.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		String hora = link.startsAt().atZoneSameInstant(zone).toLocalTime().toString();
		String header = alreadyConfirmed ? "✅ *Reserva ya confirmada*" : "✅ *Reserva confirmada*";
		String intro = alreadyConfirmed
				? "Tu reserva ya estaba confirmada. La dejamos tal cual:"
				: "Perfecto, tu reserva quedó confirmada:";
		return header + "\n\n" + intro + "\n\n" + "*Servicio:* " + safe(link.serviceName(), "No especificado") + "\n"
				+ "*Sucursal:* " + safe(link.locationName(), "No especificada") + "\n" + "*Fecha:* " + fecha + "\n"
				+ "*Hora:* " + hora + "\n" + "*Profesional:* " + safe(link.professionalName(), "A asignar") + "\n"
				+ "*Cabina:* " + safe(link.roomName(), "A asignar") + "\n\n"
				+ "¡Te esperamos! Recuerda llegar unos minutos antes. El consentimiento informado se firma en el centro antes de la sesión.";
	}

}
