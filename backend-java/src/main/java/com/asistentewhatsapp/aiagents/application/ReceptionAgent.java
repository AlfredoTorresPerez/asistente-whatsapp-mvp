package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ReceptionAgent extends AbstractAgentHandler {

	private static final Set<String> MENU_OPTIONS = Set.of("1", "2", "3", "4");

	@Override
	public AgentType type() {
		return AgentType.RECEPTION;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		String normalized = normalize(request.messageBody());
		if (intent.primaryIntent() == AgentIntent.THANKS_OR_FAREWELL) {
			return result(request, intent, type(), entities, missing("motivo_contacto"),
					"Gracias a ti. Si necesitas algo más, aquí estaré.", false, null);
		}
		if (intent.primaryIntent() == AgentIntent.AMBIGUOUS) {
			if (MENU_OPTIONS.contains(normalized.trim())) {
				return switch (normalized.trim()) {
					case "1" -> result(request, intent, type(), entities, missing("servicio_o_producto"),
							"Claro, ¿sobre qué servicio te gustaría información de precios o detalles?", false, null);
					case "2" -> result(request, intent, type(), entities, missing("servicio_o_producto"),
							"Perfecto, ¿qué servicio necesitas agendar?", false, null);
					case "3" -> result(request, intent, type(), entities, missing("sucursal"),
							"Claro, ¿sobre qué sucursal necesitas información de dirección u horarios?", false, null);
					case "4" -> result(request, intent, type(), entities, missing("reserva_a_consultar"),
							"Claro, puedo revisar tus reservas activas. ¿Me dices tu nombre y teléfono para buscarlas?",
							false, null);
					default -> result(request, intent, type(), entities, missing("motivo_contacto"),
							"No entendí tu selección. ¿Puedes repetirla?", false, null);
				};
			}
			return result(request, intent, type(), entities, missing("motivo_contacto"),
					"¡Hola! Soy el asistente virtual de Centro Estético Bella. Puedo ayudarte a:"
							+ "\n1. Revisar servicios y precios" + "\n2. Agendar, reprogramar o cancelar horas"
							+ "\n3. Consultar sucursales y horarios" + "\n4. Ver estado de tus reservas"
							+ "\n¿En qué te puedo ayudar?",
					false, null);
		}
		if (isSocialGreeting(normalized)) {
			return result(request, intent, type(), entities, missing("motivo_contacto"),
					"Muy bien, gracias. ¿Te ayudo con precios, servicios o agenda?", false, null);
		}
		if (has(entities, "nombre")) {
			return result(request, intent, type(), entities, missing("motivo_contacto"),
					"Gracias. ¿Qué necesitas resolver hoy?", false, null);
		}
		return result(request, intent, type(), entities, missing("motivo_contacto"),
				"Hola, gracias por escribirnos. ¿Te ayudo con servicios, precios o agenda?", false, null);
	}

	private boolean isSocialGreeting(String normalized) {
		return normalized.contains("como estas") || normalized.contains("como esta") || normalized.contains("que tal")
				|| normalized.contains("hola como estas") || normalized.contains("hola que tal");
	}

	private String normalize(String value) {
		return TextNormalizer.normalize(value);
	}
}
