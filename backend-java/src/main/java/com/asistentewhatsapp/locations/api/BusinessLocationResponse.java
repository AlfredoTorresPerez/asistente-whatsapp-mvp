package com.asistentewhatsapp.locations.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessLocationResponse(
        UUID id,
        String code,
        String name,
        String address,
        String city,
        String commune,
        String phone,
        String whatsappNumber,
        String timezone,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
