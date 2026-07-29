package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentsAgent extends AbstractAgentHandler {

	private final AiBusinessKnowledgeService knowledgeService;

	public PaymentsAgent(AiBusinessKnowledgeService knowledgeService) {
		this.knowledgeService = knowledgeService;
	}

	@Override
	public AgentType type() {
		return AgentType.PAYMENTS;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		List<String> missing = new ArrayList<>();
		boolean hasRequest = has(entities, "numero_solicitud");
		boolean hasAmount = has(entities, "monto");
		if (!hasRequest) {
			missing.add("numero_pedido_o_solicitud");
		}
		if (!hasAmount) {
			missing.add("monto");
		}

		String response;
		String normalizedMessage = TextNormalizer.normalize(request.messageBody());
		if (containsAny(normalizedMessage, "por persona", "por la reserva completa")) {
			response = "El pago varía según el servicio y la cantidad de personas. Para darte una respuesta precisa, ¿me indicas el servicio exacto que te interesa?";
		} else if (containsAny(normalizedMessage, "senal", "señal", "abono", "abonar", "pagar reserva",
				"link de pago")) {
			missing.clear();
			missing.add("servicio_o_reserva");
			response = "Para responder sobre señal o pago debo revisar la regla configurada del servicio o reserva. No voy a inventar montos. ¿Qué servicio quieres reservar?";
		} else if (hasRequest && hasAmount) {
			response = knowledgeService.paymentWithRequestAndAmountResponse(request.businessId(),
					entities.get("numero_solicitud"), entities.get("monto"));
			missing.clear();
			missing.add("metodo_pago");
		} else if (hasRequest) {
			response = knowledgeService.renderRule(request.businessId(), "AI_PAYMENT_MISSING_AMOUNT_RESPONSE",
					Map.of("requestNumber", entities.get("numero_solicitud")));
		} else {
			response = knowledgeService.renderRule(request.businessId(), "AI_PAYMENT_MISSING_REQUEST_RESPONSE",
					Map.of());
		}
		return result(request, intent, type(), entities, missing, response, false, null);
	}

	private boolean containsAny(String normalized, String... values) {
		if (normalized == null || normalized.isBlank()) {
			return false;
		}
		for (String value : values) {
			if (normalized.contains(TextNormalizer.normalize(value))) {
				return true;
			}
		}
		return false;
	}
}
