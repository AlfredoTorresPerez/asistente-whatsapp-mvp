package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.List;

public record WhatsAppWebStatusResponse(String sessionStatus, String phoneNumber, String qrCode,
		OffsetDateTime lastEventAt, boolean adapterReachable, String adapterMode, String warningMessage,
		List<WhatsAppWebRecentEventResponse> recentEvents) {
}
