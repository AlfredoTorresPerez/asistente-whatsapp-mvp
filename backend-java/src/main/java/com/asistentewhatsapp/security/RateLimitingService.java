package com.asistentewhatsapp.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitingService {

	private final Map<String, RateLimitEntry> buckets = new ConcurrentHashMap<>();

	public static final int LOGIN_MAX = 5;
	public static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);

	public static final int PASSWORD_RESET_MAX = 3;
	public static final Duration PASSWORD_RESET_WINDOW = Duration.ofHours(1);

	public static final int API_MAX = 60;
	public static final Duration API_WINDOW = Duration.ofSeconds(10);

	public static final int WEBHOOK_MAX = 30;
	public static final Duration WEBHOOK_WINDOW = Duration.ofSeconds(10);

	public enum LimitType {
		LOGIN(LOGIN_MAX, LOGIN_WINDOW), PASSWORD_RESET(PASSWORD_RESET_MAX, PASSWORD_RESET_WINDOW), API(API_MAX,
				API_WINDOW), WEBHOOK(WEBHOOK_MAX, WEBHOOK_WINDOW);

		final int maxAttempts;
		final Duration window;

		LimitType(int maxAttempts, Duration window) {
			this.maxAttempts = maxAttempts;
			this.window = window;
		}
	}

	public boolean tryConsume(String key, LimitType type) {
		String cacheKey = type.name() + ":" + key;
		Instant now = Instant.now();
		RateLimitEntry entry = buckets.compute(cacheKey, (k, existing) -> {
			if (existing == null || now.isAfter(existing.windowEndsAt)) {
				return new RateLimitEntry(1, now.plus(type.window));
			}
			return new RateLimitEntry(existing.count + 1, existing.windowEndsAt);
		});
		return entry.count <= type.maxAttempts;
	}

	public long remainingAttempts(String key, LimitType type) {
		String cacheKey = type.name() + ":" + key;
		RateLimitEntry entry = buckets.get(cacheKey);
		Instant now = Instant.now();
		if (entry == null || now.isAfter(entry.windowEndsAt)) {
			return type.maxAttempts;
		}
		return Math.max(0, type.maxAttempts - entry.count);
	}

	public void cleanUp() {
		Instant now = Instant.now();
		buckets.entrySet().removeIf(e -> now.isAfter(e.getValue().windowEndsAt));
	}

	private record RateLimitEntry(int count, Instant windowEndsAt) {
	}
}
