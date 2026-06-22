package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingPublicLinkSummaryResponse(
        UUID id,
        String type,
        String status,
        String url,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        OffsetDateTime createdAt) {
}
