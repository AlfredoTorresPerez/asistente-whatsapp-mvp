package com.asistentewhatsapp.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.agenda.api.AgendaCalendarItemResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class CompleteAgendaJdbcRepositoryTest {

    @Test
    void findCalendarWithoutStatusReturnsActiveBookingsWithoutCollapsingByDay() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(
                anyString(),
                any(MapSqlParameterSource.class),
                ArgumentMatchers.<RowMapper<AgendaCalendarItemResponse>>any()))
                .thenReturn(List.of());
        CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

        repository.findCalendar(
                UUID.randomUUID(),
                OffsetDateTime.of(2026, 6, 18, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 6, 19, 0, 0, 0, 0, ZoneOffset.UTC),
                null,
                null,
                null,
                null,
                null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sql.capture(),
                any(MapSqlParameterSource.class),
                ArgumentMatchers.<RowMapper<AgendaCalendarItemResponse>>any());
        assertThat(sql.getValue())
                .contains("b.status in (:activeStatuses)")
                .doesNotContain("limit 1")
                .doesNotContain("distinct on")
                .doesNotContain("group by");
    }

    @Test
    void updateBookingScheduleUsesVersionAndActiveStatusGuard() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of("CONFIRMED"));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

        repository.updateBookingSchedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.of(2026, 6, 22, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 6, 22, 15, 0, 0, 0, ZoneOffset.UTC),
                60,
                "Cliente pide cambio",
                "ADMIN");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(MapSqlParameterSource.class));
        assertThat(sql.getAllValues())
                .anySatisfy(statement -> assertThat(statement)
                        .contains("update booking")
                        .contains("status = 'REPROGRAMADA'")
                        .contains("version = version + 1")
                        .contains("status in (:mutableStatuses)"));
    }

    @Test
    void cancelBookingUsesVersionAndActiveStatusGuard() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of("CONFIRMED"));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

        repository.cancelBooking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Cliente cancela", "ADMIN");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(MapSqlParameterSource.class));
        assertThat(sql.getAllValues())
                .anySatisfy(statement -> assertThat(statement)
                        .contains("update booking")
                        .contains("status = 'CANCELADA'")
                        .contains("version = version + 1")
                        .contains("status in (:mutableStatuses)"));
    }
}
