package com.asistentewhatsapp.reports.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.reports.api.ReportsKpiItem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ReportsJdbcRepositoryTest {

	private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
	private final ReportsJdbcRepository repository = new ReportsJdbcRepository(jdbcTemplate);

	@Test
	void responseRateDoesNotInventVariationWhenPreviousPeriodIsZero() {
		when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Double.class)))
				.thenReturn(35.0, 0.0);

		ReportsKpiItem kpi = repository.buildResponseRateKpi(UUID.randomUUID(), null, null, null, null, null,
				OffsetDateTime.parse("2026-08-01T00:00:00Z"), OffsetDateTime.parse("2026-08-02T00:00:00Z"),
				OffsetDateTime.parse("2026-07-30T00:00:00Z"), OffsetDateTime.parse("2026-07-31T00:00:00Z"));

		assertThat(kpi.variationPercent()).isNull();
		assertThat(kpi.valueType()).isEqualTo("PERCENT");
	}

	@Test
	void appointmentPerformanceUsesAppointmentPeriodInsteadOfCreationDate() {
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of());

		repository.loadAppointmentPerformance(UUID.randomUUID(), null, null, null, null, null,
				OffsetDateTime.parse("2026-08-01T00:00:00Z"), OffsetDateTime.parse("2026-08-02T00:00:00Z"));

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
		assertThat(sql.getValue()).contains("b.starts_at between :from and :to");
		assertThat(sql.getValue()).doesNotContain("b.created_at between :from and :to");
	}
}
