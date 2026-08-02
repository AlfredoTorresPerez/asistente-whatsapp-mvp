package com.asistentewhatsapp.shared.observability.health;

import java.util.Comparator;
import java.util.Optional;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasksHealthIndicator implements HealthIndicator {

	private final ScheduledTaskRunRegistry registry;

	public ScheduledTasksHealthIndicator(ScheduledTaskRunRegistry registry) {
		this.registry = registry;
	}

	@Override
	public Health health() {
		if (!hasAnyRun()) {
			return Health.up().withDetail("tareas", "sin_ejecuciones").build();
		}
		Optional<ScheduledTaskRunRegistry.TaskRun> latest = latestRun();
		boolean healthy = latest.map(ScheduledTaskRunRegistry.TaskRun::successful).orElse(true);
		Health.Builder builder = healthy ? Health.up() : Health.down();
		latest.ifPresent(run -> builder.withDetail("ultimaEjecucion", run.timestamp().toString())
				.withDetail("resultado", run.successful() ? "exitosa" : "fallida"));
		return builder.build();
	}

	private boolean hasAnyRun() {
		return latestRun().isPresent();
	}

	private Optional<ScheduledTaskRunRegistry.TaskRun> latestRun() {
		return registry.allRuns().stream().max(Comparator.comparing(ScheduledTaskRunRegistry.TaskRun::timestamp));
	}
}
