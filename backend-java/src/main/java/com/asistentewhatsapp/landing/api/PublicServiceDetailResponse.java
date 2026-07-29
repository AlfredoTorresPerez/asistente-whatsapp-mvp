package com.asistentewhatsapp.landing.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicServiceDetailResponse(UUID id, String code, String name, String description, String categoryCode,
		String categoryName, Integer durationMinutes, BigDecimal priceBase, String professionalRequired,
		String supplies, String contraindications, String availabilityRules, String bookingRules,
		String cancellationRules, String aftercareRecommendations, boolean requiresPriorEvaluation,
		boolean requiresInformedConsent, boolean active) {
}
