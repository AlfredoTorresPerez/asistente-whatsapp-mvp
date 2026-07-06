package com.asistentewhatsapp.landing.api;

import java.math.BigDecimal;
import java.util.UUID;

public record LandingServiceItemResponse(
        UUID id,
        String name,
        String description,
        String categoryCode,
        String categoryName,
        Integer durationMinutes,
        BigDecimal priceBase,
        String professionalRequired,
        boolean requiresPriorEvaluation,
        boolean requiresInformedConsent) {
}
