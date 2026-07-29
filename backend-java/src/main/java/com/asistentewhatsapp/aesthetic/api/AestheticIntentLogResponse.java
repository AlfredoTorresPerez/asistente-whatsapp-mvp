package com.asistentewhatsapp.aesthetic.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AestheticIntentLogResponse(UUID id, String sourceMessage, String intent, BigDecimal confidence,
		String entities, boolean requiresDatabaseLookup, boolean requiresHumanHandoff, String handoffReason,
		String suggestedResponse, String modelName, OffsetDateTime createdAt) {
}
