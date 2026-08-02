package com.asistentewhatsapp.shared.observability.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScheduledTaskRunRegistryTest {

	@Test
	void tracksSuccessAndFailurePerTask() {
		ScheduledTaskRunRegistry registry = new ScheduledTaskRunRegistry();

		assertThat(registry.lastRun("tarea")).isNull();
		assertThat(registry.lastSuccessful("tarea")).isTrue();

		registry.markSuccess("tarea");
		assertThat(registry.lastRun("tarea")).isNotNull();
		assertThat(registry.lastSuccessful("tarea")).isTrue();

		registry.markFailure("tarea");
		assertThat(registry.lastSuccessful("tarea")).isFalse();
		assertThat(registry.allRuns()).hasSize(1);
	}

	@Test
	void keepsRunsOfDifferentTasksIndependent() {
		ScheduledTaskRunRegistry registry = new ScheduledTaskRunRegistry();

		registry.markFailure("tarea-a");
		registry.markSuccess("tarea-b");

		assertThat(registry.lastSuccessful("tarea-a")).isFalse();
		assertThat(registry.lastSuccessful("tarea-b")).isTrue();
		assertThat(registry.allRuns()).hasSize(2);
	}
}
