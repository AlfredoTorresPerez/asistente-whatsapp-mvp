package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeAgent extends AbstractAgentHandler {

	@Override
	public AgentType type() {
		return AgentType.KNOWLEDGE;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		String response = "Puedo revisar la información autorizada. ¿Sobre qué política, documento o servicio necesitas información?";
		return result(request, intent, type(), entities, missing("tema_documental"), response, false, null);
	}
}
