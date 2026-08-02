package com.asistentewhatsapp.shared.observability.health;

import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AiProviderHealthIndicator implements HealthIndicator {

	private final AiProviderStatusRegistry statusRegistry;

	public AiProviderHealthIndicator(AiProviderStatusRegistry statusRegistry) {
		this.statusRegistry = statusRegistry;
	}

	@Override
	public Health health() {
		Instant lastSuccess = statusRegistry.getLastSuccessAt();
		Instant lastFailure = statusRegistry.getLastFailureAt();
		if (lastSuccess == null && lastFailure == null) {
			return Health.up().withDetail("estado", "sin_llamadas").build();
		}
		boolean failing = lastFailure != null && (lastSuccess == null || lastFailure.isAfter(lastSuccess));
		Health.Builder builder = failing ? Health.down() : Health.up();
		builder.withDetail("proveedor",
				statusRegistry.getProvider() == null ? "no_configurado" : statusRegistry.getProvider());
		if (lastSuccess != null) {
			builder.withDetail("ultimoExito", lastSuccess.toString());
		}
		if (lastFailure != null) {
			builder.withDetail("ultimoFallo", lastFailure.toString()).withDetail("tipoError",
					statusRegistry.getLastErrorType());
		}
		return builder.build();
	}
}
