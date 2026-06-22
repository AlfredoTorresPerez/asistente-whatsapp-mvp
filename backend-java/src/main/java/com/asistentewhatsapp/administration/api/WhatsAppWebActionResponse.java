package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;

public record WhatsAppWebActionResponse(
        String sessionStatus,
        String phoneNumber,
        String qrCode,
        OffsetDateTime acceptedAt,
        boolean adapterReachable,
        String adapterMode) {
}
