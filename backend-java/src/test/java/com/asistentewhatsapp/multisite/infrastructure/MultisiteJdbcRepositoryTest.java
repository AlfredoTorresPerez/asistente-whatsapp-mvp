package com.asistentewhatsapp.multisite.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class MultisiteJdbcRepositoryTest {

	@Test
	void catalogAvailabilityFiltersOnlyServices() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(java.util.Collections.emptyList());
		MultisiteJdbcRepository repository = new MultisiteJdbcRepository(jdbcTemplate);

		repository.catalogAvailability(UUID.randomUUID(), null);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
		assertThat(sql.getValue()).contains("ps.type = 'SERVICE'").doesNotContain("ps.type = 'PRODUCT'");
	}

	@Test
	void catalogAvailabilityFiltersByLocationWhenLocationIdProvided() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(java.util.Collections.emptyList());
		MultisiteJdbcRepository repository = new MultisiteJdbcRepository(jdbcTemplate);

		UUID businessId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		repository.catalogAvailability(businessId, locationId);

		ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
		verify(jdbcTemplate).query(anyString(), params.capture(), any(RowMapper.class));
		assertThat(params.getValue().getValue("businessId")).isEqualTo(businessId);
		assertThat(params.getValue().getValue("locationId")).isEqualTo(locationId);
	}

	@Test
	void catalogAvailabilityDoesNotIncludeProducts() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(java.util.Collections.emptyList());
		MultisiteJdbcRepository repository = new MultisiteJdbcRepository(jdbcTemplate);

		repository.catalogAvailability(UUID.randomUUID(), UUID.randomUUID());

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
		assertThat(sql.getValue()).contains("ps.type = 'SERVICE'").doesNotContain("PRODUCT");
	}
}
