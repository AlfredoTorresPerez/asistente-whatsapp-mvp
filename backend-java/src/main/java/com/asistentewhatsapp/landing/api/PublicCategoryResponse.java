package com.asistentewhatsapp.landing.api;

import java.util.UUID;

public record PublicCategoryResponse(UUID id, String code, String name, String description, boolean active,
		int serviceCount, Integer displayOrder) {
}
