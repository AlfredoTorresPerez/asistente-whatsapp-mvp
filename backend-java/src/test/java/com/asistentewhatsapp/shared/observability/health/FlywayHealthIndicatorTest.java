package com.asistentewhatsapp.shared.observability.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.DefaultApplicationArguments;

class FlywayHealthIndicatorTest {

	private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@Test
	void reportsUpWithAppliedMigrationCount() {
		Flyway flyway = mock(Flyway.class);
		MigrationInfoService infoService = mock(MigrationInfoService.class);
		when(flyway.info()).thenReturn(infoService);
		when(infoService.applied()).thenReturn(new MigrationInfo[4]);
		FlywayHealthIndicator indicator = new FlywayHealthIndicator(flyway, meterRegistry);

		indicator.run(new DefaultApplicationArguments());

		var health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("migracionesAplicadas", 4).containsEntry("estado", "correcto");
		assertThat(meterRegistry.get("assistente_flyway_estado").gauge().value()).isEqualTo(1.0);
	}

	@Test
	void reportsDownWhenFlywayInspectionFails() {
		Flyway flyway = mock(Flyway.class);
		MigrationInfoService infoService = mock(MigrationInfoService.class);
		when(flyway.info()).thenReturn(infoService);
		when(infoService.applied()).thenThrow(new IllegalStateException("db no disponible"));
		FlywayHealthIndicator indicator = new FlywayHealthIndicator(flyway, meterRegistry);

		indicator.run(new DefaultApplicationArguments());

		var health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("tipoError", "IllegalStateException");
		assertThat(meterRegistry.get("assistente_flyway_estado").gauge().value()).isEqualTo(0.0);
	}

	@Test
	void reportsUpBeforeFirstRunWithoutDetails() {
		Flyway flyway = mock(Flyway.class);
		FlywayHealthIndicator indicator = new FlywayHealthIndicator(flyway, meterRegistry);

		assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
	}
}
