package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaRescheduleRequest(
        @NotNull(message = "locationId es obligatorio") UUID locationId,
        @NotNull(message = "serviceId es obligatorio") UUID serviceId,
        UUID professionalId,
        UUID roomId,
        @NotNull(message = "startsAt es obligatorio") OffsetDateTime startsAt,
        @NotBlank(message = "reason es obligatorio") @Size(max = 500) String reason) {
}
