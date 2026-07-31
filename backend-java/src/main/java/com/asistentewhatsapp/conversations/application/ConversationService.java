package com.asistentewhatsapp.conversations.application;

import com.asistentewhatsapp.aesthetic.api.AestheticServiceResponse;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.aiagents.application.AgentConversationRequest;
import com.asistentewhatsapp.aiagents.application.AgentCoordinatorService;
import com.asistentewhatsapp.aiagents.application.AgentRoutingResult;
import com.asistentewhatsapp.aiagents.application.AiTraceLogger;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.conversations.api.AssignConversationRequest;
import com.asistentewhatsapp.conversations.api.ConversationAiReplyResponse;
import com.asistentewhatsapp.conversations.api.ConversationDetailResponse;
import com.asistentewhatsapp.conversations.api.ConversationMessageResponse;
import com.asistentewhatsapp.conversations.api.ConversationMetricsResponse;
import com.asistentewhatsapp.conversations.api.ConversationSummaryResponse;
import com.asistentewhatsapp.conversations.api.CreateConversationRequest;
import com.asistentewhatsapp.conversations.api.SendConversationMessageRequest;
import com.asistentewhatsapp.conversations.infrastructure.ConversationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import com.asistentewhatsapp.shared.exception.UnsupportedMessagingChannelException;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ConversationService {

	private final ConversationJdbcRepository conversationJdbcRepository;
	private final ChannelDispatchService channelDispatchService;
	private final TemplateVariableRenderer templateVariableRenderer;
	private final AgentCoordinatorService agentCoordinatorService;
	private final AestheticCenterJdbcRepository aestheticCenterJdbcRepository;
	private final TransactionTemplate transactionTemplate;

	public ConversationService(ConversationJdbcRepository conversationJdbcRepository,
			ChannelDispatchService channelDispatchService, TemplateVariableRenderer templateVariableRenderer,
			AgentCoordinatorService agentCoordinatorService,
			AestheticCenterJdbcRepository aestheticCenterJdbcRepository,
			PlatformTransactionManager transactionManager) {
		this.conversationJdbcRepository = conversationJdbcRepository;
		this.channelDispatchService = channelDispatchService;
		this.templateVariableRenderer = templateVariableRenderer;
		this.agentCoordinatorService = agentCoordinatorService;
		this.aestheticCenterJdbcRepository = aestheticCenterJdbcRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Transactional(readOnly = true)
	public ConversationMetricsResponse getMetrics(AuthenticatedUser authenticatedUser) {
		return conversationJdbcRepository.findConversationMetrics(authenticatedUser.businessId());
	}

	@Transactional(readOnly = true)
	public PagedResponse<ConversationSummaryResponse> list(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String status, UUID ownerUserId) {
		int resolvedPage = Math.max(page, 0);
		int resolvedSize = Math.min(Math.max(size, 1), 100);
		String normalizedSearch = normalizeSearch(search);
		String normalizedStatus = normalizeConversationStatus(status);

		return conversationJdbcRepository.findConversations(authenticatedUser.businessId(), resolvedPage, resolvedSize,
				normalizedSearch, normalizedStatus, ownerUserId);
	}

	@Transactional(readOnly = true)
	public ConversationDetailResponse getDetail(AuthenticatedUser authenticatedUser, UUID conversationId) {
		return conversationJdbcRepository.findConversationDetail(authenticatedUser.businessId(), conversationId);
	}

	@Transactional
	public ConversationDetailResponse create(AuthenticatedUser authenticatedUser, CreateConversationRequest request) {
		ConversationJdbcRepository.ChannelAccountRecord channelAccount = conversationJdbcRepository
				.findPrimaryActiveChannelAccount(authenticatedUser.businessId())
				.orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CHANNEL_NOT_CONFIGURED",
						"No hay un canal de WhatsApp activo configurado para crear conversaciones."));

		UUID assignedUserId = request.ownerUserId() == null
				? authenticatedUser.userId()
				: conversationJdbcRepository.findUserId(authenticatedUser.businessId(), request.ownerUserId())
						.orElseThrow(() -> validationError("ownerUserId", "El responsable indicado no existe."));

		ConversationJdbcRepository.CustomerRecord customer = resolveCustomer(authenticatedUser, request);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		UUID conversationId = conversationJdbcRepository.insertConversation(authenticatedUser.businessId(),
				channelAccount.id(), customer.id(), assignedUserId, customer.displayName(), customer.phone(), now);

		if (request.initialMessage() != null && !request.initialMessage().isBlank()) {
			sendMessageInternal(authenticatedUser,
					conversationJdbcRepository.findConversationContext(authenticatedUser.businessId(), conversationId),
					request.initialMessage().trim());
		}

		return conversationJdbcRepository.findConversationDetail(authenticatedUser.businessId(), conversationId);
	}

	public ConversationMessageResponse sendMessage(AuthenticatedUser authenticatedUser, UUID conversationId,
			SendConversationMessageRequest request) {
		PendingDispatch pendingDispatch = transactionTemplate.execute(status -> {
			ConversationJdbcRepository.ConversationContextRecord conversation = conversationJdbcRepository
					.findConversationContext(authenticatedUser.businessId(), conversationId);

			if ("CLOSED".equals(conversation.status())) {
				throw validationError("conversationId",
						"La conversacion esta cerrada. Reabrela antes de enviar mensajes.");
			}

			String body = resolveMessageBody(authenticatedUser, conversation, request);
			String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
			Optional<ConversationMessageResponse> existingMessage = conversationJdbcRepository
					.findOutboundMessageByIdempotencyKey(authenticatedUser.businessId(), conversation.id(),
							idempotencyKey);
			if (existingMessage.isPresent()) {
				return PendingDispatch.existing(existingMessage.get());
			}
			body = resolveRealAiMessageBodyIfRequired(authenticatedUser, conversation, body, request.aiSource());

			MessageChannelType channelType = MessageChannelType.valueOf(conversation.channelType());
			String recipientAddress = resolveRecipientAddress(authenticatedUser.businessId(), conversation,
					channelType);
			UUID messageId = conversationJdbcRepository.insertOutboundMessage(authenticatedUser.businessId(),
					conversation.id(), authenticatedUser.userId(), body, "PENDING", null, null, idempotencyKey);
			OffsetDateTime pendingAt = OffsetDateTime.now(ZoneOffset.UTC);
			conversationJdbcRepository.insertMessageDeliveryLog(authenticatedUser.businessId(), messageId, "PENDING",
					null, pendingAt);
			return PendingDispatch.pending(conversation, body, channelType, recipientAddress, messageId);
		});

		if (pendingDispatch.existingMessage() != null) {
			return pendingDispatch.existingMessage();
		}

		DispatchOutcome dispatchOutcome = dispatchOutbound(authenticatedUser.businessId(),
				pendingDispatch.conversation().id(), pendingDispatch.messageId(), pendingDispatch.channelType(),
				pendingDispatch.recipientAddress(), pendingDispatch.body());

		transactionTemplate.executeWithoutResult(
				status -> applyDispatchOutcome(authenticatedUser.businessId(), pendingDispatch.conversation().id(),
						pendingDispatch.messageId(), pendingDispatch.body(), dispatchOutcome));
		logDispatchOutcome(pendingDispatch.conversation().id(), pendingDispatch.messageId(), dispatchOutcome);

		return conversationJdbcRepository.findMessageById(authenticatedUser.businessId(),
				pendingDispatch.conversation().id(), pendingDispatch.messageId());
	}

	@Transactional
	public ConversationDetailResponse assign(AuthenticatedUser authenticatedUser, UUID conversationId,
			AssignConversationRequest request) {
		UUID assignedUserId = conversationJdbcRepository.findUserId(authenticatedUser.businessId(), request.userId())
				.orElseThrow(() -> validationError("userId", "El responsable indicado no existe."));

		conversationJdbcRepository.updateConversationAssignment(authenticatedUser.businessId(), conversationId,
				assignedUserId, OffsetDateTime.now(ZoneOffset.UTC));
		return conversationJdbcRepository.findConversationDetail(authenticatedUser.businessId(), conversationId);
	}

	@Transactional
	public ConversationDetailResponse markRead(AuthenticatedUser authenticatedUser, UUID conversationId) {
		conversationJdbcRepository.markConversationRead(authenticatedUser.businessId(), conversationId,
				OffsetDateTime.now(ZoneOffset.UTC));
		return conversationJdbcRepository.findConversationDetail(authenticatedUser.businessId(), conversationId);
	}

	@Transactional
	public ConversationAiReplyResponse previewAiReply(AuthenticatedUser authenticatedUser, UUID conversationId) {
		String traceId = AiTraceLogger.newTraceId("PREVIEW");
		AiTraceLogger.info("PREVIEW_AI_REQUEST_RECEIVED", traceId, conversationId, null, "ConversationService",
				"businessId=" + authenticatedUser.businessId());
		ConversationDetailResponse detail = conversationJdbcRepository
				.findConversationDetail(authenticatedUser.businessId(), conversationId);
		String lastInbound = detail.messages().stream().filter(message -> "INBOUND".equals(message.direction()))
				.map(ConversationMessageResponse::body).filter(body -> !isNonActionableInbound(body))
				.reduce((first, second) -> second)
				.orElse(detail.lastMessagePreview() == null || isNonActionableInbound(detail.lastMessagePreview())
						? ""
						: detail.lastMessagePreview());

		String customerName = detail.customer().displayName();
		String firstName = customerName == null || customerName.isBlank() ? "" : customerName.trim().split("\\s+")[0];
		String greetingName = firstName.isBlank() ? "" : " " + firstName;

		AiTraceLogger.info("CONVERSATION_CONTEXT_LOADED", traceId, detail.id(), null, "ConversationService",
				"customerId=" + detail.customer().id() + " phoneMasked="
						+ AiTraceLogger.maskPhone(detail.customer().phone()) + " conversationLocation="
						+ detail.locationName() + " messages=" + detail.messages().size() + " "
						+ LogSanitizer.messageSummary("lastInbound", lastInbound));

		Optional<AgentRoutingResult> routing = agentCoordinatorService.preview(new AgentConversationRequest(
				authenticatedUser.businessId(), null, detail.id(), detail.customer().id(), detail.customer().phone(),
				detail.customer().displayName(), lastInbound, OffsetDateTime.now(ZoneOffset.UTC), detail.locationId(),
				detail.locationName(), traceId, true, null));

		if (routing.isPresent() && routing.get().agentType() == AgentType.BOOKING) {
			AgentRoutingResult result = routing.get();
			assignConversationLocationIfDetected(authenticatedUser.businessId(), detail.id(), result, lastInbound,
					traceId);
			AiTraceLogger.info("AI_FINAL_RESPONSE", traceId, detail.id(), null, "ConversationService",
					"intent=" + result.primaryIntent() + " agent=" + result.agentType() + " confidence="
							+ result.confidence() + " response=" + result.responseToCustomer());
			String source = "AI_AGENT_COORDINATOR_" + result.agentType().name() + "_" + result.primaryIntent().name();
			if (isAiDryRunPreviewBody(result.responseToCustomer())) {
				source = source + "_BOOKING_PREVIEW";
			}
			return new ConversationAiReplyResponse(result.responseToCustomer(), result.confidence(), source);
		}

		Optional<ConversationAiReplyResponse> catalogResponse = buildCatalogAwareAiReply(authenticatedUser.businessId(),
				lastInbound, greetingName);
		if (catalogResponse.isPresent()) {
			return catalogResponse.get();
		}

		if (routing.isPresent()) {
			AgentRoutingResult result = routing.get();
			assignConversationLocationIfDetected(authenticatedUser.businessId(), detail.id(), result, lastInbound,
					traceId);
			return new ConversationAiReplyResponse(result.responseToCustomer(), result.confidence(),
					"AI_AGENT_COORDINATOR_" + result.agentType().name() + "_" + result.primaryIntent().name());
		}

		return new ConversationAiReplyResponse(
				"Hola" + greetingName + ", gracias por escribirnos. ¿Qué necesitas revisar hoy?", 0.58,
				"SAFE_FALLBACK");
	}

	private Optional<ConversationAiReplyResponse> buildCatalogAwareAiReply(UUID businessId, String lastInbound,
			String greetingName) {
		String normalized = normalizeForAiReply(lastInbound);

		if (isGreetingOnly(normalized)) {
			return Optional.of(new ConversationAiReplyResponse(
					"Hola" + greetingName + ", gracias por escribirnos. ¿Qué necesitas revisar hoy?", 0.74,
					"SAFE_GREETING"));
		}

		if (isBookingLookup(normalized)) {
			return Optional.of(new ConversationAiReplyResponse("Hola" + greetingName
					+ ", puedo ayudarte a revisar tus reservas, pero debo validarlo en agenda. ¿Qué fecha o mes quieres revisar?",
					0.86, "AI_AGENT_COORDINATOR_BOOKING_STATUS"));
		}

		if (containsAny(normalized, "manana", "mañana", "hoy", "esta semana")
				&& containsAny(normalized, "hora", "hrs", "horas", "am", "pm")) {
			return Optional.of(new ConversationAiReplyResponse(
					"Perfecto" + greetingName
							+ ". Para revisar disponibilidad en ese horario, ¿qué servicio quieres agendar?",
					0.78, "AI_AGENT_COORDINATOR_BOOKING_CONTINUATION"));
		}

		boolean asksPrice = containsAny(normalized, "precio", "valor", "cuanto cuesta", "cuanto sale", "cotizar",
				"cotizacion");
		boolean mentionsCatalogService = mentionsCatalogService(normalized);
		if (!asksPrice && !mentionsCatalogService) {
			return Optional.empty();
		}

		List<AestheticServiceResponse> matches = findServiceMatches(businessId, normalized);
		if (matches.isEmpty()) {
			return Optional.of(new ConversationAiReplyResponse(
					"Hola" + greetingName + ", con gusto reviso el catálogo. ¿Qué servicio específico quieres cotizar?",
					0.72, "CATALOG_SERVICE_NEEDS_DETAIL"));
		}

		if (shouldOfferServiceOptions(normalized, matches)) {
			String options = matches.stream().limit(4)
					.map(service -> service.name() + " " + formatCurrency(service.priceBase()))
					.collect(java.util.stream.Collectors.joining("; "));
			String label = containsAny(normalized, "axila", "axilas")
					? "depilación de axilas"
					: containsAny(normalized, "facial", "rostro", "cara") ? "depilación facial" : "depilación";
			return Optional.of(new ConversationAiReplyResponse("Hola" + greetingName + ", para " + label
					+ " tengo estas opciones en catálogo: " + options + ". ¿Cuál modalidad quieres revisar?", 0.88,
					"CATALOG_SERVICE_OPTIONS"));
		}

		AestheticServiceResponse service = matches.get(0);
		return Optional.of(new ConversationAiReplyResponse("Hola" + greetingName + ", el valor base de "
				+ service.name() + " es " + formatCurrency(service.priceBase()) + " y dura aproximadamente "
				+ service.durationMinutes()
				+ " minutos. Para reservar debo validar disponibilidad real en agenda. ¿Quieres que revise una fecha?",
				0.9, asksPrice ? "CATALOG_PRICE_SAFE" : "CATALOG_SERVICE_SAFE"));
	}

	private List<AestheticServiceResponse> findServiceMatches(UUID businessId, String normalized) {
		List<AestheticServiceResponse> services = aestheticCenterJdbcRepository
				.findServices(businessId, 0, 200, null, null, true).items();

		if (containsAny(normalized, "depilacion")) {
			return services.stream().filter(service -> {
				String name = normalizeForAiReply(service.name());
				if (!name.contains("depilacion")) {
					return false;
				}
				if (containsAny(normalized, "axila", "axilas")) {
					return name.contains("axila") || name.contains("axilas");
				}
				if (containsAny(normalized, "pierna", "piernas")) {
					return name.contains("pierna") || name.contains("piernas");
				}
				if (containsAny(normalized, "bikini", "rebaje")) {
					return name.contains("bikini") || name.contains("rebaje");
				}
				if (containsAny(normalized, "facial", "rostro", "cara")) {
					return name.contains("rostro") || name.contains("bozo") || name.contains("facial");
				}
				if (containsAny(normalized, "bozo")) {
					return name.contains("bozo");
				}
				return true;
			}).sorted(Comparator.comparing(AestheticServiceResponse::priceBase)).toList();
		}

		return services.stream().filter(service -> {
			String name = normalizeForAiReply(service.name());
			return normalized.contains(name) || meaningfulTokensMatch(normalized, name);
		}).sorted(Comparator.comparing(AestheticServiceResponse::name)).toList();
	}

	private boolean mentionsCatalogService(String normalized) {
		return containsAny(normalized, "depilacion", "limpieza facial", "manicure", "pedicure", "masaje", "peeling",
				"laser", "facial", "axilas", "piernas", "bikini", "bozo");
	}

	private boolean shouldOfferServiceOptions(String normalized, List<AestheticServiceResponse> matches) {
		return matches.size() > 1 && (isAmbiguousFacialDepilation(normalized)
				|| (containsAny(normalized, "depilacion") && containsAny(normalized, "axila", "axilas")
						&& !containsAny(normalized, "laser", "cera"))
				|| (containsAny(normalized, "depilacion") && !containsAny(normalized, "laser", "cera", "bozo", "rostro",
						"facial", "axila", "axilas", "pierna", "piernas", "bikini")));
	}

	private boolean isAmbiguousFacialDepilation(String normalized) {
		return containsAny(normalized, "depilacion") && containsAny(normalized, "facial", "rostro", "cara")
				&& !containsAny(normalized, "laser", "cera", "bozo", "perfilado");
	}

	private boolean isBookingLookup(String normalized) {
		return containsAny(normalized, "tengo agendado", "tengo agendada", "tengo reserva", "mi reserva",
				"mis reservas", "revisar agenda", "revisa la agenda", "revisala la agenda", "agenda de junio",
				"agenda de este mes", "estado reserva", "confirmar mi hora", "ver mi cita", "tengo cita");
	}

	private boolean meaningfulTokensMatch(String normalizedMessage, String normalizedName) {
		int matches = 0;
		for (String token : normalizedName.split("\\s+")) {
			if (token.length() > 4 && normalizedMessage.contains(token)) {
				matches++;
			}
		}
		return matches >= Math.min(2, Math.max(1, normalizedName.split("\\s+").length));
	}

	private boolean containsAny(String normalized, String... words) {
		for (String word : words) {
			if (normalized.contains(normalizeForAiReply(word))) {
				return true;
			}
		}
		return false;
	}

	private boolean isGreetingOnly(String normalized) {
		return normalized.equals("hola") || normalized.equals("buenas") || normalized.equals("buenos dias")
				|| normalized.equals("buenas tardes") || normalized.equals("buenas noches");
	}

	private String normalizeForAiReply(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replace('ñ', 'n').replaceAll("\\s+", " ").trim();
	}

	private String formatCurrency(BigDecimal value) {
		if (value == null) {
			return "$0";
		}
		return "$" + String.format(Locale.forLanguageTag("es-CL"), "%,.0f", value);
	}

	private boolean isNonActionableInbound(String value) {
		String normalized = normalizeForAiReply(value);
		return normalized.isBlank() || normalized.equals("mensaje recibido sin texto")
				|| normalized.equals("mensaje recibido sin texto.");
	}

	@Transactional
	public ConversationDetailResponse close(AuthenticatedUser authenticatedUser, UUID conversationId) {
		conversationJdbcRepository.updateConversationStatus(authenticatedUser.businessId(), conversationId, "CLOSED",
				OffsetDateTime.now(ZoneOffset.UTC));
		return conversationJdbcRepository.findConversationDetail(authenticatedUser.businessId(), conversationId);
	}

	@Transactional
	public ConversationDetailResponse reopen(AuthenticatedUser authenticatedUser, UUID conversationId) {
		conversationJdbcRepository.updateConversationStatus(authenticatedUser.businessId(), conversationId, "OPEN",
				OffsetDateTime.now(ZoneOffset.UTC));
		return conversationJdbcRepository.findConversationDetail(authenticatedUser.businessId(), conversationId);
	}

	private ConversationJdbcRepository.CustomerRecord resolveCustomer(AuthenticatedUser authenticatedUser,
			CreateConversationRequest request) {
		if (request.customerId() != null) {
			return conversationJdbcRepository.findCustomerById(authenticatedUser.businessId(), request.customerId());
		}

		String customerName = normalizeRequiredValue(request.customerName(), "customerName", 160);
		String customerPhone = normalizePhone(request.customerPhone());
		String email = normalizeOptionalValue(request.customerEmail(), 255);

		return conversationJdbcRepository.findCustomerByNormalizedPhone(authenticatedUser.businessId(), customerPhone)
				.orElseGet(() -> {
					NameParts nameParts = splitDisplayName(customerName);
					UUID customerId = conversationJdbcRepository.insertCustomer(authenticatedUser.businessId(),
							nameParts.firstName(), nameParts.lastName(), customerName, customerPhone, email);
					return conversationJdbcRepository.findCustomerById(authenticatedUser.businessId(), customerId);
				});
	}

	private ConversationMessageResponse sendMessageInternal(AuthenticatedUser authenticatedUser,
			ConversationJdbcRepository.ConversationContextRecord conversation, String body) {
		MessageChannelType channelType = MessageChannelType.valueOf(conversation.channelType());
		String recipientAddress = resolveRecipientAddress(authenticatedUser.businessId(), conversation, channelType);

		UUID messageId = conversationJdbcRepository.insertOutboundMessage(authenticatedUser.businessId(),
				conversation.id(), authenticatedUser.userId(), body, "PENDING", null, null, null);
		conversationJdbcRepository.insertMessageDeliveryLog(authenticatedUser.businessId(), messageId, "PENDING", null,
				OffsetDateTime.now(ZoneOffset.UTC));

		DispatchOutcome dispatchOutcome = dispatchOutbound(authenticatedUser.businessId(), conversation.id(), messageId,
				channelType, recipientAddress, body);
		applyDispatchOutcome(authenticatedUser.businessId(), conversation.id(), messageId, body, dispatchOutcome);
		logDispatchOutcome(conversation.id(), messageId, dispatchOutcome);

		return conversationJdbcRepository.findMessageById(authenticatedUser.businessId(), conversation.id(), messageId);
	}

	private DispatchOutcome dispatchOutbound(UUID businessId, UUID conversationId, UUID messageId,
			MessageChannelType channelType, String recipientAddress, String body) {
		String traceId = AiTraceLogger.newTraceId("SEND");
		AiTraceLogger.info("WHATSAPP_RESPONSE_SEND_STARTED", traceId, conversationId, messageId, "ConversationService",
				"channel=" + channelType + " phoneMasked=" + AiTraceLogger.maskPhone(recipientAddress)
						+ " messageLength=" + (body == null ? 0 : body.length()));
		try {
			ChannelDispatchResponse delivery = channelDispatchService
					.dispatch(new ChannelDispatchRequest(businessId, channelType, recipientAddress, body));
			return DispatchOutcome.delivered(traceId, delivery);
		} catch (MessagingChannelUnavailableException | UnsupportedMessagingChannelException
				| UnsupportedOperationException exception) {
			return DispatchOutcome.failed(traceId, exception.getClass().getSimpleName());
		}
	}

	private void applyDispatchOutcome(UUID businessId, UUID conversationId, UUID messageId, String body,
			DispatchOutcome dispatchOutcome) {
		OffsetDateTime occurredAt = dispatchOutcome.occurredAt();
		conversationJdbcRepository.updateOutboundMessageDelivery(businessId, conversationId, messageId,
				dispatchOutcome.status(), dispatchOutcome.externalMessageId(), occurredAt);
		if (!"FAILED".equals(dispatchOutcome.status())) {
			conversationJdbcRepository.updateConversationOutboundActivity(businessId, conversationId, body, occurredAt);
		}
		conversationJdbcRepository.insertMessageDeliveryLog(businessId, messageId, dispatchOutcome.status(),
				dispatchOutcome.externalMessageId(), occurredAt);
	}

	private void logDispatchOutcome(UUID conversationId, UUID messageId, DispatchOutcome dispatchOutcome) {
		String detail = "sent=" + dispatchOutcome.sent() + " status=" + dispatchOutcome.status()
				+ " externalMessageIdMasked=" + LogSanitizer.maskExternalId(dispatchOutcome.externalMessageId());
		if (dispatchOutcome.sent()) {
			AiTraceLogger.info("WHATSAPP_RESPONSE_SEND_RESULT", dispatchOutcome.traceId(), conversationId, messageId,
					"ConversationService", detail);
		} else {
			AiTraceLogger.warn("WHATSAPP_RESPONSE_SEND_RESULT", dispatchOutcome.traceId(), conversationId, messageId,
					"ConversationService", detail + " reason=" + dispatchOutcome.failureReason());
		}
	}

	private String resolveRecipientAddress(UUID businessId,
			ConversationJdbcRepository.ConversationContextRecord conversation, MessageChannelType channelType) {
		return channelType == MessageChannelType.WHATSAPP
				? conversationJdbcRepository.findLatestProviderChatId(businessId, conversation.id())
						.orElse(conversation.customerPhone())
				: conversation.customerPhone();
	}

	private String resolveMessageBody(AuthenticatedUser authenticatedUser,
			ConversationJdbcRepository.ConversationContextRecord conversation, SendConversationMessageRequest request) {
		if (request.body() != null && !request.body().isBlank()) {
			return request.body().trim();
		}

		if (request.templateId() == null) {
			throw validationError("body", "Debes indicar un mensaje o seleccionar una plantilla.");
		}

		ConversationJdbcRepository.TemplateRecord template = conversationJdbcRepository
				.findTemplateRecordById(authenticatedUser.businessId(), request.templateId());

		return templateVariableRenderer.render(template.body(),
				Map.of("customer_name", conversation.customerDisplayName(), "customer_phone",
						conversation.customerPhone(), "agent_name", authenticatedUser.displayName(), "business_name",
						authenticatedUser.businessName()));
	}

	private String resolveRealAiMessageBodyIfRequired(AuthenticatedUser authenticatedUser,
			ConversationJdbcRepository.ConversationContextRecord conversation, String body, String aiSource) {
		boolean dryRunPreviewBody = isAiDryRunPreviewBody(body);
		boolean dryRunPreviewSource = isAiDryRunPreviewSource(aiSource);
		boolean genericAiGreetingBody = isGenericAiGreetingBody(body);
		if (!dryRunPreviewBody && !dryRunPreviewSource && !genericAiGreetingBody) {
			return body;
		}

		ConversationDetailResponse detail = conversationJdbcRepository
				.findConversationDetail(authenticatedUser.businessId(), conversation.id());
		String lastInbound = detail.messages().stream().filter(message -> "INBOUND".equals(message.direction()))
				.map(ConversationMessageResponse::body).filter(messageBody -> !isNonActionableInbound(messageBody))
				.reduce((first, second) -> second)
				.orElse(detail.lastMessagePreview() == null || isNonActionableInbound(detail.lastMessagePreview())
						? ""
						: detail.lastMessagePreview());

		boolean genericGreetingAfterBookingInbound = genericAiGreetingBody && isBookingLikeInbound(lastInbound);
		if (!dryRunPreviewBody && !dryRunPreviewSource && !genericGreetingAfterBookingInbound) {
			return body;
		}

		String traceId = AiTraceLogger.newTraceId("SENDREAL");
		String reason = dryRunPreviewSource
				? "DRY_RUN_PREVIEW_SOURCE_DETECTED"
				: dryRunPreviewBody
						? "DRY_RUN_PREVIEW_BODY_DETECTED"
						: "GENERIC_AI_GREETING_AFTER_BOOKING_INBOUND_DETECTED";
		AiTraceLogger.info("AI_REAL_SEND_STARTED", traceId, conversation.id(), null, "ConversationService",
				"reason=" + reason + " aiSource=" + (aiSource == null ? "" : aiSource) + " bodyLength="
						+ (body == null ? 0 : body.length()) + " "
						+ LogSanitizer.messageSummary("lastInbound", lastInbound));

		AiTraceLogger.info("CONVERSATION_CONTEXT_LOADED", traceId, detail.id(), null, "ConversationService",
				"mode=REAL_SEND customerId=" + detail.customer().id() + " phoneMasked="
						+ AiTraceLogger.maskPhone(detail.customer().phone()) + " conversationLocation="
						+ detail.locationName() + " messages=" + detail.messages().size() + " "
						+ LogSanitizer.messageSummary("lastInbound", lastInbound));

		Optional<AgentRoutingResult> routing = agentCoordinatorService.route(new AgentConversationRequest(
				authenticatedUser.businessId(), null, detail.id(), detail.customer().id(), detail.customer().phone(),
				detail.customer().displayName(), lastInbound, OffsetDateTime.now(ZoneOffset.UTC), detail.locationId(),
				detail.locationName(), traceId, false, "REAL_CONVERSATION"));

		if (routing.isEmpty()) {
			AiTraceLogger.warn("AI_REAL_SEND_RESULT", traceId, detail.id(), null, "ConversationService",
					"generated=false reason=NO_AGENT_RESULT");
			return "No fue posible generar la respuesta real de agenda en este momento. Puedo revisar nuevamente o derivarte con una persona del equipo.";
		}

		AgentRoutingResult result = routing.get();
		assignConversationLocationIfDetected(authenticatedUser.businessId(), detail.id(), result, lastInbound, traceId);
		String response = result.responseToCustomer();
		AiTraceLogger.info("AI_REAL_SEND_RESULT", traceId, detail.id(), null, "ConversationService",
				"generated=true agent=" + result.agentType() + " intent=" + result.primaryIntent() + " confidence="
						+ result.confidence() + " containsLink="
						+ (response != null && response.contains("/reservas/confirmar/")) + " "
						+ LogSanitizer.responseSummary(response));
		if (response == null || response.isBlank()) {
			return "No fue posible generar la respuesta real de agenda en este momento. Puedo revisar nuevamente o derivarte con una persona del equipo.";
		}
		return response;
	}

	private void assignConversationLocationIfDetected(UUID businessId, UUID conversationId, AgentRoutingResult result,
			String lastInbound, String traceId) {
		if (result == null) {
			return;
		}
		String locationText = result.extractedData() == null ? null : result.extractedData().get("sede");
		String sourceText = locationText == null || locationText.isBlank() ? lastInbound : locationText;
		boolean assigned = conversationJdbcRepository.assignConversationLocationFromMessageIfBlank(businessId,
				conversationId, sourceText);
		if (assigned) {
			AiTraceLogger.info("CONVERSATION_LOCATION_ASSIGNED", traceId, conversationId, null, "ConversationService",
					"source=AI_ENTITY_OR_LAST_INBOUND locationText=" + (sourceText == null ? "" : sourceText));
		}
	}

	private boolean isAiDryRunPreviewSource(String aiSource) {
		if (aiSource == null || aiSource.isBlank()) {
			return false;
		}
		String normalized = aiSource.trim().toUpperCase(Locale.ROOT);
		return normalized.contains("BOOKING_PREVIEW") || normalized.contains("DRY_RUN")
				|| normalized.contains("AI_AGENT_COORDINATOR_BOOKING_BOOKING_REQUEST");
	}

	private boolean isGenericAiGreetingBody(String body) {
		String normalized = normalizeForAiReply(body);
		return normalized.equals("hola contacto gracias por escribirnos te ayudo de inmediato")
				|| normalized.equals("hola gracias por escribirnos te ayudo de inmediato")
				|| (normalized.contains("gracias por escribirnos") && normalized.contains("te ayudo de inmediato")
						&& normalized.length() <= 90);
	}

	private boolean isBookingLikeInbound(String body) {
		String normalized = normalizeForAiReply(body);
		if (normalized.isBlank()) {
			return false;
		}
		return normalized.contains("agendar") || normalized.contains("agenda") || normalized.contains("reservar")
				|| normalized.contains("reserva") || normalized.contains("cita") || normalized.contains("hora")
				|| normalized.contains("manana") || normalized.contains("viernes") || normalized.contains("sabado")
				|| normalized.contains("lunes") || normalized.contains("martes") || normalized.contains("miercoles")
				|| normalized.contains("jueves") || normalized.contains("domingo");
	}

	private boolean isAiDryRunPreviewBody(String body) {
		if (body == null || body.isBlank()) {
			return false;
		}
		String normalized = normalizeForAiReply(body);

		boolean legacyPreview = normalized.contains("esta es una vista previa")
				&& normalized.contains("no se creo una reserva temporal")
				&& (normalized.contains("al enviar la respuesta real")
						|| normalized.contains("al enviar la respuesta por whatsapp"));

		boolean formattedBookingPreview = normalized.contains("vista previa de reserva")
				&& normalized.contains("no creo una reserva temporal ni un enlace real")
				&& normalized.contains("al enviar la respuesta por whatsapp se creara la reserva temporal");

		boolean fallbackBookingPreview = normalized.contains("no creo una reserva temporal ni un enlace real")
				&& normalized.contains("se creara la reserva temporal y el enlace de confirmacion");

		return legacyPreview || formattedBookingPreview || fallbackBookingPreview;
	}

	private String normalizeSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		String normalized = search.trim();
		if (normalized.length() > 80) {
			throw validationError("search", "La busqueda no puede superar los 80 caracteres.");
		}
		return normalized;
	}

	private String normalizeConversationStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		String normalized = status.trim().toUpperCase();
		return switch (normalized) {
			case "OPEN", "PENDING", "CLOSED" -> normalized;
			default -> throw validationError("status", "El estado de conversacion no es valido.");
		};
	}

	private String normalizeRequiredValue(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw validationError(field, "Este campo es obligatorio.");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw validationError(field, "El valor supera el largo maximo permitido.");
		}
		return normalized;
	}

	private String normalizeOptionalValue(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException("El valor supera el largo maximo permitido.");
		}
		return normalized;
	}

	private String normalizeIdempotencyKey(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > 120) {
			throw validationError("idempotencyKey", "La clave de idempotencia no puede superar los 120 caracteres.");
		}
		return normalized;
	}

	private String normalizePhone(String phone) {
		String normalized = normalizeRequiredValue(phone, "customerPhone", 30).replace(" ", "");
		if (normalized.length() < 8) {
			throw validationError("customerPhone", "El telefono debe tener al menos 8 caracteres.");
		}
		return normalized;
	}

	private NameParts splitDisplayName(String displayName) {
		String[] parts = displayName.trim().split("\\s+", 2);
		if (parts.length == 1) {
			return new NameParts(parts[0], parts[0]);
		}
		return new NameParts(parts[0], parts[1]);
	}

	private ApiException validationError(String field, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos invalidos.",
				Map.of(field, message));
	}

	private record NameParts(String firstName, String lastName) {
	}

	private record PendingDispatch(ConversationMessageResponse existingMessage,
			ConversationJdbcRepository.ConversationContextRecord conversation, String body,
			MessageChannelType channelType, String recipientAddress, UUID messageId) {

		static PendingDispatch existing(ConversationMessageResponse existingMessage) {
			return new PendingDispatch(existingMessage, null, null, null, null, null);
		}

		static PendingDispatch pending(ConversationJdbcRepository.ConversationContextRecord conversation, String body,
				MessageChannelType channelType, String recipientAddress, UUID messageId) {
			return new PendingDispatch(null, conversation, body, channelType, recipientAddress, messageId);
		}
	}

	private record DispatchOutcome(String traceId, String status, String externalMessageId, OffsetDateTime occurredAt,
			boolean sent, String failureReason) {

		static DispatchOutcome delivered(String traceId, ChannelDispatchResponse delivery) {
			Instant acceptedAt = delivery.acceptedAt() == null ? Instant.now() : delivery.acceptedAt();
			return new DispatchOutcome(traceId, normalizeStatus(delivery.status()), delivery.externalMessageId(),
					OffsetDateTime.ofInstant(acceptedAt, ZoneOffset.UTC), true, null);
		}

		static DispatchOutcome failed(String traceId, String failureReason) {
			return new DispatchOutcome(traceId, "FAILED", null, OffsetDateTime.now(ZoneOffset.UTC), false,
					failureReason);
		}

		private static String normalizeStatus(String status) {
			if (status == null || status.isBlank()) {
				return "SENT";
			}
			return switch (status.trim().toUpperCase()) {
				case "PENDING", "QUEUED", "SENT", "DELIVERED", "READ", "FAILED", "SIMULATED", "DRY_RUN" ->
					status.trim().toUpperCase();
				case "PROVIDER_ACCEPTED", "ACCEPTED" -> "SENT";
				default -> "SENT";
			};
		}
	}
}
