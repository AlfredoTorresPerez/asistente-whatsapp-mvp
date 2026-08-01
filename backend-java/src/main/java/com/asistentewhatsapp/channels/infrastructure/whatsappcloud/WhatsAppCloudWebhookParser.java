package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.channels.application.WhatsAppDeliveryStatusService;
import com.asistentewhatsapp.channels.application.WhatsAppInboundMessageService;
import com.asistentewhatsapp.channels.domain.WhatsAppDeliveryStatus;
import com.asistentewhatsapp.channels.domain.WhatsAppDeliveryStatusEvent;
import com.asistentewhatsapp.channels.domain.WhatsAppInboundMessageEvent;
import com.asistentewhatsapp.channels.domain.WhatsAppMessageType;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.Change;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.Contact;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.Entry;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.Message;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.ErrorInfo;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.Status;
import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudWebhookPayload.Value;
import com.asistentewhatsapp.channels.infrastructure.WhatsAppChannelJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
public class WhatsAppCloudWebhookParser {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsAppCloudWebhookParser.class);

	private final ObjectMapper objectMapper;
	private final WhatsAppChannelJdbcRepository repository;
	private final WhatsAppInboundMessageService inboundMessageService;
	private final WhatsAppDeliveryStatusService deliveryStatusService;
	private final WhatsAppCloudApiMetrics metrics;

	public WhatsAppCloudWebhookParser(ObjectMapper objectMapper, WhatsAppChannelJdbcRepository repository,
			WhatsAppInboundMessageService inboundMessageService, WhatsAppDeliveryStatusService deliveryStatusService,
			WhatsAppCloudApiMetrics metrics) {
		this.objectMapper = objectMapper;
		this.repository = repository;
		this.inboundMessageService = inboundMessageService;
		this.deliveryStatusService = deliveryStatusService;
		this.metrics = metrics;
	}

	public void parseAndProcess(String rawBody) {
		WhatsAppCloudWebhookPayload payload;
		try {
			payload = objectMapper.readValue(rawBody, WhatsAppCloudWebhookPayload.class);
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WEBHOOK_INVALID_JSON",
					"El payload del webhook no es un JSON valido.");
		}

		if (payload == null || !"whatsapp_business_account".equals(payload.object())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WEBHOOK_INVALID_OBJECT",
					"El objeto del webhook no es whatsapp_business_account.");
		}

		if (payload.entry() == null || payload.entry().isEmpty()) {
			return;
		}

		for (Entry entry : payload.entry()) {
			if (entry.changes() == null)
				continue;
			for (Change change : entry.changes()) {
				if (change.value() == null)
					continue;
				Value value = change.value();

				String phoneNumberId = value.metadata() != null ? value.metadata().phoneNumberId() : null;

				WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount = resolveChannelAccount(
						phoneNumberId);

				if (channelAccount == null) {
					LOG.warn("No channel account found for phoneNumberId={}; skipping change", phoneNumberId);
					metrics.incrementWebhookRejected();
					continue;
				}

				UUID businessId = channelAccount.businessId();
				UUID channelAccountId = channelAccount.id();

				if (value.messages() != null) {
					for (Message message : value.messages()) {
						processMessageIdempotent(message, value, channelAccount, businessId, channelAccountId, rawBody);
					}
				}

				if (value.statuses() != null) {
					for (Status status : value.statuses()) {
						processStatusIdempotent(status, channelAccount, businessId, channelAccountId, rawBody);
					}
				}
			}
		}
	}

	private void processMessageIdempotent(Message message, Value value,
			WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount, UUID businessId, UUID channelAccountId,
			String rawBody) {
		String idempotencyKey = message.id() != null ? message.id() : computeHash(message);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		boolean inserted = repository.insertChannelEventLog(businessId, channelAccountId, idempotencyKey,
				"WHATSAPP_CLOUD_MESSAGE", rawBody, now);

		if (!inserted) {
			LOG.debug("Duplicate message ignored: id={}", idempotencyKey);
			return;
		}

		processMessage(message, value, channelAccount, businessId, channelAccountId, idempotencyKey);
	}

	private void processStatusIdempotent(Status status,
			WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount, UUID businessId, UUID channelAccountId,
			String rawBody) {
		String stableKey = status.id() + "-" + status.status() + "-"
				+ (status.timestamp() != null ? status.timestamp() : "0");
		String idempotencyKey = "STATUS_" + computeHash(stableKey);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		boolean inserted = repository.insertChannelEventLog(businessId, channelAccountId, idempotencyKey,
				"WHATSAPP_CLOUD_STATUS", rawBody, now);

		if (!inserted) {
			LOG.debug("Duplicate status ignored: key={}", idempotencyKey);
			return;
		}

		processStatus(status, channelAccount, businessId, channelAccountId);
	}

	private WhatsAppChannelJdbcRepository.ChannelAccountRecord resolveChannelAccount(String phoneNumberId) {
		if (phoneNumberId != null && !phoneNumberId.isBlank()) {
			Optional<WhatsAppChannelJdbcRepository.ChannelAccountRecord> byPhoneNumberId = repository
					.findChannelAccountByPhoneNumberId(phoneNumberId);
			if (byPhoneNumberId.isPresent()) {
				return byPhoneNumberId.get();
			}
		}

		return null;
	}

	private void processMessage(Message message, Value value,
			WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount, UUID businessId, UUID channelAccountId,
			String idempotencyKey) {
		metrics.incrementMessagesReceived();

		String from = message.from();
		String messageId = message.id();
		String timestampStr = message.timestamp();
		OffsetDateTime timestamp = parseTimestamp(timestampStr);

		String contactName = null;
		if (value.contacts() != null && !value.contacts().isEmpty()) {
			Contact first = value.contacts().getFirst();
			if (first.profile() != null) {
				contactName = first.profile().name();
			}
		}

		WhatsAppInboundMessageEvent event = parseMessage(message, from, messageId, timestamp, contactName,
				channelAccount);

		inboundMessageService.processInboundMessage(event, businessId, channelAccountId, idempotencyKey);
	}

	private WhatsAppInboundMessageEvent parseMessage(Message message, String from, String messageId,
			OffsetDateTime timestamp, String contactName,
			WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount) {
		String body = null;
		WhatsAppMessageType messageType = WhatsAppMessageType.UNKNOWN;
		String contextMessageId = null;
		Map<String, Object> metadata = new HashMap<>();

		if (message.context() != null) {
			contextMessageId = message.context().id();
		}

		String type = message.type();
		if (type == null) {
			type = "unknown";
		}

		switch (type) {
			case "text" -> {
				messageType = WhatsAppMessageType.TEXT;
				body = message.text() != null ? message.text().body() : null;
				metrics.incrementMessagesByType("text");
			}
			case "interactive" -> {
				if (message.interactive() != null) {
					if (message.interactive().buttonReply() != null) {
						messageType = WhatsAppMessageType.INTERACTIVE_BUTTON_REPLY;
						body = message.interactive().buttonReply().title();
						metadata.put("buttonId", message.interactive().buttonReply().id());
						metrics.incrementMessagesByType("interactive_button_reply");
					} else if (message.interactive().listReply() != null) {
						messageType = WhatsAppMessageType.INTERACTIVE_LIST_REPLY;
						body = message.interactive().listReply().title();
						metadata.put("listItemId", message.interactive().listReply().id());
						metrics.incrementMessagesByType("interactive_list_reply");
					}
				}
			}
			case "button" -> {
				messageType = WhatsAppMessageType.BUTTON;
				body = message.button() != null ? message.button().text() : null;
				metrics.incrementMessagesByType("button");
			}
			case "image" -> {
				messageType = WhatsAppMessageType.IMAGE;
				if (message.image() != null) {
					metadata.put("mediaId", message.image().id());
					metadata.put("mimeType", message.image().mimeType());
					metadata.put("caption", message.image().caption() != null ? message.image().caption() : "");
				}
				body = message.image() != null ? message.image().caption() : null;
				metrics.incrementMessagesByType("image");
			}
			case "document" -> {
				messageType = WhatsAppMessageType.DOCUMENT;
				if (message.document() != null) {
					metadata.put("mediaId", message.document().id());
					metadata.put("mimeType", message.document().mimeType());
					metadata.put("caption", message.document().caption() != null ? message.document().caption() : "");
					metadata.put("filename",
							message.document().filename() != null ? message.document().filename() : "");
				}
				body = message.document() != null ? message.document().caption() : null;
				metrics.incrementMessagesByType("document");
			}
			case "audio" -> {
				messageType = WhatsAppMessageType.AUDIO;
				if (message.audio() != null) {
					metadata.put("mediaId", message.audio().id());
					metadata.put("mimeType", message.audio().mimeType());
					metadata.put("voice", message.audio().voice());
				}
				metrics.incrementMessagesByType("audio");
			}
			case "video" -> {
				messageType = WhatsAppMessageType.VIDEO;
				if (message.video() != null) {
					metadata.put("mediaId", message.video().id());
					metadata.put("mimeType", message.video().mimeType());
					metadata.put("caption", message.video().caption() != null ? message.video().caption() : "");
				}
				body = message.video() != null ? message.video().caption() : null;
				metrics.incrementMessagesByType("video");
			}
			case "sticker" -> {
				messageType = WhatsAppMessageType.STICKER;
				if (message.sticker() != null) {
					metadata.put("mediaId", message.sticker().id());
					metadata.put("mimeType", message.sticker().mimeType());
					metadata.put("animated", message.sticker().animated());
				}
				metrics.incrementMessagesByType("sticker");
			}
			case "location" -> {
				messageType = WhatsAppMessageType.LOCATION;
				if (message.location() != null) {
					metadata.put("latitude", message.location().latitude());
					metadata.put("longitude", message.location().longitude());
					metadata.put("locationName", message.location().name() != null ? message.location().name() : "");
					metadata.put("address", message.location().address() != null ? message.location().address() : "");
				}
				metrics.incrementMessagesByType("location");
			}
			case "contacts" -> {
				messageType = WhatsAppMessageType.CONTACTS;
				if (message.contacts() != null && message.contacts().contacts() != null
						&& !message.contacts().contacts().isEmpty()) {
					var firstContact = message.contacts().contacts().getFirst();
					if (firstContact.names() != null && !firstContact.names().isEmpty()) {
						var name = firstContact.names().getFirst();
						metadata.put("contactName", name.formattedName() != null ? name.formattedName() : "");
					}
				}
				metrics.incrementMessagesByType("contacts");
			}
			default -> {
				metrics.incrementMessagesByType("unknown");
				LOG.debug("Unsupported message type: {}", type);
			}
		}

		return new WhatsAppInboundMessageEvent(messageId, from, null, body != null ? body : "", messageType, timestamp,
				contactName, null, null, null, null, contextMessageId, metadata.isEmpty() ? null : metadata);
	}

	private void processStatus(Status status, WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount,
			UUID businessId, UUID channelAccountId) {
		String statusStr = status.status();
		String externalMessageId = status.id();
		String timestampStr = status.timestamp();
		OffsetDateTime timestamp = parseTimestamp(timestampStr);

		WhatsAppDeliveryStatus deliveryStatus = switch (statusStr) {
			case "sent" -> WhatsAppDeliveryStatus.SENT;
			case "delivered" -> WhatsAppDeliveryStatus.DELIVERED;
			case "read" -> WhatsAppDeliveryStatus.READ;
			case "failed" -> WhatsAppDeliveryStatus.FAILED;
			case "deleted" -> WhatsAppDeliveryStatus.DELETED;
			default -> {
				LOG.debug("Unsupported status type: {}", statusStr);
				yield null;
			}
		};

		if (deliveryStatus == null) {
			return;
		}

		metrics.incrementMessageStatus(deliveryStatus.name().toLowerCase());

		String errorCode = null;
		String errorTitle = null;
		String errorDetails = null;
		if (status.errors() != null && !status.errors().isEmpty()) {
			ErrorInfo firstError = status.errors().getFirst();
			errorCode = String.valueOf(firstError.code());
			errorTitle = firstError.title();
			errorDetails = firstError.message();
		}

		WhatsAppDeliveryStatusEvent event = new WhatsAppDeliveryStatusEvent(externalMessageId, businessId.toString(),
				channelAccountId.toString(), deliveryStatus, timestamp, errorCode, errorTitle, errorDetails);

		deliveryStatusService.processDeliveryStatus(event, businessId);
	}

	private OffsetDateTime parseTimestamp(String timestampStr) {
		if (timestampStr == null || timestampStr.isBlank()) {
			return OffsetDateTime.now(ZoneOffset.UTC);
		}
		try {
			long epoch = Long.parseLong(timestampStr);
			return OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
		} catch (NumberFormatException exception) {
			return OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	private static String computeHash(Object obj) {
		try {
			String input = obj != null ? obj.toString() : "null";
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.substring(0, 32);
		} catch (Exception exception) {
			return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
		}
	}

	private static String computeHash(String input) {
		return computeHash((Object) input);
	}
}
