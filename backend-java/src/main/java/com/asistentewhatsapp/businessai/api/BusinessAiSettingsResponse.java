package com.asistentewhatsapp.businessai.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BusinessAiSettingsResponse(UUID id, UUID businessId, boolean active, String mode, String tone,
		String language, BigDecimal escalationThreshold, boolean allowPrices, boolean allowBooking,
		boolean allowPromotions, boolean requireAvailabilityCheck, List<String> allowedTopics,
		List<String> blockedTopics, Integer activePromptVersion, UUID updatedBy, OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
