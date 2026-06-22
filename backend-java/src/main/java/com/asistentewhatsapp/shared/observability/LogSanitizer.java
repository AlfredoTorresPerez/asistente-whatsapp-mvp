package com.asistentewhatsapp.shared.observability;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LogSanitizer {

    private static final int MAX_TEXT_LENGTH = 240;
    private static final AtomicBoolean INCLUDE_MESSAGE_BODY = new AtomicBoolean(false);
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "body",
            "customerName",
            "customer_name",
            "displayName",
            "display_name",
            "extractedData",
            "from",
            "lastInbound",
            "messageBody",
            "normalizedPhone",
            "originalText",
            "phone",
            "phoneNumber",
            "phone_number",
            "rawText",
            "recipientPhone",
            "responseText",
            "to",
            "ultimo_mensaje_cliente",
            "ultima_respuesta_ia");

    private LogSanitizer() {
    }

    public static void setIncludeMessageBody(boolean includeMessageBody) {
        INCLUDE_MESSAGE_BODY.set(includeMessageBody);
    }

    public static boolean includeMessageBody() {
        return INCLUDE_MESSAGE_BODY.get();
    }

    public static String messageSummary(String label, String message) {
        String prefix = label == null || label.isBlank() ? "message" : label.trim();
        String value = message == null ? "" : message;
        String summary = prefix + "Length=" + value.length()
                + " " + prefix + "ContainsLink=" + containsLink(value);
        if (includeMessageBody()) {
            summary += " " + prefix + "=" + sanitizeFreeText(value);
        }
        return summary;
    }

    public static String responseSummary(String response) {
        return messageSummary("response", response);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String compact = phone.replaceAll("\\s+", "").trim();
        if (compact.length() <= 6) {
            return "****";
        }
        String prefix = compact.substring(0, Math.min(4, compact.length()));
        String suffix = compact.substring(Math.max(0, compact.length() - 4));
        return prefix + "****" + suffix;
    }

    public static String maskExternalId(String externalMessageId) {
        if (externalMessageId == null || externalMessageId.isBlank()) {
            return "";
        }
        String value = externalMessageId.trim();
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    public static String sanitizeFreeText(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replaceAll("(?i)authorization=([^, ]+)", "authorization=***")
                .replaceAll("(?i)token=([^, ]+)", "token=***")
                .replaceAll("(?i)(/reservas/(?:confirmar|reprogramar|cancelar)/)[A-Za-z0-9_\\-]+", "$1***")
                .replaceAll("\\+?\\d[\\d\\s().-]{7,}\\d", "[TELEFONO_REDACTADO]")
                .replaceAll("(?i)(nombre|cliente|customerName)=([^,]+)", "$1=[REDACTADO]")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.length() > MAX_TEXT_LENGTH) {
            return sanitized.substring(0, MAX_TEXT_LENGTH) + "...";
        }
        return sanitized;
    }

    public static String summarizeMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (index > 0) {
                builder.append(", ");
            }
            if (index >= 20) {
                builder.append("...");
                break;
            }
            String key = String.valueOf(entry.getKey());
            builder.append(key).append("=").append(summarizeValue(key, entry.getValue()));
            index++;
        }
        builder.append("}");
        return builder.toString();
    }

    private static String summarizeValue(String key, Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (isSensitiveKey(key)) {
            if (includeMessageBody()) {
                return sanitizeFreeText(text);
            }
            return "length=" + text.length() + ",containsLink=" + containsLink(text);
        }
        return sanitizeFreeText(text);
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.replace("-", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream()
                .map(candidate -> candidate.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private static boolean containsLink(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return normalized.contains("http://") || normalized.contains("https://") || normalized.contains("/reservas/");
    }
}
