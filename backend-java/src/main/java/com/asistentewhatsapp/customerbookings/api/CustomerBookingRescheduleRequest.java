package com.asistentewhatsapp.customerbookings.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerBookingRescheduleRequest(@NotNull UUID serviceId, @NotNull UUID locationId, UUID professionalId,
		UUID roomId, @NotNull LocalDate date, @NotNull OffsetDateTime startsAt, String reason) {
}
