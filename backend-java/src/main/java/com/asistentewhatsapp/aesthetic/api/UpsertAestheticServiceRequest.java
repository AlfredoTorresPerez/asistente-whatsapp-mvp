package com.asistentewhatsapp.aesthetic.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpsertAestheticServiceRequest(@Size(max = 70, message = "code no debe superar 70 caracteres") String code,
		@Size(max = 60, message = "categoryCode no debe superar 60 caracteres") String categoryCode,
		@Size(max = 160, message = "name no debe superar 160 caracteres") String name,
		@Size(max = 4000, message = "description no debe superar 4000 caracteres") String description,
		@Min(value = 10, message = "durationMinutes debe ser mayor o igual a 10") @Max(value = 480, message = "durationMinutes no debe superar 480") Integer durationMinutes,
		@DecimalMin(value = "0.00", message = "priceBase debe ser mayor o igual a cero") BigDecimal priceBase,
		@Size(max = 160, message = "professionalRequired no debe superar 160 caracteres") String professionalRequired,
		@Size(max = 4000, message = "supplies no debe superar 4000 caracteres") String supplies,
		@Size(max = 4000, message = "contraindications no debe superar 4000 caracteres") String contraindications,
		@Size(max = 4000, message = "availabilityRules no debe superar 4000 caracteres") String availabilityRules,
		@Size(max = 4000, message = "bookingRules no debe superar 4000 caracteres") String bookingRules,
		@Size(max = 4000, message = "cancellationRules no debe superar 4000 caracteres") String cancellationRules,
		@Size(max = 4000, message = "aftercareRecommendations no debe superar 4000 caracteres") String aftercareRecommendations,
		Boolean requiresPriorEvaluation, Boolean requiresInformedConsent, Boolean active, List<UUID> professionalIds,
		List<UUID> roomIds) {
}
