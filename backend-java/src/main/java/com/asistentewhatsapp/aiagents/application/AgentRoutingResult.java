package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentRoutingResult(UUID businessId, UUID conversationId, UUID customerId, AgentIntent primaryIntent,
		AgentIntent secondaryIntent, AgentType agentType, Map<String, String> extractedData, List<String> missingData,
		String urgency, boolean requiresHuman, String handoffReason, String responseToCustomer, double confidence,
		String summaryForHuman, String source) {
}
