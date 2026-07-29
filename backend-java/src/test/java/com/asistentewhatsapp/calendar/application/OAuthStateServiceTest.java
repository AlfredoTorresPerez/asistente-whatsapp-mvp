package com.asistentewhatsapp.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@DisplayName("OAuthStateService - Gestión de estados OAuth con Mockito")
class OAuthStateServiceTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final String PROVIDER = "GOOGLE";

	private NamedParameterJdbcTemplate jdbcTemplate;
	private OAuthStateService service;

	@BeforeEach
	void setUp() {
		jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		service = new OAuthStateService(jdbcTemplate);
	}

	@Test
	@DisplayName("generateState retorna un string no nulo y no vacío")
	void generateStateReturnsNonNullNonEmpty() {
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		String state = service.generateState(BUSINESS_ID, PROVIDER);
		assertThat(state).isNotNull().isNotEmpty();
	}

	@Test
	@DisplayName("generateState calcula SHA-256 y guarda en BD")
	void generateStateStoresHashInDb() {
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		String state = service.generateState(BUSINESS_ID, PROVIDER);
		assertThat(state).hasSize(43);
		verify(jdbcTemplate).update(anyString(), any(MapSqlParameterSource.class));
	}

	@Test
	@DisplayName("consumeAndValidate con estado nulo lanza IllegalArgumentException")
	void consumeNullStateThrows() {
		assertThatThrownBy(() -> service.consumeAndValidate(null, BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing");
	}

	@Test
	@DisplayName("consumeAndValidate con estado vacío lanza IllegalArgumentException")
	void consumeBlankStateThrows() {
		assertThatThrownBy(() -> service.consumeAndValidate("", BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing");
	}

	@Test
	@DisplayName("consumeAndValidate con estado no encontrado lanza IllegalArgumentException")
	void consumeNonExistentStateThrows() {
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of());
		assertThatThrownBy(() -> service.consumeAndValidate("unknown-state", BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not found");
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> buildRowList(Object... rows) {
		return (List<T>) List.of(rows);
	}

	private Object createRow(UUID businessId, String provider, String redirectUri, boolean consumed,
			OffsetDateTime expiresAt) {
		try {
			Class<?> rowClass = Class
					.forName("com.asistentewhatsapp.calendar.application.OAuthStateService$OAuthStateRow");
			Constructor<?> constructor = rowClass.getDeclaredConstructor(UUID.class, String.class, String.class,
					boolean.class, OffsetDateTime.class);
			constructor.setAccessible(true);
			return constructor.newInstance(businessId, provider, redirectUri, consumed, expiresAt);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create OAuthStateRow via reflection", e);
		}
	}

	@Test
	@DisplayName("consumeAndValidate con estado válido retorna OAuthStateInfo")
	void consumeValidStateReturnsInfo() {
		OffsetDateTime future = OffsetDateTime.now().plusHours(1);
		Object row = createRow(BUSINESS_ID, PROVIDER, null, false, future);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(buildRowList(row));
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

		OAuthStateService.OAuthStateInfo info = service.consumeAndValidate("test-state", BUSINESS_ID, PROVIDER);
		assertThat(info.businessId()).isEqualTo(BUSINESS_ID);
		assertThat(info.provider()).isEqualTo(PROVIDER);
	}

	@Test
	@DisplayName("consumeAndValidate con businessId incorrecto lanza IllegalArgumentException")
	void consumeWithWrongBusinessIdThrows() {
		UUID otherBusiness = UUID.randomUUID();
		OffsetDateTime future = OffsetDateTime.now().plusHours(1);
		Object row = createRow(otherBusiness, PROVIDER, null, false, future);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(buildRowList(row));
		assertThatThrownBy(() -> service.consumeAndValidate("state", BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("business_id mismatch");
	}

	@Test
	@DisplayName("consumeAndValidate con provider incorrecto lanza IllegalArgumentException")
	void consumeWithWrongProviderThrows() {
		OffsetDateTime future = OffsetDateTime.now().plusHours(1);
		Object row = createRow(BUSINESS_ID, "OUTLOOK", null, false, future);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(buildRowList(row));
		assertThatThrownBy(() -> service.consumeAndValidate("state", BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("provider mismatch");
	}

	@Test
	@DisplayName("consumeAndValidate con estado expirado lanza IllegalArgumentException")
	void consumeExpiredStateThrows() {
		OffsetDateTime past = OffsetDateTime.now().minusHours(1);
		Object row = createRow(BUSINESS_ID, PROVIDER, null, false, past);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(buildRowList(row));
		assertThatThrownBy(() -> service.consumeAndValidate("state", BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expired");
	}

	@Test
	@DisplayName("consumeAndValidate con estado ya consumido lanza IllegalArgumentException")
	void consumeAlreadyConsumedThrows() {
		OffsetDateTime future = OffsetDateTime.now().plusHours(1);
		Object row = createRow(BUSINESS_ID, PROVIDER, null, true, future);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(buildRowList(row));
		assertThatThrownBy(() -> service.consumeAndValidate("state", BUSINESS_ID, PROVIDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already been consumed");
	}

	@Test
	@DisplayName("cleanupExpired ejecuta DELETE en la BD")
	void cleanupExpiredDeletesFromDb() {
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(5);
		service.cleanupExpired();
		verify(jdbcTemplate).update(eq("delete from oauth_state where expires_at < :now"),
				any(MapSqlParameterSource.class));
	}
}
