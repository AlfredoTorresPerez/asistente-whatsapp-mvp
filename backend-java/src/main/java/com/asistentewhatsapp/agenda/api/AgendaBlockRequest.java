package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaBlockRequest(UUID locationId, UUID professionalId, UUID roomId,
		@NotNull(message = "startsAt es obligatorio") OffsetDateTime startsAt,
		@NotNull(message = "endsAt es obligatorio") OffsetDateTime endsAt,
		@NotBlank(message = "reason es obligatorio") @Size(max = 240) String reason) {
}
