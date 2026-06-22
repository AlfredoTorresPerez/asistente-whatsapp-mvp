package com.asistentewhatsapp.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertCatalogProductRequest(
        @NotBlank @Size(max = 50) String categoryCode,
        @Size(max = 50) String sku,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Min(0) Integer stock,
        @Min(0) Integer stockMinimum,
        @Size(max = 160) String supplier,
        LocalDate expiresAt,
        Boolean active) {
}
