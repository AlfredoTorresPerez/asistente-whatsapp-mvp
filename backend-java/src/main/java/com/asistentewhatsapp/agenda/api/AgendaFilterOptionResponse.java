package com.asistentewhatsapp.agenda.api;

import java.util.UUID;

public record AgendaFilterOptionResponse(
        UUID id,
        String name,
        String detail,
        UUID locationId,
        boolean active) {
}
