package com.asistentewhatsapp.shared.observability;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.lang.reflect.RecordComponent;
import java.security.Principal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TraceSanitizer {

	private static final Set<String> SENSITIVE_KEYS = Set.of("authorization", "access_token", "accesstoken", "api_key",
			"apikey", "bearer", "cardnumber", "clave", "confirmPassword", "confirm_password", "cookie",
			"currentPassword", "current_password", "cvv", "documentNumber", "document_number", "from", "newPassword",
			"new_password", "password", "phoneNumber", "phone_number", "qrCode", "qr_code", "refresh_token",
			"refreshtoken", "secret", "signature", "x_signature", "x-signature", "token");

	private final TraceProperties traceProperties;

	public TraceSanitizer(TraceProperties traceProperties) {
		this.traceProperties = traceProperties;
	}

	public Object sanitizeArguments(Object[] args) {
		return sanitizeArguments(args, null);
	}

	public Object sanitizeArguments(Object[] args, String[] parameterNames) {
		if (args == null || args.length == 0) {
			return List.of();
		}

		List<Object> sanitized = new ArrayList<>();
		for (int index = 0; index < args.length; index++) {
			String parameterName = parameterNames != null && index < parameterNames.length
					? parameterNames[index]
					: null;
			Object value = parameterName != null && isSensitiveKey(parameterName)
					? "[REDACTADO]"
					: sanitize(args[index], 0);
			if (value != null) {
				sanitized.add(value);
			}
		}
		return truncate(sanitized.toString());
	}

	public Object sanitizeResult(Object value) {
		Object sanitized = sanitize(value, 0);
		return sanitized == null ? null : truncate(sanitized.toString());
	}

	private Object sanitize(Object value, int depth) {
		if (value == null) {
			return null;
		}

		if (isIgnoredFrameworkType(value)) {
			return null;
		}

		if (depth > 2) {
			return compactValue(value);
		}

		if (isSimpleValue(value)) {
			return compactValue(value);
		}

		if (value instanceof Map<?, ?> map) {
			Map<String, Object> sanitizedMap = new LinkedHashMap<>();
			int count = 0;
			sanitizedMap.put("_type", value.getClass().getSimpleName());
			sanitizedMap.put("_size", map.size());
			sanitizedMap.put("_keys", map.keySet().stream().limit(20).map(String::valueOf).toList());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (count++ >= 20) {
					sanitizedMap.put("_truncated", true);
					break;
				}
				String key = String.valueOf(entry.getKey());
				sanitizedMap.put(key, isSensitiveKey(key) ? "[REDACTADO]" : sanitize(entry.getValue(), depth + 1));
			}
			return sanitizedMap;
		}

		if (value instanceof Collection<?> collection) {
			List<Object> sanitizedItems = new ArrayList<>();
			int count = 0;
			sanitizedItems.add(Map.of("_type", value.getClass().getSimpleName(), "_size", collection.size()));
			for (Object item : collection) {
				if (count++ >= 20) {
					sanitizedItems.add("[TRUNCADO]");
					break;
				}
				sanitizedItems.add(sanitize(item, depth + 1));
			}
			return sanitizedItems;
		}

		if (value.getClass().isArray()) {
			return Map.of("_type", value.getClass().getComponentType().getSimpleName() + "[]", "_size",
					java.lang.reflect.Array.getLength(value));
		}

		if (value.getClass().isRecord()) {
			return sanitizeRecord(value, depth);
		}

		return compactValue(value);
	}

	private Object sanitizeRecord(Object value, int depth) {
		Map<String, Object> sanitizedRecord = new LinkedHashMap<>();
		for (RecordComponent component : value.getClass().getRecordComponents()) {
			String fieldName = component.getName();
			try {
				sanitizedRecord.put(fieldName,
						isSensitiveKey(fieldName)
								? "[REDACTADO]"
								: sanitize(component.getAccessor().invoke(value), depth + 1));
			} catch (ReflectiveOperationException exception) {
				sanitizedRecord.put(fieldName, "[NO_DISPONIBLE]");
			}
		}
		return sanitizedRecord;
	}

	private boolean isIgnoredFrameworkType(Object value) {
		return value instanceof ServletRequest || value instanceof ServletResponse || value instanceof MultipartFile
				|| value instanceof BindingResult || value instanceof Authentication || value instanceof Principal;
	}

	private boolean isSimpleValue(Object value) {
		return value instanceof CharSequence || value instanceof Number || value instanceof Boolean
				|| value instanceof Enum<?> || value instanceof TemporalAccessor || value instanceof java.util.UUID;
	}

	private boolean isSensitiveKey(String key) {
		String normalized = key.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
		return SENSITIVE_KEYS.stream()
				.map(candidate -> candidate.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT))
				.anyMatch(normalized::contains);
	}

	private String compactValue(Object value) {
		return truncate(sanitizeText(String.valueOf(value)));
	}

	private String sanitizeText(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		if (trimmed.matches("(?i)^sha256=[0-9a-f]{16,}$")) {
			return "sha256=[REDACTADO]";
		}
		if (trimmed.matches("^\\+?56\\d{8,10}$")) {
			return "[TELEFONO_REDACTADO]";
		}

		String sanitized = value
				.replaceAll("(?i)data:image/[^;\\s]+;base64,[A-Za-z0-9+/=]+",
						"data:image/[REDACTADO];base64,[REDACTADO]")
				.replaceAll("(?i)(sha256=)[0-9a-f]{16,}", "$1[REDACTADO]")
				.replaceAll("(?i)([?&](?:token|reset_token|resetToken|access_token|refresh_token)=)[^\\s,\\]})]+",
						"$1[REDACTADO]")
				.replaceAll("(?i)([A-Z0-9._%+-]{1,2})[A-Z0-9._%+-]*(@[A-Z0-9.-]+\\.[A-Z]{2,})", "$1***$2")
				.replaceAll("(?i)(\\\"(?:qrCode|qr_code)\\\"\\s*:\\s*\\\")(?:\\\\\\.|[^\\\"\\\\])*(\\\")",
						"$1[REDACTADO]$2")
				.replaceAll("(?i)(\\\"(?:phoneNumber|phone_number)\\\"\\s*:\\s*\\\")\\+?\\d{6,20}(\\\")",
						"$1[REDACTADO]$2")
				.replaceAll(
						"(?i)(\\\"(?:body|messageBody|rawText|responseText|originalText|lastInbound|authorization|access_token|refresh_token|token|secret|password|signature)\\\"\\s*:\\s*\\\")(?:\\\\\\.|[^\\\"\\\\])*(\\\")",
						"$1[REDACTADO]$2");
		return sanitized;
	}

	private String truncate(String value) {
		if (value == null) {
			return null;
		}
		int maxLength = Math.max(120, traceProperties.getMaxPayloadLength());
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength) + "...[TRUNCADO]";
	}
}
