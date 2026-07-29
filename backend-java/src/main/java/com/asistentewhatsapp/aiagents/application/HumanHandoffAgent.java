package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HumanHandoffAgent extends AbstractAgentHandler {

	@Override
	public AgentType type() {
		return AgentType.HUMAN_HANDOFF;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		String reason = intent.handoffReason() == null ? "caso requiere revisión humana" : intent.handoffReason();
		String response = "caso sensible o reacción post tratamiento".equals(reason)
				? WhatsAppMessageFormatter.sensitiveCase()
				: WhatsAppMessageFormatter.humanHandoff();
		return result(request, intent, type(), entities, missing("contexto_para_ejecutivo"), response, true, reason);
	}
}
