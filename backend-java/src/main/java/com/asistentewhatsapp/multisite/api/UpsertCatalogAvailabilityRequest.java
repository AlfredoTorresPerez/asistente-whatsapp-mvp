package com.asistentewhatsapp.multisite.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UpsertCatalogAvailabilityRequest(
        @NotNull UUID productServiceId,
        @NotNull UUID locationId,
        Boolean active,
        BigDecimal priceOverride,
        Integer durationOverrideMinutes,
        Boolean stockEnabled,
        Integer stockQuantity,
        Integer stockMinimum) {
}
