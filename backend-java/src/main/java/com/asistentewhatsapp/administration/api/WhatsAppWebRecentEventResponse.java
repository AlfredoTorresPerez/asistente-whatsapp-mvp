package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;

public record WhatsAppWebRecentEventResponse(
        String deliveryId,
        String eventType,
        String processingStatus,
        OffsetDateTime receivedAt,
        OffsetDateTime processedAt) {
}
