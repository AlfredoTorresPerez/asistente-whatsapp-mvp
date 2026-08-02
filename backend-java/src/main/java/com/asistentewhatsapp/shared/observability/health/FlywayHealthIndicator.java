package com.asistentewhatsapp.shared.observability.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FlywayHealthIndicator implements ApplicationRunner, HealthIndicator {

	private final org.flywaydb.core.Flyway flyway;
	private final AtomicLong gaugeValue = new AtomicLong(0);

	public FlywayHealthIndicator(org.flywaydb.core.Flyway flyway, MeterRegistry meterRegistry) {
		this.flyway = flyway;
		Gauge.builder("assistente_flyway_estado", gaugeValue, AtomicLong::get)
				.description("Estado de las migraciones Flyway (1 = aplicadas correctamente, 0 = fallo)")
				.register(meterRegistry);
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			int applied = flyway.info().applied().length;
			gaugeValue.set(1);
			lastAppliedCount = applied;
		} catch (RuntimeException exception) {
			gaugeValue.set(0);
			lastError = exception.getClass().getSimpleName();
		}
	}

	private volatile int lastAppliedCount;
	private volatile String lastError;

	@Override
	public Health health() {
		if (lastError != null) {
			return Health.down().withDetail("migraciones", "fallo_al_inspeccionar").withDetail("tipoError", lastError)
					.build();
		}
		return Health.up().withDetail("migracionesAplicadas", lastAppliedCount).withDetail("estado", "correcto")
				.build();
	}
}
