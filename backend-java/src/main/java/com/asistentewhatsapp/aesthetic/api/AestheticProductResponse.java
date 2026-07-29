package com.asistentewhatsapp.aesthetic.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AestheticProductResponse(UUID id, String code, String name, String description, String categoryCode,
		String categoryName, BigDecimal price, int stock, int stockMinimum, String supplier, LocalDate expirationDate,
		String compatibleServices, String recommendationRules, String crossSellRules, String usageRestrictions,
		boolean lowStock, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
