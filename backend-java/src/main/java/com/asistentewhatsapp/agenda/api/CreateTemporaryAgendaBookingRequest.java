package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTemporaryAgendaBookingRequest(
        @NotNull(message = "locationId es obligatorio") UUID locationId,
        @NotNull(message = "serviceId es obligatorio") UUID serviceId,
        UUID professionalId,
        UUID roomId,
        @NotNull(message = "startsAt es obligatorio") OffsetDateTime startsAt,
        @NotBlank(message = "customerName es obligatorio") @Size(max = 160) String customerName,
        @NotBlank(message = "customerPhone es obligatorio") @Size(max = 30) String customerPhone,
        @Email(message = "customerEmail debe tener formato valido") @Size(max = 255) String customerEmail,
        UUID customerId,
        UUID conversationId,
        UUID leadId,
        @Size(max = 2000) String notes,
        Integer expirationMinutes,
        Boolean generateConfirmationLink,
        Boolean sendWhatsApp,
        @Size(max = 255) String idempotencyKey) {
}
