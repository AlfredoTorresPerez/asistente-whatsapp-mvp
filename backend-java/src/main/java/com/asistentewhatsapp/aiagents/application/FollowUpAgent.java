package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FollowUpAgent extends AbstractAgentHandler {

	@Override
	public AgentType type() {
		return AgentType.FOLLOW_UP;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		String response = "Claro, puedo retomar el seguimiento. ¿Quieres continuar con la cotización, la cita o el pago pendiente?";
		return result(request, intent, type(), entities, missing("tipo_seguimiento"), response, false, null);
	}
}
