package com.asistentewhatsapp.conversations.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        String direction,
        String messageType,
        String body,
        String status,
        String externalMessageId,
        UUID sentByUserId,
        String sentByUserName,
        OffsetDateTime sentAt,
        OffsetDateTime receivedAt,
        OffsetDateTime failedAt,
        OffsetDateTime createdAt) {
}
