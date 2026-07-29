package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidad centralizada para trazabilidad conversacional de IA. Evita imprimir
 * secretos, tokens completos o telefonos completos.
 */
public final class AiTraceLogger {

	private static final Logger log = LoggerFactory.getLogger("AI_TRACE");
	private static final int MAX_TEXT_LENGTH = 600;

	private AiTraceLogger() {
	}

	public static String newTraceId(String prefix) {
		String normalizedPrefix = prefix == null || prefix.isBlank() ? "TRACE" : prefix.trim().toUpperCase();
		return normalizedPrefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	public static String traceId(AgentConversationRequest request) {
		if (request != null && request.traceId() != null && !request.traceId().isBlank()) {
			return request.traceId();
		}
		return newTraceId("AI");
	}

	public static void info(String step, String traceId, UUID conversationId, UUID messageId, String layer,
			String detail) {
		log.info("[AI_TRACE] step={} traceId={} conversationId={} messageId={} layer={} {}", safe(step), safe(traceId),
				safeId(conversationId), safeId(messageId), safe(layer), sanitizeText(detail));
	}

	public static void debug(String step, String traceId, UUID conversationId, UUID messageId, String layer,
			String detail) {
		log.debug("[AI_TRACE] step={} traceId={} conversationId={} messageId={} layer={} {}", safe(step), safe(traceId),
				safeId(conversationId), safeId(messageId), safe(layer), sanitizeText(detail));
	}

	public static void warn(String step, String traceId, UUID conversationId, UUID messageId, String layer,
			String detail) {
		log.warn("[AI_TRACE] step={} traceId={} conversationId={} messageId={} layer={} {}", safe(step), safe(traceId),
				safeId(conversationId), safeId(messageId), safe(layer), sanitizeText(detail));
	}

	public static void error(String step, String traceId, UUID conversationId, UUID messageId, String layer,
			String detail, Throwable exception) {
		log.error("[AI_TRACE] step={} traceId={} conversationId={} messageId={} layer={} {}", safe(step), safe(traceId),
				safeId(conversationId), safeId(messageId), safe(layer), sanitizeText(detail), exception);
	}

	public static String maskPhone(String phone) {
		return LogSanitizer.maskPhone(phone);
	}

	public static String maskToken(String token) {
		if (token == null || token.isBlank()) {
			return "";
		}
		String trimmed = token.trim();
		if (trimmed.length() <= 12) {
			return "***";
		}
		return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 4);
	}

	public static String sanitizeText(String value) {
		if (value == null) {
			return "";
		}
		String sanitized = LogSanitizer.sanitizeFreeText(value);
		if (sanitized.length() > MAX_TEXT_LENGTH) {
			return sanitized.substring(0, MAX_TEXT_LENGTH) + "...";
		}
		return sanitized;
	}

	public static String summarizeMap(Map<?, ?> values) {
		if (values == null || values.isEmpty()) {
			return "{}";
		}
		return LogSanitizer.summarizeMap(values);
	}

	public static String summarizeCollection(Collection<?> values) {
		if (values == null || values.isEmpty()) {
			return "[]";
		}
		StringBuilder builder = new StringBuilder("[");
		int index = 0;
		for (Object value : values) {
			if (index > 0) {
				builder.append(", ");
			}
			if (index >= 20) {
				builder.append("...");
				break;
			}
			builder.append(sanitizeText(String.valueOf(value)));
			index++;
		}
		builder.append("]");
		return builder.toString();
	}

	public static String safe(String value) {
		return value == null || value.isBlank() ? "-" : value.trim();
	}

	public static String safeId(UUID value) {
		return value == null ? "-" : value.toString();
	}
}
