package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;

public record IntentDetectionResult(AgentIntent primaryIntent, AgentIntent secondaryIntent, double confidence,
		String urgency, boolean requiresHuman, String handoffReason) {
}
