package com.asistentewhatsapp.aiagents.application;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiBusinessKnowledgeService {

    private final AiKnowledgeRepository repository;

    public AiBusinessKnowledgeService(AiKnowledgeRepository repository) {
        this.repository = repository;
    }

    public List<AiKnowledgeRepository.EntityAlias> findAliases(UUID businessId) {
        return repository.findActiveEntityAliases(businessId).stream()
                .sorted(Comparator.comparing(AiKnowledgeRepository.EntityAlias::priority).reversed())
                .toList();
    }

    public Optional<AiKnowledgeRepository.ServiceCatalogItem> findService(UUID businessId, String serviceText) {
        String normalizedService = normalize(serviceText);
        if (normalizedService.isBlank()) {
            return Optional.empty();
        }
        return repository.findActiveServices(businessId).stream()
                .filter(service -> normalize(service.name()).equals(normalizedService)
                        || normalize(service.code()).equals(normalizedService)
                        || normalizedService.contains(normalize(service.name()))
                        || normalize(service.name()).contains(normalizedService))
                .min(Comparator.comparingInt(service -> Math.abs(normalize(service.name()).length() - normalizedService.length())));
    }

    public Optional<AiKnowledgeRepository.ServiceCatalogItem> findServiceMentionedInText(UUID businessId, String text) {
        String normalizedText = normalize(text);
        if (normalizedText.isBlank()) {
            return Optional.empty();
        }
        return repository.findActiveServices(businessId).stream()
                .map(service -> new ServiceMatch(service, serviceMentionScore(normalizedText, service)))
                .filter(match -> match.score() > 0)
                .max(Comparator
                        .comparingInt(ServiceMatch::score)
                        .thenComparingInt(match -> normalize(match.service().name()).length()))
                .map(ServiceMatch::service);
    }

    public boolean isCategoryQuestion(UUID businessId, String categoryCode, String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return false;
        }
        boolean asksOptions = containsAny(normalized, "que", "tipo", "tipos", "ofrecen", "opciones", "servicios", "tratamientos");
        boolean referencesCategory = repository.findActiveServices(businessId).stream()
                .filter(service -> categoryCode.equalsIgnoreCase(service.categoryCode()))
                .anyMatch(service -> normalized.contains(firstCategoryToken(service.name())) || normalized.contains(normalize(service.name())));
        return asksOptions && referencesCategory;
    }

    public String renderRule(UUID businessId, String code, Map<String, String> variables) {
        String template = repository.findActiveRule(businessId, code)
                .map(AiKnowledgeRepository.ResponseRule::template)
                .orElseGet(() -> repository.findActiveRule(businessId, "AI_GENERIC_NEXT_STEP")
                        .map(AiKnowledgeRepository.ResponseRule::template)
                        .orElse("¿Qué necesitas revisar hoy?"));
        return render(template, variables);
    }

    public String depilationCatalogResponse(UUID businessId) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("services", depilationLabels(businessId));
        return renderRule(businessId, "AI_DEPILATION_CATALOG_RESPONSE", variables);
    }

    public String servicePriceResponse(UUID businessId, AiKnowledgeRepository.ServiceCatalogItem service) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("service", displayServiceName(service.name()));
        variables.put("price", formatMoney(service.priceBase()));
        variables.put("duration", String.valueOf(service.durationMinutes()));
        return renderRule(businessId, "AI_PRICE_KNOWN_SERVICE_RESPONSE", variables);
    }

    public String quoteMissingDetailResponse(UUID businessId, String category) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("category", category == null || category.isBlank() ? "el servicio" : category);
        variables.put("options", rulePayloadListAsText(businessId, "AI_QUOTE_MISSING_DETAIL_RESPONSE", "options"));
        return renderRule(businessId, "AI_QUOTE_MISSING_DETAIL_RESPONSE", variables);
    }

    public String paymentWithRequestAndAmountResponse(UUID businessId, String requestNumber, String amount) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("requestNumber", requestNumber);
        variables.put("amount", formatAmount(amount));
        return renderRule(businessId, "AI_PAYMENT_REQUEST_AMOUNT_RESPONSE", variables);
    }

    public String bookingCompleteResponse(UUID businessId, String service, String date, String time) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("service", displayServiceName(service));
        variables.put("date", date);
        variables.put("time", time);
        return renderRule(businessId, "AI_BOOKING_COMPLETE_RESPONSE", variables);
    }

    public String bookingMissingServiceResponse(UUID businessId) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("examples", rulePayloadListAsText(businessId, "AI_BOOKING_MISSING_SERVICE_RESPONSE", "examples"));
        return renderRule(businessId, "AI_BOOKING_MISSING_SERVICE_RESPONSE", variables);
    }

    public String bookingChangeIdentifyResponse(UUID businessId) {
        return renderRule(businessId, "AI_BOOKING_CHANGE_IDENTIFY_RESPONSE", Map.of());
    }

    private int serviceMentionScore(String normalizedText, AiKnowledgeRepository.ServiceCatalogItem service) {
        String normalizedName = normalize(service.name());
        String normalizedCode = normalize(service.code());
        if (!normalizedName.isBlank() && normalizedText.contains(normalizedName)) {
            return 1000 + normalizedName.length();
        }
        if (!normalizedCode.isBlank() && normalizedText.contains(normalizedCode)) {
            return 950 + normalizedCode.length();
        }

        List<String> tokens = meaningfulServiceTokens(normalizedName);
        if (tokens.size() < 2) {
            return 0;
        }

        String corePhrase = String.join(" ", tokens);
        if (normalizedText.contains(corePhrase)) {
            return 850 + corePhrase.length();
        }
        boolean allCoreTokensPresent = tokens.stream().allMatch(token -> containsWholeToken(normalizedText, token));
        if (allCoreTokensPresent) {
            return 700 + tokens.stream().mapToInt(String::length).sum();
        }
        return 0;
    }

    private List<String> meaningfulServiceTokens(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(normalizedName.split(" "))
                .filter(token -> token.length() > 2)
                .filter(token -> !isServiceModifier(token))
                .distinct()
                .toList();
    }

    private boolean isServiceModifier(String token) {
        return switch (token) {
            case "profunda", "profundo", "basica", "basico", "suave", "completa", "completo",
                    "estetica", "estetico", "cosmetica", "cosmetico", "invasiva", "invasivo", "controlada",
                    "controlado", "tradicional", "premium", "demo" -> true;
            default -> false;
        };
    }

    private boolean containsWholeToken(String normalizedText, String token) {
        return java.util.Arrays.asList(normalizedText.split(" ")).contains(token);
    }

    private String depilationLabels(UUID businessId) {
        Optional<AiKnowledgeRepository.ResponseRule> rule = repository.findActiveRule(businessId, "AI_DEPILATION_CATALOG_RESPONSE");
        String configured = rule.map(item -> payloadListAsText(item.payload(), "labels")).orElse("");
        if (!configured.isBlank()) {
            return configured;
        }
        List<String> names = repository.findActiveServices(businessId).stream()
                .filter(service -> "DEPILACION".equalsIgnoreCase(service.categoryCode()))
                .map(service -> displayServiceName(service.name()))
                .distinct()
                .toList();
        return joinNatural(names);
    }

    private String rulePayloadListAsText(UUID businessId, String code, String key) {
        return repository.findActiveRule(businessId, code)
                .map(rule -> payloadListAsText(rule.payload(), key))
                .orElse("");
    }

    private String payloadListAsText(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value instanceof List<?> list) {
            List<String> labels = new ArrayList<>();
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    labels.add(item.toString());
                }
            }
            return joinNatural(labels);
        }
        return "";
    }

    private String joinNatural(List<String> values) {
        List<String> clean = values.stream().filter(value -> value != null && !value.isBlank()).toList();
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.size() == 1) {
            return clean.getFirst();
        }
        return String.join(", ", clean.subList(0, clean.size() - 1)) + " y " + clean.getLast();
    }

    private String render(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered.replaceAll("\\s+", " ").trim();
    }

    private String displayServiceName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim()
                .replace("Depilacion", "depilación")
                .replace("depilacion", "depilación")
                .replace("Laser", "láser")
                .replace("laser", "láser");
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.substring(0, 1).toLowerCase(Locale.ROOT) + normalized.substring(1);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return "$" + format.format(value.longValue());
    }

    private String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return "";
        }
        try {
            long value = Long.parseLong(amount.replaceAll("[^0-9]", ""));
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,###", symbols);
            return "$" + format.format(value);
        } catch (NumberFormatException ex) {
            return amount;
        }
    }

    private String firstCategoryToken(String value) {
        String normalized = normalize(value);
        int index = normalized.indexOf(' ');
        return index < 0 ? normalized : normalized.substring(0, index);
    }

    private boolean containsAny(String normalized, String... candidates) {
        for (String candidate : candidates) {
            if (normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return TextNormalizer.normalize(value);
    }

    private record ServiceMatch(AiKnowledgeRepository.ServiceCatalogItem service, int score) {
    }
}
