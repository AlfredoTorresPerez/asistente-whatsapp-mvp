package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractAgentHandler implements AgentHandler {

    protected AgentRoutingResult result(
            AgentConversationRequest request,
            IntentDetectionResult intent,
            AgentType agentType,
            Map<String, String> entities,
            List<String> missingData,
            String response,
            boolean requiresHuman,
            String handoffReason) {
        return new AgentRoutingResult(
                request.businessId(),
                request.conversationId(),
                request.customerId(),
                intent.primaryIntent(),
                intent.secondaryIntent(),
                agentType,
                entities,
                missingData,
                intent.urgency(),
                requiresHuman,
                handoffReason,
                response,
                intent.confidence(),
                summary(request, intent, agentType, entities, handoffReason));
    }

    protected List<String> missing(String... values) {
        List<String> missingData = new ArrayList<>();
        for (String value : values) {
            missingData.add(value);
        }
        return missingData;
    }

    protected boolean has(Map<String, String> entities, String key) {
        String value = entities.get(key);
        return value != null && !value.isBlank();
    }

    private String summary(
            AgentConversationRequest request,
            IntentDetectionResult intent,
            AgentType agentType,
            Map<String, String> entities,
            String handoffReason) {
        String customer = request.customerDisplayName() == null || request.customerDisplayName().isBlank()
                ? request.customerPhone()
                : request.customerDisplayName();
        String reason = handoffReason == null || handoffReason.isBlank() ? "ninguno" : handoffReason;
        return "Cliente: " + customer
                + "\nTelefono: " + request.customerPhone()
                + "\nIntencion: " + intent.primaryIntent()
                + "\nAgente sugerido: " + agentType.displayName()
                + "\nUrgencia: " + intent.urgency()
                + "\nDatos: " + entities
                + "\nUltimo mensaje: " + request.messageBody()
                + "\nMotivo derivacion: " + reason;
    }
}
