package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.domain.WhatsAppDeliveryStatus;
import com.asistentewhatsapp.channels.domain.WhatsAppDeliveryStatusEvent;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppDeliveryStatusService {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsAppDeliveryStatusService.class);

	private static final java.util.Map<WhatsAppDeliveryStatus, Integer> STATUS_ORDER = java.util.Map.of(
			WhatsAppDeliveryStatus.SENT, 0, WhatsAppDeliveryStatus.DELIVERED, 1, WhatsAppDeliveryStatus.READ, 2,
			WhatsAppDeliveryStatus.FAILED, 3, WhatsAppDeliveryStatus.DELETED, 4);

	private final WhatsAppWebChannelJdbcRepository repository;

	public WhatsAppDeliveryStatusService(WhatsAppWebChannelJdbcRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void processDeliveryStatus(WhatsAppDeliveryStatusEvent event, UUID businessId) {
		OffsetDateTime occurredAt = event.timestamp() != null ? event.timestamp() : OffsetDateTime.now();

		repository.findMessageIdByExternalMessageId(businessId, event.externalMessageId()).ifPresent(messageId -> {
			repository.updateMessageStatus(messageId, toMessageStatus(event.status()), event.externalMessageId(),
					occurredAt);
			repository.insertMessageDeliveryLog(businessId, messageId, event.status().name(), event.externalMessageId(),
					buildSafePayload(event), occurredAt);
		});
	}

	public boolean isMonotonic(String currentStatus, WhatsAppDeliveryStatus newStatus) {
		int currentOrder = STATUS_ORDER.getOrDefault(toDomainStatus(currentStatus), -1);
		int newOrder = STATUS_ORDER.getOrDefault(newStatus, -1);
		return newOrder >= currentOrder;
	}

	private String toMessageStatus(WhatsAppDeliveryStatus status) {
		return switch (status) {
			case SENT -> "SENT";
			case DELIVERED -> "DELIVERED";
			case READ -> "READ";
			case FAILED -> "FAILED";
			case DELETED -> "SENT";
		};
	}

	private WhatsAppDeliveryStatus toDomainStatus(String status) {
		try {
			return WhatsAppDeliveryStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			return WhatsAppDeliveryStatus.FAILED;
		}
	}

	private Object buildSafePayload(WhatsAppDeliveryStatusEvent event) {
		return java.util.Map.of("status", event.status().name(), "externalMessageId", event.externalMessageId(),
				"errorCode", event.errorCode() != null ? event.errorCode() : "", "errorTitle",
				event.errorTitle() != null ? event.errorTitle() : "");
	}
}
