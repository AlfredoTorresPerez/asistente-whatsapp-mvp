package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.aiagents.application.AgentCoordinatorService;
import com.asistentewhatsapp.aiagents.application.AiTraceLogger;
import com.asistentewhatsapp.aiagents.infrastructure.AiReplyOutboxJdbcRepository;
import com.asistentewhatsapp.channels.domain.WhatsAppInboundMessageEvent;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppInboundMessageService {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsAppInboundMessageService.class);

	private final WhatsAppWebChannelJdbcRepository repository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;
	private final AgentCoordinatorService agentCoordinatorService;
	private final AiReplyOutboxJdbcRepository aiReplyOutboxJdbcRepository;

	public WhatsAppInboundMessageService(WhatsAppWebChannelJdbcRepository repository,
			AuditLogJdbcRepository auditLogJdbcRepository, AgentCoordinatorService agentCoordinatorService,
			AiReplyOutboxJdbcRepository aiReplyOutboxJdbcRepository) {
		this.repository = repository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
		this.agentCoordinatorService = agentCoordinatorService;
		this.aiReplyOutboxJdbcRepository = aiReplyOutboxJdbcRepository;
	}

	@Transactional
	public void processInboundMessage(WhatsAppInboundMessageEvent event, UUID businessId, UUID channelAccountId,
			String deliveryId) {
		String traceId = AiTraceLogger.newTraceId("WA");
		String normalizedPhone = normalizePhone(event.fromPhone());
		String displayName = deriveDisplayName(normalizedPhone, event.contactName());
		OffsetDateTime occurredAt = event.timestamp() != null ? event.timestamp() : OffsetDateTime.now(ZoneOffset.UTC);

		String bodyText = extractBodyText(event);

		AiTraceLogger.info("WHATSAPP_MESSAGE_RECEIVED", traceId, null, null, "WhatsAppInboundMessageService",
				"deliveryId=" + deliveryId + " phoneMasked=" + AiTraceLogger.maskPhone(normalizedPhone)
						+ " externalMessageIdMasked=" + LogSanitizer.maskExternalId(event.externalMessageId()) + " "
						+ LogSanitizer.messageSummary("message", bodyText));

		WhatsAppWebChannelJdbcRepository.CustomerRecord customer = repository
				.findCustomerByPhone(businessId, normalizedPhone)
				.orElseGet(() -> new WhatsAppWebChannelJdbcRepository.CustomerRecord(
						repository.insertCustomer(businessId, normalizedPhone, displayName), displayName,
						normalizedPhone, normalizedPhone));

		UUID assignedUserId = repository.findLatestConversation(businessId, channelAccountId, customer.id())
				.map(WhatsAppWebChannelJdbcRepository.ConversationRecord::assignedUserId)
				.or(() -> repository.findFirstActiveUserId(businessId)).orElse(null);

		WhatsAppWebChannelJdbcRepository.ConversationRecord conversation = repository
				.findLatestConversation(businessId, channelAccountId, customer.id()).orElseGet(
						() -> new WhatsAppWebChannelJdbcRepository.ConversationRecord(
								repository.insertConversation(businessId, channelAccountId, customer.id(),
										assignedUserId, customer.displayName(), normalizedPhone, occurredAt),
								assignedUserId, 0, null, null));

		if (event.externalMessageId() != null && !event.externalMessageId().isBlank()
				&& repository.findMessageIdByExternalMessageId(businessId, event.externalMessageId()).isPresent()) {
			AiTraceLogger.info("DUPLICATE_MESSAGE_SKIPPED", traceId, conversation.id(), null,
					"WhatsAppInboundMessageService",
					"externalMessageId=" + LogSanitizer.maskExternalId(event.externalMessageId()) + " phoneMasked="
							+ AiTraceLogger.maskPhone(normalizedPhone));
			return;
		}

		UUID inboundMessageId = repository.insertInboundMessage(businessId, conversation.id(), bodyText,
				event.externalMessageId(), deliveryId, occurredAt);

		repository.updateConversationInboundActivity(conversation.id(), bodyText, occurredAt);

		if (agentCoordinatorService.autoReplyEnabled(businessId)) {
			boolean enqueued = aiReplyOutboxJdbcRepository
					.enqueue(new AiReplyOutboxJdbcRepository.InboundAiReplyJob(businessId, channelAccountId,
							conversation.id(), customer.id(), inboundMessageId, normalizedPhone, customer.displayName(),
							bodyText, conversation.locationId(), conversation.locationName(), traceId), occurredAt, 5);
			AiTraceLogger.info("AI_OUTBOX_ENQUEUED", traceId, conversation.id(), inboundMessageId,
					"WhatsAppInboundMessageService", "enqueued=" + enqueued + " phoneMasked="
							+ AiTraceLogger.maskPhone(normalizedPhone) + " source=WHATSAPP_CLOUD");
		} else {
			AiTraceLogger.warn("AI_OUTBOX_SKIPPED", traceId, conversation.id(), inboundMessageId,
					"WhatsAppInboundMessageService", "reason=AI_AUTO_REPLY_DISABLED");
		}

		if (assignedUserId != null) {
			repository.insertNotification(businessId, assignedUserId, "Nuevo mensaje recibido", bodyText,
					conversation.id());
		}

		auditLogJdbcRepository.insert(businessId, assignedUserId, "WHATSAPP_CLOUD_MESSAGE_RECEIVED", "CONVERSATION",
				conversation.id(), "Mensaje entrante recibido desde WhatsApp Cloud API.",
				Map.of("deliveryId", deliveryId, "from", normalizedPhone, "externalMessageId",
						event.externalMessageId() == null ? "" : event.externalMessageId(), "messageType",
						event.messageType().name()),
				occurredAt);
	}

	private String extractBodyText(WhatsAppInboundMessageEvent event) {
		if (event.body() != null && !event.body().isBlank()) {
			return event.body();
		}
		return switch (event.messageType()) {
			case INTERACTIVE_BUTTON_REPLY -> "Respuesta de boton interactivo";
			case INTERACTIVE_LIST_REPLY -> "Respuesta de lista interactiva";
			case BUTTON -> "Respuesta de boton";
			case IMAGE -> "Mensaje de imagen recibido";
			case DOCUMENT -> "Documento recibido";
			case AUDIO -> "Nota de voz recibida";
			case VIDEO -> "Video recibido";
			case STICKER -> "Sticker recibido";
			case LOCATION -> "Ubicacion compartida";
			case CONTACTS -> "Contacto compartido";
			case UNKNOWN -> "Tipo de mensaje no soportado";
			default -> "Mensaje recibido sin texto";
		};
	}

	private String deriveDisplayName(String normalizedPhone, String contactName) {
		if (contactName != null && !contactName.isBlank()) {
			return contactName;
		}
		if (normalizedPhone == null || normalizedPhone.isBlank()) {
			return "Contacto WhatsApp";
		}
		String lastDigits = normalizedPhone.length() > 4
				? normalizedPhone.substring(normalizedPhone.length() - 4)
				: normalizedPhone;
		return "Contacto " + lastDigits;
	}

	private String normalizePhone(String rawPhone) {
		return rawPhone == null ? "" : rawPhone.replaceAll("\\D", "");
	}
}
