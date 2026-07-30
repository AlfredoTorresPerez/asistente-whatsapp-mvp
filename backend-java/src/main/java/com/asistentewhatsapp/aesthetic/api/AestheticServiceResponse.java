package com.asistentewhatsapp.aesthetic.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AestheticServiceResponse(UUID id, String code, String name, String description, String categoryCode,
		String categoryName, Integer durationMinutes, BigDecimal priceBase, String professionalRequired,
		String supplies, String contraindications, String availabilityRules, String bookingRules,
		String cancellationRules, String aftercareRecommendations, boolean requiresPriorEvaluation,
		boolean requiresInformedConsent, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt,
		List<UUID> professionalIds, List<UUID> roomIds) {
}
