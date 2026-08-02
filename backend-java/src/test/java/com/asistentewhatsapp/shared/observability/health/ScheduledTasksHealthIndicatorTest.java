package com.asistentewhatsapp.shared.observability.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class ScheduledTasksHealthIndicatorTest {

	@Test
	void reportsUpWithoutExecutions() {
		ScheduledTasksHealthIndicator indicator = new ScheduledTasksHealthIndicator(new ScheduledTaskRunRegistry());

		assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
		assertThat(indicator.health().getDetails()).containsEntry("tareas", "sin_ejecuciones");
	}

	@Test
	void reportsUpWhenLatestRunSucceeded() {
		ScheduledTaskRunRegistry registry = new ScheduledTaskRunRegistry();
		registry.markSuccess("tarea-a");
		registry.markSuccess("tarea-b");
		ScheduledTasksHealthIndicator indicator = new ScheduledTasksHealthIndicator(registry);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("resultado", "exitosa");
		assertThat(health.getDetails().get("ultimaEjecucion")).isInstanceOf(String.class);
	}

	@Test
	void reportsDownWhenLatestRunFailed() {
		ScheduledTaskRunRegistry registry = new ScheduledTaskRunRegistry();
		registry.markFailure("tarea-a");
		registry.markFailure("tarea-b");
		ScheduledTasksHealthIndicator indicator = new ScheduledTasksHealthIndicator(registry);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("resultado", "fallida");
	}
}
