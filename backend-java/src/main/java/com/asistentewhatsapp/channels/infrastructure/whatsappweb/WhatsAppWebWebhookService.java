package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import com.asistentewhatsapp.aiagents.application.AgentCoordinatorService;
import com.asistentewhatsapp.aiagents.application.AiTraceLogger;
import com.asistentewhatsapp.aiagents.infrastructure.AiReplyOutboxJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.shared.api.StatusResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppWebWebhookService {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsAppWebWebhookService.class);

	private final WhatsAppWebClientProperties properties;
	private final WhatsAppWebChannelJdbcRepository repository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;
	private final AgentCoordinatorService agentCoordinatorService;
	private final AiReplyOutboxJdbcRepository aiReplyOutboxJdbcRepository;
	private final ObjectMapper objectMapper;

	public WhatsAppWebWebhookService(WhatsAppWebClientProperties properties,
			WhatsAppWebChannelJdbcRepository repository, AuditLogJdbcRepository auditLogJdbcRepository,
			AgentCoordinatorService agentCoordinatorService, AiReplyOutboxJdbcRepository aiReplyOutboxJdbcRepository,
			ObjectMapper objectMapper) {
		this.properties = properties;
		this.repository = repository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
		this.agentCoordinatorService = agentCoordinatorService;
		this.aiReplyOutboxJdbcRepository = aiReplyOutboxJdbcRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public StatusResponse handleWebhook(String rawBody, String timestampHeader, String signatureHeader,
			String deliveryIdHeader) {
		validateHeaders(rawBody, timestampHeader, signatureHeader, deliveryIdHeader);

		if (properties.logRawPayload()) {
			LOG.debug("Webhook payload received: {}", rawBody);
		}

		try {
			WhatsAppWebWebhookPayload request = objectMapper.readValue(rawBody, WhatsAppWebWebhookPayload.class);
			String resolvedDeliveryId = resolveDeliveryId(request.deliveryId(), deliveryIdHeader);
			if (resolvedDeliveryId == null || resolvedDeliveryId.isBlank()) {
				resolvedDeliveryId = UUID.randomUUID().toString();
				LOG.warn("Webhook deliveryId was blank, generated fallback: {}", resolvedDeliveryId);
			}
			OffsetDateTime receivedAt = OffsetDateTime.now(ZoneOffset.UTC);

			// Try to find channel account by sessionKey first
			WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount = repository
					.findChannelAccountBySessionKey(request.sessionKey()).orElseGet(() -> {
						// Fallback: try to find by phone number from payload
						String phoneNumber = extractPhoneFromPayload(request.payload());
						if (phoneNumber != null) {
							return repository.findChannelAccountByPhoneNumber(phoneNumber)
									.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
											"WHATSAPP_WEB_SESSION_NOT_FOUND",
											"No se encontro la sesion experimental informada por WhatsApp Web."));
						}
						throw new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_WEB_SESSION_NOT_FOUND",
								"No se encontro la sesion experimental informada por WhatsApp Web.");
					});

			boolean inserted = repository.insertChannelEventLog(channelAccount.businessId(), channelAccount.id(),
					resolvedDeliveryId, request.eventType(), rawBody, receivedAt);

			if (!inserted) {
				if (properties.logRawPayload()) {
					LOG.debug("Duplicate webhook event ignored: deliveryId={}", resolvedDeliveryId);
				}
				return new StatusResponse("ACCEPTED");
			}

			try {
				processEvent(channelAccount, request, resolvedDeliveryId);
				repository.markChannelEventProcessed(channelAccount.businessId(), resolvedDeliveryId, "PROCESSED",
						OffsetDateTime.now(ZoneOffset.UTC));
			} catch (RuntimeException exception) {
				repository.markChannelEventProcessed(channelAccount.businessId(), resolvedDeliveryId, "FAILED",
						OffsetDateTime.now(ZoneOffset.UTC));
				throw exception;
			}

			return new StatusResponse("ACCEPTED");
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			LOG.error("Failed to process WhatsApp Web webhook", exception);
			throw new ApiException(HttpStatus.BAD_REQUEST, "WHATSAPP_WEB_WEBHOOK_INVALID",
					"El evento experimental WhatsApp Web no pudo procesarse.");
		}
	}

	private String extractPhoneFromPayload(JsonNode payload) {
		String to = readText(payload, "to");
		String from = readText(payload, "from");
		String normalizedTo = normalizePhone(to);
		String normalizedFrom = normalizePhone(from);
		return normalizedTo != null && !normalizedTo.isBlank() ? normalizedTo : normalizedFrom;
	}

	private void processEvent(WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount,
			WhatsAppWebWebhookPayload request, String deliveryId) {
		OffsetDateTime occurredAt = request.occurredAt() == null
				? OffsetDateTime.now(ZoneOffset.UTC)
				: request.occurredAt();

		switch (request.eventType()) {
			case "SESSION_STATUS_CHANGED" -> handleSessionStatusChanged(channelAccount, request.payload(), occurredAt);
			case "QR_UPDATED" -> handleQrUpdated(channelAccount, request.payload(), occurredAt);
			case "MESSAGE_RECEIVED" -> handleMessageReceived(channelAccount, request.payload(), deliveryId, occurredAt);
			case "MESSAGE_SENT_EXTERNAL" ->
				handleMessageSentExternal(channelAccount, request.payload(), deliveryId, occurredAt);
			case "MESSAGE_ACK_UPDATED" ->
				handleMessageAckUpdated(channelAccount, request.payload(), deliveryId, occurredAt);
			default -> throw new ApiException(HttpStatus.BAD_REQUEST, "WHATSAPP_WEB_EVENT_UNSUPPORTED",
					"El evento experimental WhatsApp Web no esta soportado en esta fase.");
		}
	}

	private void handleSessionStatusChanged(WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount,
			JsonNode payload, OffsetDateTime occurredAt) {
		String status = readText(payload, "connectionStatus", "status", "sessionStatus");
		String qrCode = readText(payload, "qrCode");
		String phoneNumber = readText(payload, "phoneNumber");
		repository.updateChannelAccount(channelAccount.id(), normalizeSessionStatus(status), phoneNumber, qrCode,
				occurredAt);
	}

	private void handleQrUpdated(WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount, JsonNode payload,
			OffsetDateTime occurredAt) {
		String qrCode = readText(payload, "qrCode");
		repository.updateChannelAccount(channelAccount.id(), "QR_PENDING", channelAccount.phoneNumber(), qrCode,
				occurredAt);
		if (qrCode != null && !qrCode.isBlank() && properties.logRawPayload()) {
			LOG.debug("QR updated at {} for channel account {}", occurredAt, channelAccount.id());
		}
	}

	private void handleMessageReceived(WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount,
			JsonNode payload, String deliveryId, OffsetDateTime occurredAt) {
		String from = requireText(payload, "from");
		String body = requireText(payload, "body");
		String externalMessageId = readText(payload, "externalMessageId");
		String companyPhone = normalizePhone(readText(payload, "to"));

		// Use configurable test phone map
		String resolvedPhone = resolveTestPhone(normalizePhone(from));
		String normalizedPhone = resolvedPhone;
		String displayName = deriveDisplayName(normalizedPhone);
		String traceId = AiTraceLogger.newTraceId("WA");

		if (properties.logRawPayload()) {
			LOG.debug("Raw inbound message payload: {}", payload);
		}

		AiTraceLogger.info("WHATSAPP_MESSAGE_RECEIVED", traceId, null, null, "WhatsAppWebWebhookService",
				"deliveryId=" + deliveryId + " phoneMasked=" + AiTraceLogger.maskPhone(normalizedPhone)
						+ " externalMessageIdMasked=" + LogSanitizer.maskExternalId(externalMessageId) + " "
						+ LogSanitizer.messageSummary("message", body));

		WhatsAppWebChannelJdbcRepository.CustomerRecord customer = repository
				.findCustomerByPhone(channelAccount.businessId(), normalizedPhone)
				.orElseGet(() -> new WhatsAppWebChannelJdbcRepository.CustomerRecord(
						repository.insertCustomer(channelAccount.businessId(), normalizedPhone, displayName),
						displayName, normalizedPhone, normalizedPhone));

		UUID assignedUserId = repository
				.findLatestConversation(channelAccount.businessId(), channelAccount.id(), customer.id())
				.map(WhatsAppWebChannelJdbcRepository.ConversationRecord::assignedUserId)
				.or(() -> repository.findFirstActiveUserId(channelAccount.businessId())).orElse(null);

		WhatsAppWebChannelJdbcRepository.ConversationRecord conversation = repository
				.findLatestConversation(channelAccount.businessId(), channelAccount.id(), customer.id())
				.orElseGet(() -> new WhatsAppWebChannelJdbcRepository.ConversationRecord(
						repository.insertConversation(channelAccount.businessId(), channelAccount.id(), customer.id(),
								assignedUserId, customer.displayName(), normalizedPhone, occurredAt),
						assignedUserId, 0, null, null));

		if (externalMessageId != null && !externalMessageId.isBlank() && repository
				.findMessageIdByExternalMessageId(channelAccount.businessId(), externalMessageId).isPresent()) {
			AiTraceLogger.info("DUPLICATE_MESSAGE_SKIPPED", traceId, conversation.id(), null,
					"WhatsAppWebWebhookService", "externalMessageId=" + LogSanitizer.maskExternalId(externalMessageId)
							+ " phoneMasked=" + AiTraceLogger.maskPhone(normalizedPhone));
			return;
		}

		UUID inboundMessageId = repository.insertInboundMessage(channelAccount.businessId(), conversation.id(), body,
				externalMessageId, deliveryId, occurredAt);
		AiTraceLogger.info("CONVERSATION_CONTEXT_LOADED", traceId, conversation.id(), inboundMessageId,
				"WhatsAppWebWebhookService",
				"customerId=" + customer.id() + " contactLocation=" + conversation.locationName()
						+ " conversationLocation=" + conversation.locationName() + " unreadCount="
						+ conversation.unreadCount());
		repository.updateConversationInboundActivity(conversation.id(), body, occurredAt);
		WhatsAppWebChannelJdbcRepository.ConversationRecord effectiveConversation = repository
				.assignConversationLocationFromMessageIfBlank(channelAccount.businessId(), conversation.id(), body)
				.orElse(conversation);
		if (effectiveConversation.locationId() != null && conversation.locationId() == null) {
			AiTraceLogger.info("CONVERSATION_LOCATION_ASSIGNED", traceId, effectiveConversation.id(), inboundMessageId,
					"WhatsAppWebWebhookService",
					"source=INBOUND_MESSAGE locationId=" + effectiveConversation.locationId() + " locationName="
							+ effectiveConversation.locationName());
		}
		repository.updateChannelAccount(channelAccount.id(), "CONNECTED",
				companyPhone.isBlank() ? channelAccount.phoneNumber() : companyPhone, channelAccount.lastQrCode(),
				occurredAt);

		if (agentCoordinatorService.autoReplyEnabled(channelAccount.businessId())) {
			boolean enqueued = aiReplyOutboxJdbcRepository.enqueue(new AiReplyOutboxJdbcRepository.InboundAiReplyJob(
					channelAccount.businessId(), channelAccount.id(), effectiveConversation.id(), customer.id(),
					inboundMessageId, normalizedPhone, customer.displayName(), body, effectiveConversation.locationId(),
					effectiveConversation.locationName(), traceId), occurredAt, 5);
			AiTraceLogger.info("AI_OUTBOX_ENQUEUED", traceId, effectiveConversation.id(), inboundMessageId,
					"WhatsAppWebWebhookService", "enqueued=" + enqueued + " phoneMasked="
							+ AiTraceLogger.maskPhone(normalizedPhone) + " source=MESSAGE_RECEIVED");
		} else {
			AiTraceLogger.warn("AI_OUTBOX_SKIPPED", traceId, effectiveConversation.id(), inboundMessageId,
					"WhatsAppWebWebhookService", "reason=AI_AUTO_REPLY_DISABLED");
		}

		if (assignedUserId != null) {
			repository.insertNotification(channelAccount.businessId(), assignedUserId, "Nuevo mensaje recibido", body,
					effectiveConversation.id());
		}

		auditLogJdbcRepository.insert(channelAccount.businessId(), assignedUserId, "WHATSAPP_WEB_MESSAGE_RECEIVED",
				"CONVERSATION", effectiveConversation.id(),
				"Mensaje entrante recibido desde el adaptador WhatsApp Web experimental.",
				Map.of("deliveryId", deliveryId, "from", normalizedPhone, "externalMessageId",
						externalMessageId == null ? "" : externalMessageId),
				occurredAt);
	}

	private void handleMessageSentExternal(WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount,
			JsonNode payload, String deliveryId, OffsetDateTime occurredAt) {
		String to = requireText(payload, "to");
		String body = requireText(payload, "body");
		String externalMessageId = readText(payload, "externalMessageId");
		String providerEventId = readText(payload, "providerEventId");
		String companyPhone = normalizePhone(readText(payload, "from"));
		String normalizedPhone = normalizePhone(to);
		String displayName = deriveDisplayName(normalizedPhone);
		String traceId = AiTraceLogger.newTraceId("WA");

		if (properties.logRawPayload()) {
			LOG.debug("Raw outbound external message payload: {}", payload);
		}

		AiTraceLogger.info("WHATSAPP_MESSAGE_RECEIVED", traceId, null, null, "WhatsAppWebWebhookService",
				"deliveryId=" + deliveryId + " phoneMasked=" + AiTraceLogger.maskPhone(normalizedPhone)
						+ " externalMessageIdMasked=" + LogSanitizer.maskExternalId(externalMessageId) + " "
						+ LogSanitizer.messageSummary("message", body));

		WhatsAppWebChannelJdbcRepository.CustomerRecord customer = repository
				.findCustomerByPhone(channelAccount.businessId(), normalizedPhone)
				.orElseGet(() -> new WhatsAppWebChannelJdbcRepository.CustomerRecord(
						repository.insertCustomer(channelAccount.businessId(), normalizedPhone, displayName),
						displayName, normalizedPhone, normalizedPhone));

		UUID assignedUserId = repository
				.findLatestConversation(channelAccount.businessId(), channelAccount.id(), customer.id())
				.map(WhatsAppWebChannelJdbcRepository.ConversationRecord::assignedUserId)
				.or(() -> repository.findFirstActiveUserId(channelAccount.businessId())).orElse(null);

		WhatsAppWebChannelJdbcRepository.ConversationRecord conversation = repository
				.findLatestConversation(channelAccount.businessId(), channelAccount.id(), customer.id())
				.orElseGet(() -> new WhatsAppWebChannelJdbcRepository.ConversationRecord(
						repository.insertConversation(channelAccount.businessId(), channelAccount.id(), customer.id(),
								assignedUserId, customer.displayName(), normalizedPhone, occurredAt),
						assignedUserId, 0, null, null));

		if (externalMessageId != null && !externalMessageId.isBlank() && repository
				.findMessageIdByExternalMessageId(channelAccount.businessId(), externalMessageId).isPresent()) {
			repository.updateConversationOutboundActivity(conversation.id(), body, occurredAt);
			return;
		}

		UUID messageId = repository.insertExternalOutboundMessage(channelAccount.businessId(), conversation.id(), body,
				externalMessageId, providerEventId == null ? deliveryId : providerEventId, occurredAt);
		repository.updateConversationOutboundActivity(conversation.id(), body, occurredAt);
		repository.updateChannelAccount(channelAccount.id(), "CONNECTED",
				companyPhone.isBlank() ? channelAccount.phoneNumber() : companyPhone, channelAccount.lastQrCode(),
				occurredAt);
		repository.insertMessageDeliveryLog(channelAccount.businessId(), messageId, "SENT",
				providerEventId == null ? deliveryId : providerEventId, payload, occurredAt);

		auditLogJdbcRepository.insert(channelAccount.businessId(), assignedUserId, "WHATSAPP_WEB_EXTERNAL_MESSAGE_SENT",
				"CONVERSATION", conversation.id(),
				"Mensaje saliente manual detectado desde WhatsApp Web y registrado sin activar IA.",
				Map.of("deliveryId", deliveryId, "to", normalizedPhone, "externalMessageId",
						externalMessageId == null ? "" : externalMessageId),
				occurredAt);
	}

	private void handleMessageAckUpdated(WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount,
			JsonNode payload, String deliveryId, OffsetDateTime occurredAt) {
		String externalMessageId = readText(payload, "externalMessageId");
		String providerEventId = readText(payload, "providerEventId", "ackId");
		String status = normalizeDeliveryStatus(readText(payload, "status", "deliveryStatus"));

		repository.findMessageIdByExternalMessageId(channelAccount.businessId(), externalMessageId)
				.ifPresent(messageId -> {
					repository.updateMessageStatus(messageId, toMessageStatus(status), providerEventId, occurredAt);
					repository.insertMessageDeliveryLog(channelAccount.businessId(), messageId, status,
							providerEventId == null ? deliveryId : providerEventId, payload, occurredAt);
				});
	}

	private void validateHeaders(String rawBody, String timestampHeader, String signatureHeader,
			String deliveryIdHeader) {
		if (timestampHeader == null || signatureHeader == null || deliveryIdHeader == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "WHATSAPP_WEB_SIGNATURE_MISSING",
					"Faltan encabezados de seguridad del webhook WhatsApp Web.");
		}

		OffsetDateTime timestamp;
		try {
			timestamp = OffsetDateTime.parse(timestampHeader);
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "WHATSAPP_WEB_TIMESTAMP_INVALID",
					"El timestamp del webhook WhatsApp Web es invalido.");
		}

		long skewSeconds = Math.abs(Duration.between(timestamp, OffsetDateTime.now(ZoneOffset.UTC)).toSeconds());
		long tolerance = properties.webhookToleranceSeconds();
		if (skewSeconds > tolerance) {
			LOG.warn("Webhook timestamp skew detected: received={}, server={}, skew={}s, tolerance={}s", timestamp,
					OffsetDateTime.now(ZoneOffset.UTC), skewSeconds, tolerance);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "WHATSAPP_WEB_TIMESTAMP_EXPIRED",
					"El evento WhatsApp Web esta fuera de la ventana de tiempo permitida.");
		}

		String expectedSignature = sign(timestampHeader, rawBody);
		if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
				signatureHeader.getBytes(StandardCharsets.UTF_8))) {
			LOG.warn("Webhook HMAC validation failed: expected={}, received={}", expectedSignature, signatureHeader);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "WHATSAPP_WEB_SIGNATURE_INVALID",
					"La firma del webhook WhatsApp Web no es valida.");
		}
	}

	private String sign(String timestamp, String rawBody) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] signature = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
			return "sha256=" + java.util.HexFormat.of().formatHex(signature);
		} catch (Exception exception) {
			throw new IllegalStateException("No se pudo calcular la firma HMAC WhatsApp Web.", exception);
		}
	}

	private String resolveDeliveryId(String payloadDeliveryId, String headerDeliveryId) {
		if (payloadDeliveryId != null && !payloadDeliveryId.isBlank()) {
			return payloadDeliveryId;
		}
		return headerDeliveryId;
	}

	private String requireText(JsonNode payload, String fieldName) {
		String value = readText(payload, fieldName);
		if (value == null || value.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WHATSAPP_WEB_PAYLOAD_INVALID",
					"El payload WhatsApp Web no contiene los datos requeridos.");
		}
		return value;
	}

	private String readText(JsonNode payload, String... fieldNames) {
		if (payload == null || payload.isNull()) {
			return null;
		}
		for (String fieldName : fieldNames) {
			JsonNode node = payload.get(fieldName);
			if (node != null && !node.isNull()) {
				String text = node.asText();
				if (!text.isBlank()) {
					return text;
				}
			}
		}
		return null;
	}

	private String normalizeSessionStatus(String status) {
		if (status == null || status.isBlank()) {
			return "ERROR";
		}
		return switch (status) {
			case "CONNECTED", "DISCONNECTED", "QR_PENDING", "ERROR" -> status;
			case "QR_REQUIRED", "QR_REQUIRED_SCAN" -> "QR_PENDING";
			default -> "ERROR";
		};
	}

	private String normalizeDeliveryStatus(String status) {
		if (status == null || status.isBlank()) {
			return "FAILED";
		}
		return switch (status) {
			case "QUEUED", "PROVIDER_ACCEPTED", "SENT", "DELIVERED", "READ", "FAILED" -> status;
			default -> "FAILED";
		};
	}

	private String toMessageStatus(String deliveryStatus) {
		return switch (deliveryStatus) {
			case "PROVIDER_ACCEPTED" -> "SENT";
			default -> deliveryStatus;
		};
	}

	private String normalizePhone(String rawPhone) {
		return rawPhone == null ? "" : rawPhone.trim().replace(" ", "");
	}

	private String resolveTestPhone(String normalizedPhone) {
		if (properties.testPhoneMap() != null && !properties.testPhoneMap().isEmpty()) {
			return properties.testPhoneMap().getOrDefault(normalizedPhone, normalizedPhone);
		}
		// Default fallback for backward compatibility
		return "224145803620505".equals(normalizedPhone) ? "56950954580" : normalizedPhone;
	}

	private String deriveDisplayName(String normalizedPhone) {
		if (normalizedPhone == null || normalizedPhone.isBlank()) {
			return "Contacto WhatsApp Web";
		}
		String lastDigits = normalizedPhone.length() > 4
				? normalizedPhone.substring(normalizedPhone.length() - 4)
				: normalizedPhone;
		return "Contacto " + lastDigits;
	}

	private record WhatsAppWebWebhookPayload(String eventType, String deliveryId, OffsetDateTime occurredAt,
			String sessionKey, JsonNode payload) {
	}
}