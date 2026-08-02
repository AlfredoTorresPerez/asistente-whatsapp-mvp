package com.asistentewhatsapp.shared.observability.health;

import com.asistentewhatsapp.aiagents.application.AiReplyOutboxProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OutboxHealthIndicator implements HealthIndicator {

	private final AiReplyOutboxProcessor outboxProcessor;
	private final long maxPending;
	private final long maxAgeMinutes;

	public OutboxHealthIndicator(AiReplyOutboxProcessor outboxProcessor,
			@Value("${app.observability.outbox-health.max-pending:50}") long maxPending,
			@Value("${app.observability.outbox-health.max-age-minutes:15}") long maxAgeMinutes) {
		this.outboxProcessor = outboxProcessor;
		this.maxPending = maxPending;
		this.maxAgeMinutes = maxAgeMinutes;
	}

	@Override
	public Health health() {
		AiReplyOutboxProcessor.AiOutboxStats stats = outboxProcessor.getStats();
		long pending = stats.pending() + stats.processing();
		long oldestAgeSeconds = stats.oldestAgeSeconds();
		long oldestAgeMinutes = oldestAgeSeconds / 60;

		Health.Builder builder = pending <= maxPending && oldestAgeMinutes <= maxAgeMinutes
				? Health.up()
				: Health.down();
		return builder.withDetail("pendientes", pending).withDetail("procesando", stats.processing())
				.withDetail("fallidas", stats.failed()).withDetail("antiguedadMaximaMinutos", oldestAgeMinutes)
				.withDetail("umbralPendientes", maxPending).withDetail("umbralAntiguedadMinutos", maxAgeMinutes)
				.build();
	}
}
