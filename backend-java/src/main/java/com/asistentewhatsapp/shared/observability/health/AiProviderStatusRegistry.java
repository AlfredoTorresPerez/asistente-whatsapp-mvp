package com.asistentewhatsapp.shared.observability.health;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class AiProviderStatusRegistry {

	private final AtomicReference<Instant> lastSuccessAt = new AtomicReference<>();
	private final AtomicReference<Instant> lastFailureAt = new AtomicReference<>();
	private final AtomicReference<String> lastErrorType = new AtomicReference<>();
	private volatile String provider;

	public void markSuccess(String provider) {
		this.provider = provider;
		lastSuccessAt.set(Instant.now());
	}

	public void markFailure(String provider, String errorType) {
		this.provider = provider;
		lastFailureAt.set(Instant.now());
		lastErrorType.set(errorType);
	}

	public Instant getLastSuccessAt() {
		return lastSuccessAt.get();
	}

	public Instant getLastFailureAt() {
		return lastFailureAt.get();
	}

	public String getLastErrorType() {
		return lastErrorType.get();
	}

	public String getProvider() {
		return provider;
	}
}
