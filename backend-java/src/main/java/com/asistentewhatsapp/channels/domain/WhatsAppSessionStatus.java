package com.asistentewhatsapp.channels.domain;

import java.time.OffsetDateTime;

public record WhatsAppSessionStatus(String sessionId, String connectionStatus, String phoneNumber, String qrCode,
		String adapterMode, OffsetDateTime lastEventAt) {
}
