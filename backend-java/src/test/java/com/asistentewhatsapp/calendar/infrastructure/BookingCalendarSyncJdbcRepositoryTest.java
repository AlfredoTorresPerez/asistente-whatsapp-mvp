package com.asistentewhatsapp.calendar.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class BookingCalendarSyncJdbcRepositoryTest {

	private static final UUID BOOKING_ID = UUID.randomUUID();
	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final UUID SYNC_ID = UUID.randomUUID();
	private static final String PROVIDER = "GOOGLE";

	private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
	private final BookingCalendarSyncJdbcRepository repository = new BookingCalendarSyncJdbcRepository(jdbcTemplate);

	@Test
	void insertCreatesSyncRecordWithUpsert() {
		BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord record = record("PENDING", "CREATE", null, 0);
		repository.insert(record);
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("insert into booking_calendar_sync")
				.contains("on conflict (booking_id, provider) do update");
	}

	@Test
	void updateSyncSuccessSetsStatusToSynced() {
		repository.updateSyncSuccess(SYNC_ID, "ext_event_123");
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("sync_status = 'SYNCED'").contains("retry_count = 0")
				.contains("last_successful_sync_at = :now");
	}

	@Test
	void updateSyncFailedIncrementsRetryCount() {
		repository.updateSyncFailed(SYNC_ID, "Error de red");
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("sync_status = 'FAILED'").contains("retry_count = retry_count + 1");
	}

	@Test
	void findByBookingReturnsResults() {
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of(record("SYNCED", "CREATE", "ext_1", 0)));
		List<BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord> results = repository
				.findByBooking(BOOKING_ID);
		assertThat(results).hasSize(1);
		assertThat(results.getFirst().syncStatus()).isEqualTo("SYNCED");
		assertThat(results.getFirst().externalEventId()).isEqualTo("ext_1");
	}

	@Test
	void findFailedSyncsFiltersByMaxRetriesAndBefore() {
		OffsetDateTime before = OffsetDateTime.now();
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of(record("FAILED", "CREATE", null, 2)));
		List<BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord> results = repository.findFailedSyncs(5,
				before);
		assertThat(results).hasSize(1);
		assertThat(results.getFirst().syncStatus()).isEqualTo("FAILED");
		assertThat(results.getFirst().retryCount()).isEqualTo(2);
	}

	@Test
	void findByBookingAndProviderOptionalReturnsEmptyWhenNotFound() {
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of());
		var result = repository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER);
		assertThat(result).isEmpty();
	}

	@Test
	void findByBookingAndProviderOptionalReturnsRecordWhenFound() {
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
				.thenReturn(List.of(record("PENDING", "CREATE", null, 0)));
		var result = repository.findByBookingAndProviderOptional(BOOKING_ID, PROVIDER);
		assertThat(result).isPresent();
		assertThat(result.get().provider()).isEqualTo(PROVIDER);
	}

	@Test
	void updateSyncFailedTruncatesLongErrorMessage() {
		String longError = "a".repeat(1000);
		repository.updateSyncFailed(SYNC_ID, longError);
		ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
		verify(jdbcTemplate).update(anyString(), params.capture());
		String capturedError = (String) params.getValue().getValue("errorMessage");
		assertThat(capturedError).hasSizeLessThanOrEqualTo(500);
	}

	private BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord record(String status, String action,
			String externalId, int retryCount) {
		return new BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord(SYNC_ID, BOOKING_ID, BUSINESS_ID,
				PROVIDER, externalId, status, action, null, retryCount, OffsetDateTime.now(), null,
				OffsetDateTime.now(), OffsetDateTime.now());
	}
}
