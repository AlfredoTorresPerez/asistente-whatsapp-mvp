package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.List;

public record WhatsAppChannelStatusResponse(String provider, String connectionStatus, String phoneNumber,
		String phoneNumberId, String adapterMode, OffsetDateTime lastEventAt, boolean active, int recentEventCount,
		int recentErrorCount, List<WhatsAppChannelRecentEvent> recentEvents, String message) {
}
