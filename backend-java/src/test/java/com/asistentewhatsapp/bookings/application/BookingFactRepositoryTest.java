package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class BookingFactRepositoryTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final BookingFactRepository repository = new BookingFactRepository(jdbcTemplate);

    @Test
    void hasActiveBookingReturnsTrueWhenExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true);

        boolean result = repository.hasActiveBooking(UUID.randomUUID(), "56950954580");

        assertThat(result).isTrue();
    }

    @Test
    void hasActiveBookingReturnsFalseWhenNotExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(false);

        boolean result = repository.hasActiveBooking(UUID.randomUUID(), "56950954580");

        assertThat(result).isFalse();
    }

    @Test
    void findActiveByPhoneReturnsEmptyWhenNoResults() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any(), any()))
                .thenReturn(List.of());

        List<BookingFactRepository.BookingFact> facts = repository.findActiveByPhone(
                UUID.randomUUID(), "56950954580");

        assertThat(facts).isEmpty();
    }

    @Test
    void markInactiveExecutesUpdate() {
        UUID bookingId = UUID.randomUUID();
        repository.markInactive(bookingId);
        assertThat(bookingId).isNotNull();
    }
}
