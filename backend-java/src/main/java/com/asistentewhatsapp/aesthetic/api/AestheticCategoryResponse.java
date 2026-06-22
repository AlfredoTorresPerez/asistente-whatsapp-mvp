package com.asistentewhatsapp.aesthetic.api;

import java.util.UUID;

public record AestheticCategoryResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active) {
}
