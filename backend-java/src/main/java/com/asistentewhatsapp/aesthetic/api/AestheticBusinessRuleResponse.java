package com.asistentewhatsapp.aesthetic.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AestheticBusinessRuleResponse(
        UUID id,
        String code,
        String name,
        String ruleType,
        String description,
        int priority,
        boolean active,
        String rulePayload,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
