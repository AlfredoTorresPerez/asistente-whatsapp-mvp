package com.asistentewhatsapp.shared.observability.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class AiProviderHealthIndicatorTest {

	@Test
	void reportsUpWithoutCalls() {
		AiProviderHealthIndicator indicator = new AiProviderHealthIndicator(new AiProviderStatusRegistry());

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("estado", "sin_llamadas");
	}

	@Test
	void reportsUpWhenLastCallSucceeded() {
		AiProviderStatusRegistry registry = new AiProviderStatusRegistry();
		registry.markSuccess("openai");
		AiProviderHealthIndicator indicator = new AiProviderHealthIndicator(registry);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("proveedor", "openai").containsEntry("ultimoExito",
				registry.getLastSuccessAt().toString());
	}

	@Test
	void reportsDownWhenLastCallFailed() {
		AiProviderStatusRegistry registry = new AiProviderStatusRegistry();
		registry.markFailure("openai", "TIMEOUT");
		AiProviderHealthIndicator indicator = new AiProviderHealthIndicator(registry);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("tipoError", "TIMEOUT").containsEntry("ultimoFallo",
				registry.getLastFailureAt().toString());
	}

	@Test
	void reportsDownAfterFailureWithoutPriorSuccess() {
		AiProviderStatusRegistry registry = new AiProviderStatusRegistry();
		registry.markFailure("openai", "CONNECTION_REFUSED");
		AiProviderHealthIndicator indicator = new AiProviderHealthIndicator(registry);

		assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	void registryTracksLastTimestamps() {
		AiProviderStatusRegistry registry = new AiProviderStatusRegistry();
		Instant before = Instant.now().minusSeconds(1);

		registry.markSuccess("openai");

		assertThat(registry.getProvider()).isEqualTo("openai");
		assertThat(registry.getLastSuccessAt()).isAfter(before);
		assertThat(registry.getLastFailureAt()).isNull();
		assertThat(registry.getLastErrorType()).isNull();
	}
}
