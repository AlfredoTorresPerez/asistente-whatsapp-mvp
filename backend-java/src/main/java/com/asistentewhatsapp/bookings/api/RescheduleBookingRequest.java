package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RescheduleBookingRequest(
        @NotNull(message = "startsAt es obligatorio")
        OffsetDateTime startsAt,
        Integer durationMinutes,
        UUID locationId,
        @Size(max = 160, message = "location no puede superar 160 caracteres")
        String location,
        @Size(max = 2000, message = "notes no puede superar 2000 caracteres")
        String notes) {
}
