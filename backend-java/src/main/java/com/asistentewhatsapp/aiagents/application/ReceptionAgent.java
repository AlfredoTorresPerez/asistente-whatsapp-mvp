package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReceptionAgent extends AbstractAgentHandler {

	@Override
	public AgentType type() {
		return AgentType.RECEPTION;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		String normalized = normalize(request.messageBody());
		String response;
		if (intent.primaryIntent() == AgentIntent.THANKS_OR_FAREWELL) {
			response = "Gracias a ti. Si necesitas algo más, aquí estaré.";
		} else if (intent.primaryIntent() == AgentIntent.AMBIGUOUS) {
			response = "¡Hola! Soy el asistente virtual de Centro Estético Bella. Puedo ayudarte a:"
					+ "\n1. Revisar servicios y precios" + "\n2. Agendar, reprogramar o cancelar horas"
					+ "\n3. Consultar sucursales y horarios" + "\n4. Ver estado de tus reservas"
					+ "\n¿En qué te puedo ayudar?";
		} else if (isSocialGreeting(normalized)) {
			response = "Muy bien, gracias. ¿Te ayudo con precios, servicios o agenda?";
		} else if (has(entities, "nombre")) {
			response = "Gracias. ¿Qué necesitas resolver hoy?";
		} else {
			response = "Hola, gracias por escribirnos. ¿Te ayudo con servicios, precios o agenda?";
		}
		return result(request, intent, type(), entities, missing("motivo_contacto"), response, false, null);
	}

	private boolean isSocialGreeting(String normalized) {
		return normalized.contains("como estas") || normalized.contains("como esta") || normalized.contains("que tal")
				|| normalized.contains("hola como estas") || normalized.contains("hola que tal");
	}

	private String normalize(String value) {
		return TextNormalizer.normalize(value);
	}
}
