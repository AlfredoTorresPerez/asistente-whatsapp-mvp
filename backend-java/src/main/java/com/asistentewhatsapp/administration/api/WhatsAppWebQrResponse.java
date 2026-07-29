package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;

public record WhatsAppWebQrResponse(String qrCode, String sessionStatus, OffsetDateTime expiresAt,
		OffsetDateTime lastQrAt) {
}
