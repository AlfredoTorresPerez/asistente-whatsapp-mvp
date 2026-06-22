package com.asistentewhatsapp.agenda.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaBlockResponse(
        UUID id,
        UUID locationId,
        UUID professionalId,
        UUID roomId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String reason,
        boolean active) {
}
