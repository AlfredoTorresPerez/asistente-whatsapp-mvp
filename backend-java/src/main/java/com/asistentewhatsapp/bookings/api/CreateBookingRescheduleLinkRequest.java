package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingRescheduleLinkRequest(
        @NotNull UUID locationId,
        UUID serviceId,
        UUID professionalId,
        UUID roomId,
        @NotNull OffsetDateTime startsAt,
        String reason,
        Integer expirationMinutes,
        Boolean sendWhatsApp,
        Boolean sendEmail) {
}
