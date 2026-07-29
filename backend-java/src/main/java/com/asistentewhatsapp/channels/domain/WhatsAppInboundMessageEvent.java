package com.asistentewhatsapp.channels.domain;

import java.time.OffsetDateTime;
import java.util.Map;

public record WhatsAppInboundMessageEvent(String externalMessageId, String fromPhone, String toPhone, String body,
		WhatsAppMessageType messageType, OffsetDateTime timestamp, String contactName, String businessId,
		String channelAccountId, String phoneNumberId, String providerAccountId, String contextMessageId,
		Map<String, Object> metadata) {
}
