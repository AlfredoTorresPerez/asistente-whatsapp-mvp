package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SalesAgent extends AbstractAgentHandler {

    private final AiBusinessKnowledgeService knowledgeService;

    public SalesAgent(AiBusinessKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public AgentType type() {
        return AgentType.SALES;
    }

    @Override
    public AgentRoutingResult handle(
            AgentConversationRequest request,
            IntentDetectionResult intent,
            Map<String, String> entities,
            List<String> missingData) {
        String service = value(entities, "servicio_o_producto");
        AgentIntent primaryIntent = intent.primaryIntent();

        if (primaryIntent == AgentIntent.COMMERCIAL_INQUIRY
                && knowledgeService.isCategoryQuestion(request.businessId(), "DEPILACION", request.messageBody())) {
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    missing("servicio_o_producto_especifico"),
                    knowledgeService.depilationCatalogResponse(request.businessId()),
                    false,
                    null);
        }

        if (primaryIntent == AgentIntent.PRICE_REQUEST) {
            return knowledgeService.findService(request.businessId(), service)
                    .map(catalogService -> result(
                            request,
                            intent,
                            type(),
                            entities,
                            List.<String>of(),
                            knowledgeService.servicePriceResponse(request.businessId(), catalogService),
                            false,
                            null))
                    .orElseGet(() -> result(
                            request,
                            intent,
                            type(),
                            entities,
                            missing("servicio_o_producto_especifico"),
                            knowledgeService.renderRule(request.businessId(), "AI_PRICE_UNKNOWN_SERVICE_RESPONSE", Map.of()),
                            false,
                            null));
        }

        if (primaryIntent == AgentIntent.QUOTE_REQUEST) {
            String category = firstNonBlank(value(entities, "categoria_servicio"), genericDepilationCategory(service, request.messageBody()));
            if (!category.isBlank()) {
                entities.putIfAbsent("categoria_servicio", category);
                return result(
                        request,
                        intent,
                        type(),
                        entities,
                        missing("zona_corporal"),
                        knowledgeService.quoteMissingDetailResponse(request.businessId(), category),
                        false,
                        null);
            }
        }

        if (!has(entities, "servicio_o_producto")) {
            return result(
                    request,
                    intent,
                    type(),
                    entities,
                    missing("servicio_o_producto"),
                    knowledgeService.renderRule(request.businessId(), "AI_SALES_MISSING_SERVICE_RESPONSE", Map.of()),
                    false,
                    null);
        }

        String response = knowledgeService.renderRule(
                request.businessId(),
                "AI_SALES_NEXT_STEP_RESPONSE",
                Map.of("service", service));
        return result(request, intent, type(), entities, missing("siguiente_paso"), response, false, null);
    }


    private String genericDepilationCategory(String service, String messageBody) {
        String normalizedService = TextNormalizer.normalize(service);
        String normalizedMessage = TextNormalizer.normalize(messageBody);
        if ("depilacion".equals(normalizedService) || normalizedMessage.contains("depilacion")) {
            return "depilación";
        }
        return "";
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    private String value(Map<String, String> entities, String key) {
        String value = entities.get(key);
        return value == null ? "" : value.trim();
    }
}
