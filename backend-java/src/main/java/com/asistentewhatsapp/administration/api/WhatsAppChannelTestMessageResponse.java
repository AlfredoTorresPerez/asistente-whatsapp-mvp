package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsAppChannelTestMessageResponse(UUID conversationId, UUID messageId, String externalMessageId,
		String deliveryStatus, OffsetDateTime acceptedAt) {
}
