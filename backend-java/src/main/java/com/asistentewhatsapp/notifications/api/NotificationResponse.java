package com.asistentewhatsapp.notifications.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String status,
        String title,
        String body,
        String relatedEntityType,
        UUID relatedEntityId,
        OffsetDateTime createdAt,
        OffsetDateTime readAt) {
}
