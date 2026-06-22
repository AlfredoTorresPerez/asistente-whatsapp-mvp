package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsAppWebTestMessageResponse(
        UUID conversationId,
        UUID messageId,
        String externalMessageId,
        String deliveryStatus,
        OffsetDateTime acceptedAt) {
}
