package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;

public record WhatsAppChannelActionResponse(String connectionStatus, String phoneNumber, OffsetDateTime acceptedAt,
		String provider) {
}
