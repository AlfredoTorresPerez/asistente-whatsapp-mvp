package com.asistentewhatsapp.landing.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePublicBookingRequest(
        @NotNull UUID locationId,
        @NotNull UUID serviceId,
        UUID professionalId,
        @NotNull OffsetDateTime startsAt,
        @NotBlank @Size(max = 160) String customerName,
        @NotBlank @Size(max = 30) String customerPhone,
        @Email @Size(max = 255) String customerEmail,
        @Size(max = 2000) String notes,
        @Size(max = 255) String idempotencyKey) {
}
