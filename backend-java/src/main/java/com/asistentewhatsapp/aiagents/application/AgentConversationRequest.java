package com.asistentewhatsapp.aiagents.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgentConversationRequest(
        UUID businessId,
        UUID channelAccountId,
        UUID conversationId,
        UUID customerId,
        String customerPhone,
        String customerDisplayName,
        String messageBody,
        OffsetDateTime occurredAt,
        UUID selectedLocationId,
        String selectedLocationName,
        String traceId,
        boolean dryRun) {

    public AgentConversationRequest(
            UUID businessId,
            UUID channelAccountId,
            UUID conversationId,
            UUID customerId,
            String customerPhone,
            String customerDisplayName,
            String messageBody,
            OffsetDateTime occurredAt) {
        this(
                businessId,
                channelAccountId,
                conversationId,
                customerId,
                customerPhone,
                customerDisplayName,
                messageBody,
                occurredAt,
                null,
                null,
                null,
                false);
    }
}

