package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingFromLeadRequest(
        @NotBlank(message = "subject es obligatorio")
        @Size(max = 160, message = "subject no puede superar 160 caracteres")
        String subject,
        UUID assignedUserId,
        @Size(max = 30, message = "status no puede superar 30 caracteres")
        String status,
        @NotNull(message = "startsAt es obligatorio")
        OffsetDateTime startsAt,
        Integer durationMinutes,
        UUID locationId,
        @Size(max = 160, message = "location no puede superar 160 caracteres")
        String location,
        @Size(max = 2000, message = "notes no puede superar 2000 caracteres")
        String notes) {
}
