package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingReminderResponse(
        UUID id,
        String reminderType,
        String channelType,
        OffsetDateTime scheduledAt,
        OffsetDateTime sentAt,
        String status,
        String failureReason,
        String templateKey) {
}
