package com.asistentewhatsapp.channels.domain;

import java.time.OffsetDateTime;

public record WhatsAppSessionAction(
        String sessionId,
        String connectionStatus,
        String phoneNumber,
        String qrCode,
        OffsetDateTime acceptedAt) {
}
