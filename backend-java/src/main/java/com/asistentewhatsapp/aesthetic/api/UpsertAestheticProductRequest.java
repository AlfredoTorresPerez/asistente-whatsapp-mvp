package com.asistentewhatsapp.aesthetic.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertAestheticProductRequest(
        @Size(max = 70, message = "code no debe superar 70 caracteres")
        String code,
        @NotBlank(message = "categoryCode es obligatorio")
        @Size(max = 60, message = "categoryCode no debe superar 60 caracteres")
        String categoryCode,
        @NotBlank(message = "name es obligatorio")
        @Size(max = 160, message = "name no debe superar 160 caracteres")
        String name,
        @NotBlank(message = "description es obligatoria")
        @Size(max = 4000, message = "description no debe superar 4000 caracteres")
        String description,
        @DecimalMin(value = "0.00", message = "price debe ser mayor o igual a cero")
        BigDecimal price,
        @Min(value = 0, message = "stock debe ser mayor o igual a cero")
        Integer stock,
        @Min(value = 0, message = "stockMinimum debe ser mayor o igual a cero")
        Integer stockMinimum,
        @Size(max = 160, message = "supplier no debe superar 160 caracteres")
        String supplier,
        LocalDate expirationDate,
        @Size(max = 4000, message = "compatibleServices no debe superar 4000 caracteres")
        String compatibleServices,
        @Size(max = 4000, message = "recommendationRules no debe superar 4000 caracteres")
        String recommendationRules,
        @Size(max = 4000, message = "crossSellRules no debe superar 4000 caracteres")
        String crossSellRules,
        @Size(max = 4000, message = "usageRestrictions no debe superar 4000 caracteres")
        String usageRestrictions,
        Boolean active) {
}
