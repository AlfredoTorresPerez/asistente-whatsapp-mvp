package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingConfirmationLinkResponse(
        UUID id,
        UUID bookingId,
        String status,
        String confirmationUrl,
        OffsetDateTime expiresAt,
        OffsetDateTime sentAt) {
}
