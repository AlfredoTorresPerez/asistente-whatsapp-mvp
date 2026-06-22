package com.asistentewhatsapp.catalog.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CatalogProductResponse(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stock,
        int stockMinimum,
        boolean lowStock,
        String supplier,
        LocalDate expiresAt,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
