package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class EntityExtractionService {

    private static final Pattern EXPLICIT_TIME_WITH_PREFIX_PATTERN = Pattern.compile(
            "\\b(?:a\\s+las|desde\\s+las|para\\s+las|hora|horario)\\s+([01]?\\d|2[0-3])(?:(?::|\\.)([0-5]\\d)|\\s*(?:h|hrs?|horas?)?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLICIT_TIME_WITH_SEPARATOR_PATTERN = Pattern.compile(
            "\\b([01]?\\d|2[0-3])(?::|\\.)([0-5]\\d)\\s*(?:h|hrs?|horas?)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLICIT_TIME_WITH_SUFFIX_PATTERN = Pattern.compile(
            "\\b([01]?\\d|2[0-3])\\s*(?:h|hrs?|horas?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\$\\s?([0-9][0-9.]{2,})");
    private static final Pattern ORDER_PATTERN = Pattern.compile("(?:pedido|orden|solicitud|folio)\\s*#?\\s*([a-zA-Z0-9-]{4,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN = Pattern.compile("(?:soy|mi nombre es|me llamo)\\s+([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATIVE_DATE_TIME_LOCATION_PATTERN = Pattern.compile(
            "\\b(?:hoy|ma(?:n|ñ)ana|pasado\\s+ma(?:n|ñ)ana|lunes|martes|mi(?:e|é)rcoles|jueves|viernes|s(?:a|á)bado|domingo)\\b.*?"
                    + "\\b(?:a\\s+las|desde\\s+las|para\\s+las)?\\s*([01]?\\d|2[0-3])\\s*(?:h|hrs?|horas?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATIVE_DATE_BARE_HOUR_PATTERN = Pattern.compile(
            "\\b(?:hoy|ma(?:n|ñ)ana|pasado\\s+ma(?:n|ñ)ana)\\s+([01]?\\d|2[0-3])\\b(?!\\s*(?:de|/|-|:|\\.))",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> WEEKDAYS = Map.of(
            "lunes", "lunes",
            "martes", "martes",
            "miercoles", "miércoles",
            "jueves", "jueves",
            "viernes", "viernes",
            "sabado", "sábado",
            "domingo", "domingo");

    private final AiBusinessKnowledgeService knowledgeService;
    private final BusinessLocationJdbcRepository businessLocationJdbcRepository;

    public EntityExtractionService(
            AiBusinessKnowledgeService knowledgeService,
            BusinessLocationJdbcRepository businessLocationJdbcRepository) {
        this.knowledgeService = knowledgeService;
        this.businessLocationJdbcRepository = businessLocationJdbcRepository;
    }

    public Map<String, String> extract(AgentConversationRequest request) {
        String traceId = AiTraceLogger.traceId(request);
        Map<String, String> entities = new LinkedHashMap<>();
        String message = request.messageBody() == null ? "" : request.messageBody();
        String normalizedForTrace = normalize(message);
        AiTraceLogger.info("MESSAGE_NORMALIZED", traceId, request.conversationId(), null, "EntityExtractionService",
                LogSanitizer.messageSummary("message", message)
                        + " normalizedLength=" + normalizedForTrace.length());

        addTimeIfFound(entities, message);
        addIfFound(entities, "fecha", DATE_PATTERN.matcher(message));
        addAmountIfFound(entities, AMOUNT_PATTERN.matcher(message));
        addRequestNumberIfFound(entities, ORDER_PATTERN.matcher(message));
        addIfFound(entities, "correo", EMAIL_PATTERN.matcher(message));
        addNameIfFound(entities, NAME_PATTERN.matcher(message));

        String normalized = normalizedForTrace;
        applyDatabaseAliases(entities, request, normalized);
        applyServiceCatalogInference(entities, request, message);
        applyWeekdayRelativeDates(entities, normalized);
        applyBusinessLocationAliases(entities, request, normalized);

        String standaloneName = inferStandaloneName(message, normalized);
        if (standaloneName != null) {
            entities.putIfAbsent("nombre", standaloneName);
        }

        if (request.customerDisplayName() != null && !request.customerDisplayName().isBlank()) {
            entities.putIfAbsent("cliente", request.customerDisplayName());
        }
        if (request.customerPhone() != null && !request.customerPhone().isBlank()) {
            entities.putIfAbsent("telefono", request.customerPhone());
        }

        AiTraceLogger.info("ENTITIES_EXTRACTED", traceId, request.conversationId(), null, "EntityExtractionService",
                "entities=" + AiTraceLogger.summarizeMap(entities)
                        + " serviceText=" + entities.getOrDefault("servicio_o_producto", "")
                        + " dateText=" + firstNonBlank(entities.get("fecha"), entities.get("fecha_relativa"))
                        + " timeText=" + entities.getOrDefault("hora", "")
                        + " locationText=" + entities.getOrDefault("sede", ""));
        return entities;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : (second == null ? "" : second);
    }

    private void applyDatabaseAliases(Map<String, String> entities, AgentConversationRequest request, String normalizedMessage) {
        for (AiKnowledgeRepository.EntityAlias alias : knowledgeService.findAliases(request.businessId())) {
            String normalizedAlias = normalize(alias.alias());
            if (!normalizedAlias.isBlank() && normalizedMessage.contains(normalizedAlias)) {
                entities.putIfAbsent(alias.entityKey(), alias.entityValue());
            }
        }
    }

    private void applyServiceCatalogInference(Map<String, String> entities, AgentConversationRequest request, String message) {
        String currentService = entities.get("servicio_o_producto");
        if (currentService != null && !currentService.isBlank() && !isGenericService(currentService)) {
            return;
        }
        knowledgeService.findServiceMentionedInText(request.businessId(), message)
                .ifPresent(service -> {
                    entities.put("servicio_o_producto", service.name());
                    if (service.code() != null && !service.code().isBlank()) {
                        entities.putIfAbsent("servicio_codigo", service.code());
                    }
                });
    }

    private void applyWeekdayRelativeDates(Map<String, String> entities, String normalizedMessage) {
        if (entities.containsKey("fecha") || entities.containsKey("fecha_relativa")) {
            return;
        }
        for (Map.Entry<String, String> weekday : WEEKDAYS.entrySet()) {
            if (containsWholeToken(normalizedMessage, weekday.getKey())) {
                entities.put("fecha_relativa", weekday.getValue());
                return;
            }
        }
        if (normalizedMessage.contains("pasado manana")) {
            entities.put("fecha_relativa", "pasado mañana");
            return;
        }
        if (containsWholeToken(normalizedMessage, "hoy")) {
            entities.put("fecha_relativa", "hoy");
            return;
        }
        if (containsWholeToken(normalizedMessage, "manana")
                && !normalizedMessage.contains("en la manana")
                && !normalizedMessage.contains("por la manana")) {
            entities.put("fecha_relativa", "mañana");
            return;
        }
        if (normalizedMessage.contains("esta semana")) {
            entities.put("fecha_relativa", "esta semana");
            return;
        }
        if (normalizedMessage.contains("proxima semana") || normalizedMessage.contains("la otra semana")) {
            entities.put("fecha_relativa", "próxima semana");
        }
    }

    private boolean isGenericService(String value) {
        String normalized = normalize(value);
        return normalized.equals("depilacion") || normalized.equals("facial") || normalized.equals("servicio");
    }


    private void applyBusinessLocationAliases(Map<String, String> entities, AgentConversationRequest request, String normalizedMessage) {
        if (normalizedMessage.isBlank() || entities.containsKey("sede")) {
            return;
        }
        var locations = businessLocationJdbcRepository.findActive(request.businessId());
        for (BusinessLocationRecord location : locations) {
            if (containsNormalized(normalizedMessage, location.name())
                    || containsNormalized(normalizedMessage, location.code())) {
                entities.put("sede", location.name());
                entities.put("sede_id", location.id().toString());
                return;
            }
        }
        for (BusinessLocationRecord location : locations) {
            if (containsNormalized(normalizedMessage, location.commune())
                    || containsNormalized(normalizedMessage, location.city())) {
                entities.put("sede", location.name());
                entities.put("sede_id", location.id().toString());
                return;
            }
        }
    }

    private boolean containsNormalized(String normalizedText, String value) {
        String normalizedValue = normalize(value);
        return !normalizedValue.isBlank() && normalizedText.contains(normalizedValue);
    }

    private void addIfFound(Map<String, String> entities, String key, Matcher matcher) {
        if (matcher.find()) {
            entities.put(key, matcher.group());
        }
    }

    private void addTimeIfFound(Map<String, String> entities, String message) {
        Matcher explicitWithPrefix = EXPLICIT_TIME_WITH_PREFIX_PATTERN.matcher(message);
        if (explicitWithPrefix.find()) {
            entities.put("hora", normalizeTime(explicitWithPrefix.group(1), explicitWithPrefix.group(2)));
            return;
        }
        Matcher explicitWithSeparator = EXPLICIT_TIME_WITH_SEPARATOR_PATTERN.matcher(message);
        if (explicitWithSeparator.find()) {
            entities.put("hora", normalizeTime(explicitWithSeparator.group(1), explicitWithSeparator.group(2)));
            return;
        }
        Matcher explicitWithSuffix = EXPLICIT_TIME_WITH_SUFFIX_PATTERN.matcher(message);
        if (explicitWithSuffix.find()) {
            entities.put("hora", normalizeTime(explicitWithSuffix.group(1), null));
            return;
        }
        Matcher relativeDateBareHour = RELATIVE_DATE_BARE_HOUR_PATTERN.matcher(message);
        if (relativeDateBareHour.find()) {
            entities.put("hora", normalizeTime(relativeDateBareHour.group(1), null));
            return;
        }
        Matcher relativeDateTimeLocation = RELATIVE_DATE_TIME_LOCATION_PATTERN.matcher(message);
        if (relativeDateTimeLocation.find()) {
            entities.put("hora", normalizeTime(relativeDateTimeLocation.group(1), null));
        }
    }

    private String normalizeTime(String hour, String minute) {
        int parsedHour = Integer.parseInt(hour);
        int parsedMinute = minute == null ? 0 : Integer.parseInt(minute);
        return String.format(Locale.ROOT, "%02d:%02d", parsedHour, parsedMinute);
    }

    private void addNameIfFound(Map<String, String> entities, Matcher matcher) {
        if (matcher.find()) {
            entities.put("nombre", matcher.group(1).trim());
        }
    }

    private void addAmountIfFound(Map<String, String> entities, Matcher matcher) {
        if (matcher.find()) {
            entities.put("monto", matcher.group(1).replace(".", ""));
        }
    }

    private void addRequestNumberIfFound(Map<String, String> entities, Matcher matcher) {
        if (matcher.find()) {
            entities.put("numero_solicitud", matcher.group(1));
        }
    }

    private String inferStandaloneName(String original, String normalized) {
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("@") || containsAny(normalized,
                "hola", "buenas", "como estas", "como esta", "que tal", "gracias", "precio", "valor", "agendar", "agenda",
                "reserva", "reservar", "cita", "hora", "pago", "soporte", "necesito", "quiero", "favor")) {
            return null;
        }
        String trimmed = original == null ? "" : original.trim();
        if (!trimmed.matches("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,2}")) {
            return null;
        }
        if (trimmed.length() < 3 || trimmed.length() > 80) {
            return null;
        }
        return trimmed;
    }

    private boolean containsWholeToken(String normalizedText, String token) {
        if (normalizedText == null || normalizedText.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String part : normalizedText.split(" ")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
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
}
