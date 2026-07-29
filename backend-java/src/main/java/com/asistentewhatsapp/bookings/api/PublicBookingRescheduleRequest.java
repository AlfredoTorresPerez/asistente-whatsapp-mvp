package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingRescheduleRequest(@NotNull UUID bookingId, @NotNull UUID serviceId, @NotNull UUID locationId,
		UUID professionalId, UUID roomId, @NotNull LocalDate date, @NotNull OffsetDateTime startsAt, String reason) {
}
