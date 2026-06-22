package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentType;
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
    public AgentRoutingResult handle(
            AgentConversationRequest request,
            IntentDetectionResult intent,
            Map<String, String> entities,
            List<String> missingData) {
        String normalized = normalize(request.messageBody());
        String response;
        if (isSocialGreeting(normalized)) {
            response = "Muy bien, gracias. ¿Te ayudo con precios, servicios o agenda?";
        } else if (has(entities, "nombre")) {
            response = "Gracias. ¿Qué necesitas resolver hoy?";
        } else {
            response = "Hola, gracias por escribirnos. ¿Te ayudo con servicios, precios o agenda?";
        }
        return result(request, intent, type(), entities, missing("motivo_contacto"), response, false, null);
    }

    private boolean isSocialGreeting(String normalized) {
        return normalized.contains("como estas")
                || normalized.contains("como esta")
                || normalized.contains("que tal")
                || normalized.contains("hola como estas")
                || normalized.contains("hola que tal");
    }

    private String normalize(String value) {
        return TextNormalizer.normalize(value);
    }
}
