package com.asistentewhatsapp.catalog.api;

import java.util.UUID;

public record CatalogCategoryResponse(UUID id, String code, String name, String description, boolean active) {
}
