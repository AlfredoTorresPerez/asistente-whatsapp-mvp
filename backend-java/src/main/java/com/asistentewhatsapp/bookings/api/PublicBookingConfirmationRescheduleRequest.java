package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingConfirmationRescheduleRequest(
        @NotNull OffsetDateTime startsAt,
        UUID professionalId,
        UUID roomId,
        String reason) {
}
