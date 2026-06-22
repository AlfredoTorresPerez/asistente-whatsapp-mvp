package com.asistentewhatsapp.multisite.api;

import java.math.BigDecimal;
import java.util.UUID;

public record MultisiteCatalogAvailabilityResponse(
        UUID itemId,
        String type,
        String name,
        String sku,
        BigDecimal basePrice,
        UUID locationId,
        String locationName,
        boolean available,
        BigDecimal priceOverride,
        Integer durationOverrideMinutes,
        Boolean stockEnabled,
        Integer stockQuantity,
        Integer stockMinimum) {
}
