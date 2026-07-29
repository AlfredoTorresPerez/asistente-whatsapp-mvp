package com.asistentewhatsapp.bookings.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class BookingConfirmationJdbcRepositoryTest {

	@Test
	void updateBookingStatusDoesNotWriteConfirmedAtOnBooking() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		BookingConfirmationJdbcRepository repository = new BookingConfirmationJdbcRepository(jdbcTemplate);

		repository.updateBookingStatus(UUID.randomUUID(), UUID.randomUUID(), "CONFIRMED");

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("update booking").contains("set status = :status")
				.contains("version = version + 1").contains("status in (:activeStatuses)")
				.contains("updated_at = current_timestamp").doesNotContain("confirmed_at")
				.doesNotContain("confirmation_token");
	}
}
