package com.asistentewhatsapp.bookings.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.bookings.domain.BookingPolicyRecord;
import com.asistentewhatsapp.bookings.domain.PolicySnapshot;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class BusinessPolicyJdbcRepositoryTest {

	@Test
	void findActiveVersionIdQueriesWithCorrectSql() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		UUID expectedId = UUID.randomUUID();
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of(expectedId));
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		UUID businessId = UUID.randomUUID();
		OffsetDateTime at = OffsetDateTime.now();
		UUID result = repository.findActiveVersionId(businessId, at);

		assertThat(result).isEqualTo(expectedId);
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
		assertThat(sql.getValue()).contains("from business_policy_version").contains("effective_from <= :at")
				.contains("effective_until > :at").contains("order by version desc").contains("limit 1");
	}

	@Test
	void findActiveVersionIdReturnsNullWhenEmpty() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of());
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		UUID result = repository.findActiveVersionId(UUID.randomUUID(), OffsetDateTime.now());

		assertThat(result).isNull();
	}

	@Test
	void buildSnapshotQueriesWithVersionAndLocation() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of());
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		PolicySnapshot snapshot = repository.buildSnapshot(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(snapshot).isNotNull();
		assertThat(snapshot.cancellationWindowHours()).isNull();
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
		assertThat(sql.getValue()).contains("from business_policy").contains("bp.version_id = :versionId")
				.contains("bp.location_id is null or bp.location_id = :locationId");
	}

	@Test
	void buildSnapshotExtractsValuesFromPolicyRecords() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		UUID versionId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of());
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		PolicySnapshot snapshot = repository.buildSnapshot(UUID.randomUUID(), locationId, versionId);

		assertThat(snapshot).isNotNull();
		assertThat(snapshot.policyVersionId()).isEqualTo(versionId);
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
		assertThat(sql.getValue()).contains("from business_policy").contains("bp.version_id = :versionId")
				.contains("bp.location_id is null or bp.location_id = :locationId")
				.contains("order by bp.location_id nulls last, bp.priority asc");
	}

	@Test
	void buildSnapshotExtractsSlotStepMinutes() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		UUID versionId = UUID.randomUUID();
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of(new BookingPolicyRecord(UUID.randomUUID(), versionId, null, "SLOT_CONFIG",
						"default", "{\"slot_step_minutes\": 10}", 0, true)));
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		PolicySnapshot snapshot = repository.buildSnapshot(UUID.randomUUID(), UUID.randomUUID(), versionId);

		assertThat(snapshot.slotStepMinutes()).isEqualTo(10);
	}

	@Test
	void updateBookingPolicySetsVersionAndSnapshot() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		UUID bookingId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, 24, 12, 60, 60, 15, 15, 30, 3, "PERCENT",
				BigDecimal.TEN, null, "CLP", 15);
		repository.updateBookingPolicy(bookingId, versionId, snapshot);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("update booking").contains("policy_version_id = :policyVersionId")
				.contains("policy_snapshot = :snapshot::jsonb");
	}

	@Test
	void hasLocationOverrideReturnsTrueWhenCountPositive() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
				.thenReturn(1);
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		boolean result = repository.hasLocationOverride(UUID.randomUUID(), UUID.randomUUID(), "CANCELLATION");

		assertThat(result).isTrue();
	}

	@Test
	void hasLocationOverrideReturnsFalseWhenCountZero() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
				.thenReturn(0);
		BusinessPolicyJdbcRepository repository = new BusinessPolicyJdbcRepository(jdbcTemplate);

		boolean result = repository.hasLocationOverride(UUID.randomUUID(), UUID.randomUUID(), "CANCELLATION");

		assertThat(result).isFalse();
	}
}
