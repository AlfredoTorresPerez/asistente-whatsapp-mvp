package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Map;

public interface AgentHandler {

    AgentType type();

    AgentRoutingResult handle(
            AgentConversationRequest request,
            IntentDetectionResult intent,
            Map<String, String> entities,
            List<String> missingData);
}
