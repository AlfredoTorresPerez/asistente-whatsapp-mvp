package com.asistentewhatsapp.businessai.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record UpsertBusinessAiSettingsRequest(boolean active,

		@NotBlank @Size(max = 20) @Pattern(regexp = "suggest|auto") String mode,

		@NotBlank @Size(max = 20) @Pattern(regexp = "Cercano|Profesional|Comercial") String tone,

		@NotBlank @Size(max = 10) String language,

		@DecimalMin("0.01") @DecimalMax("1.00") BigDecimal escalationThreshold,

		boolean allowPrices, boolean allowBooking, boolean allowPromotions, boolean requireAvailabilityCheck,

		List<String> allowedTopics, List<String> blockedTopics) {
}
