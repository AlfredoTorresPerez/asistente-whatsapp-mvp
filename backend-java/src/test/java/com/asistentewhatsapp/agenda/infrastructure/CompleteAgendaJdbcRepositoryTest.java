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
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
				ArgumentMatchers.<RowMapper<AgendaCalendarItemResponse>>any())).thenReturn(List.of());
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		repository.findCalendar(UUID.randomUUID(), OffsetDateTime.of(2026, 6, 18, 0, 0, 0, 0, ZoneOffset.UTC),
				OffsetDateTime.of(2026, 6, 19, 0, 0, 0, 0, ZoneOffset.UTC), null, null, null, null, null);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class),
				ArgumentMatchers.<RowMapper<AgendaCalendarItemResponse>>any());
		assertThat(sql.getValue()).contains("b.status in (:activeStatuses)").doesNotContain("limit 1")
				.doesNotContain("distinct on").doesNotContain("group by");
	}

	@Test
	void updateBookingScheduleUsesVersionAndActiveStatusGuard() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class))).thenReturn(List.of(UUID.randomUUID()),
						List.of(false), List.of(UUID.randomUUID()), List.of(UUID.randomUUID()), List.of(false),
						List.of("CONFIRMED"));
		when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(Class.class)))
				.thenReturn(0);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		repository.updateBookingSchedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				OffsetDateTime.of(2026, 6, 22, 14, 0, 0, 0, ZoneOffset.UTC),
				OffsetDateTime.of(2026, 6, 22, 15, 0, 0, 0, ZoneOffset.UTC), 60, "Cliente pide cambio", "ADMIN");

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getAllValues()).anySatisfy(
				statement -> assertThat(statement).contains("update booking").contains("status = 'REPROGRAMADA'")
						.contains("version = version + 1").contains("status in (:mutableStatuses)"));
	}

	@Test
	void hasConflictUsesProfessionalTravelTimeBetweenLocations() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(Class.class))).thenReturn(0,
				1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		boolean result = repository.hasConflict(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(), null,
				OffsetDateTime.of(2026, 6, 22, 14, 0, 0, 0, ZoneOffset.UTC),
				OffsetDateTime.of(2026, 6, 22, 15, 0, 0, 0, ZoneOffset.UTC));

		assertThat(result).isTrue();
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate, atLeastOnce()).queryForObject(sql.capture(), any(MapSqlParameterSource.class),
				any(Class.class));
		assertThat(sql.getAllValues())
				.anySatisfy(statement -> assertThat(statement).contains("business_location_travel_time")
						.contains("t.travel_minutes").contains("b.location_id <> :locationId"));
	}

	@Test
	void hasConflictUsesRoomCapacityInsteadOfBinaryRoomOverlap() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class))).thenReturn(List.of(false), List.of(true));
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		boolean result = repository.hasConflict(UUID.randomUUID(), null, UUID.randomUUID(), null, UUID.randomUUID(),
				OffsetDateTime.of(2026, 6, 22, 14, 0, 0, 0, ZoneOffset.UTC),
				OffsetDateTime.of(2026, 6, 22, 15, 0, 0, 0, ZoneOffset.UTC));

		assertThat(result).isTrue();
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate, atLeastOnce()).query(sql.capture(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class));
		assertThat(sql.getAllValues())
				.anySatisfy(statement -> assertThat(statement).contains("count(b.id) >= r.capacity")
						.contains("from agenda_room r").contains("left join booking b").contains("b.room_id = r.id"));
	}

	@Test
	void hasConflictUsesLocationDailyCapacityBeforeSlotResources() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class))).thenReturn(List.of(true));
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		boolean result = repository.hasConflict(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), OffsetDateTime.of(2026, 6, 22, 14, 0, 0, 0, ZoneOffset.UTC),
				OffsetDateTime.of(2026, 6, 22, 15, 0, 0, 0, ZoneOffset.UTC));

		assertThat(result).isTrue();
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class));
		assertThat(sql.getValue()).contains("business_location bl").contains("daily_booking_capacity")
				.contains("cast(:startsAt as timestamptz)");
	}

	@Test
	void findProfessionalCandidatesAppliesQualificationCertificationAndDailyRotation() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class))).thenReturn(List.of());
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		repository.findProfessionalCandidates(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class));
		assertThat(sql.getValue()).contains("join aesthetic_service s")
				.contains("coalesce(p.qualification_level, 0) >= coalesce(s.required_professional_level, 0)")
				.contains("s.requires_professional_certification = false")
				.contains("p.certification_valid_until >= current_date").contains("order by (")
				.contains("b.starts_at::date = current_date");
	}

	@Test
	void recordSlotDiscardUsesIdempotentTraceKey() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);
		OffsetDateTime startsAt = OffsetDateTime.of(2026, 6, 22, 14, 0, 0, 0, ZoneOffset.UTC);

		repository.recordSlotDiscard(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), startsAt, startsAt.plusHours(1), startsAt.minusMinutes(10), startsAt.plusHours(1),
				"CONFLICT", "TEST_SOURCE");

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("insert into agenda_slot_discard_trace").contains("trace_key")
				.contains("rule_input").contains("result").contains("evaluation_ms")
				.contains("on conflict (trace_key) do update").contains("occurrence_count");
	}

	@Test
	void reserveBookingOperationIdempotencyUsesDedicatedUniqueKey() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		boolean reserved = repository.reserveBookingOperationIdempotency(UUID.randomUUID(), "TEMPORARY_BOOKING_CREATE",
				"idem-key", "request-hash", "AGENDA");

		assertThat(reserved).isTrue();
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("insert into agenda_booking_operation_idempotency")
				.contains("operation_type").contains("idempotency_key").contains("request_hash")
				.contains("on conflict (business_id, operation_type, idempotency_key) do nothing");
	}

	@Test
	void completeBookingOperationIdempotencyPersistsBookingResult() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		repository.completeBookingOperationIdempotency(UUID.randomUUID(), "PUBLIC_BOOKING_CREATE", "idem-key",
				"request-hash", UUID.randomUUID());

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("update agenda_booking_operation_idempotency")
				.contains("status = 'COMPLETED'").contains("booking_id = :bookingId")
				.contains("result = jsonb_build_object").contains("completed_at = current_timestamp");
	}

	@Test
	void recordBookingConsentPersistsAcceptedConsentAndGuardianData() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		repository.recordBookingConsent(UUID.randomUUID(), UUID.randomUUID(), true, true,
				java.time.LocalDate.of(2010, 1, 10), "Tutor Legal", "56922223333");

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getValue()).contains("update booking").contains("requires_informed_consent")
				.contains("informed_consent_accepted_at").contains("customer_birth_date").contains("guardian_name")
				.contains("guardian_phone");
	}

	@Test
	void cancelBookingUsesVersionAndActiveStatusGuard() {
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class),
				any(org.springframework.jdbc.core.RowMapper.class))).thenReturn(List.of("CONFIRMED"));
		when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
		CompleteAgendaJdbcRepository repository = new CompleteAgendaJdbcRepository(jdbcTemplate);

		repository.cancelBooking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Cliente cancela", "ADMIN");

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(MapSqlParameterSource.class));
		assertThat(sql.getAllValues()).anySatisfy(
				statement -> assertThat(statement).contains("update booking").contains("status = :targetStatus")
						.contains("version = version + 1").contains("status in (:mutableStatuses)"));
	}
}
