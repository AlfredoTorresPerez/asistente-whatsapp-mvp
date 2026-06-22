package com.asistentewhatsapp.dashboard.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DashboardActivityResponse(
        String entityType,
        UUID entityId,
        String title,
        String body,
        String status,
        OffsetDateTime occurredAt) {
}
