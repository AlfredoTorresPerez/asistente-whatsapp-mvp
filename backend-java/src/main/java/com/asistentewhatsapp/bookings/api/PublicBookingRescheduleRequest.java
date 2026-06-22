package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingRescheduleRequest(
        @NotNull(message = "startsAt es obligatorio")
        OffsetDateTime startsAt,
        UUID professionalId,
        UUID roomId,
        String reason) {
}
