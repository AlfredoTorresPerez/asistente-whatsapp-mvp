package com.asistentewhatsapp.shared.api;

import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/observability", produces = MediaType.APPLICATION_JSON_VALUE)
public class ObservabilityClientErrorsController {

	private static final Logger CLIENT_ERROR_LOGGER = LoggerFactory.getLogger("APP_CLIENT_ERROR");

	private static final int MAX_MESSAGE_LENGTH = 500;
	private static final int MAX_STACK_LENGTH = 8000;
	private static final int MAX_URL_LENGTH = 500;
	private static final int MAX_COMPONENT_LENGTH = 120;
	private static final int MAX_ERROR_TYPE_LENGTH = 80;
	private static final int MAX_PER_MINUTE_PER_CLIENT = 20;

	private final List<String> allowedOrigins;
	private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

	public ObservabilityClientErrorsController(
			@Value("${app.security.cors-allowed-origins:}") String corsAllowedOrigins) {
		this.allowedOrigins = parseAllowedOrigins(corsAllowedOrigins);
	}

	@PostMapping("/client-errors")
	public ResponseEntity<StatusResponse> reportClientError(@RequestBody(required = false) ClientErrorReport request,
			@RequestHeader(value = "Origin", required = false) String origin,
			@RequestHeader(value = "Referer", required = false) String referer,
			@RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
		if (request == null) {
			return ResponseEntity.badRequest().body(new StatusResponse("INVALID_PAYLOAD"));
		}
		if (!isAllowedSource(origin, referer)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new StatusResponse("SOURCE_NOT_ALLOWED"));
		}
		String clientKey = resolveClientKey(forwardedFor);
		if (!tryConsume(clientKey)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new StatusResponse("RATE_LIMITED"));
		}
		String message = truncate(request.message(), MAX_MESSAGE_LENGTH);
		String stack = truncate(request.stack(), MAX_STACK_LENGTH);
		String url = truncate(request.url(), MAX_URL_LENGTH);
		String component = truncate(request.component(), MAX_COMPONENT_LENGTH);
		String errorType = truncate(request.errorType(), MAX_ERROR_TYPE_LENGTH);
		if (message == null || message.isBlank()) {
			return ResponseEntity.badRequest().body(new StatusResponse("INVALID_PAYLOAD"));
		}

		CLIENT_ERROR_LOGGER.warn("[Frontend - error de cliente] tipoError={} componente={} url={} mensaje={}",
				errorType == null ? "desconocido" : errorType, component == null ? "desconocido" : component,
				url == null ? "desconocida" : url, LogSanitizer.clientErrorSummary(message, stack));
		return ResponseEntity.accepted().body(new StatusResponse("ACCEPTED"));
	}

	private boolean isAllowedSource(String origin, String referer) {
		String candidate = origin != null && !origin.isBlank() ? origin : referer;
		if (candidate == null || candidate.isBlank()) {
			return false;
		}
		if (allowedOrigins.isEmpty()) {
			return true;
		}
		String normalized = candidate.trim();
		return allowedOrigins.stream().anyMatch(normalized::startsWith);
	}

	private String resolveClientKey(String forwardedFor) {
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return "local";
	}

	private boolean tryConsume(String clientKey) {
		Instant now = Instant.now();
		BucketEntry entry = buckets.compute(clientKey, (key, existing) -> {
			if (existing == null || now.isAfter(existing.windowEndsAt)) {
				return new BucketEntry(1, now.plus(Duration.ofMinutes(1)));
			}
			return new BucketEntry(existing.count + 1, existing.windowEndsAt);
		});
		return entry.count <= MAX_PER_MINUTE_PER_CLIENT;
	}

	private List<String> parseAllowedOrigins(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return List.of(value.split(",")).stream().map(String::trim).filter(s -> !s.isBlank()).toList();
	}

	private String truncate(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
	}

	public record ClientErrorReport(String message, String stack, String url, String component, String errorType) {
	}

	private record BucketEntry(int count, Instant windowEndsAt) {
	}
}
