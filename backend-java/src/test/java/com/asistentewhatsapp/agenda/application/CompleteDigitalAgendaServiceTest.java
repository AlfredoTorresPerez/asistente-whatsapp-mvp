package com.asistentewhatsapp.agenda.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.agenda.api.CreateTemporaryAgendaBookingRequest;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.BookingOperationIdempotencyRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.LocationRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ProfessionalRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.RoomRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ServiceRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.TimeWindowRecord;
import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.bookings.application.AvailabilityService;
import com.asistentewhatsapp.bookings.application.BookingConfirmationService;
import com.asistentewhatsapp.bookings.application.BookingPolicyService;
import com.asistentewhatsapp.shared.observability.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.asistentewhatsapp.bookings.application.ReminderSchedulingService;
import com.asistentewhatsapp.bookings.infrastructure.BookingJdbcRepository;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteDigitalAgendaServiceTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final UUID LOCATION_ID = UUID.randomUUID();
	private static final UUID SERVICE_ID = UUID.randomUUID();
	private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
	private static final UUID ROOM_ID = UUID.randomUUID();
	private static final int DURATION = 60;
	private static final int PREPARATION = 10;
	private static final int CLEANUP = 10;

	private static final LocationRecord LOCATION = new LocationRecord(LOCATION_ID, "Sucursal Test", "America/Santiago");
	private static final ServiceRecord SERVICE = new ServiceRecord(SERVICE_ID, "Depilacion laser", DURATION, true, true,
			BigDecimal.valueOf(10000), PREPARATION, CLEANUP);
	private static final ProfessionalRecord PROFESSIONAL = new ProfessionalRecord(PROFESSIONAL_ID, "Dr. Test");
	private static final RoomRecord ROOM = new RoomRecord(ROOM_ID, "Cabina 1");

	private CompleteAgendaJdbcRepository repository;
	private BookingJdbcRepository bookingJdbcRepository;
	private CompleteDigitalAgendaService service;

	@BeforeEach
	void setUp() {
		repository = mock(CompleteAgendaJdbcRepository.class);
		bookingJdbcRepository = mock(BookingJdbcRepository.class);
		BookingConfirmationService bookingConfirmationService = mock(BookingConfirmationService.class);
		AuditService auditService = mock(AuditService.class);
		ChannelDispatchService channelDispatchService = mock(ChannelDispatchService.class);
		AvailabilityService availabilityService = mock(AvailabilityService.class);
		BookingPolicyService bookingPolicyService = mock(BookingPolicyService.class);

		when(repository.hasActiveAbsence(any(), any(), any(), any())).thenReturn(false);
		when(repository.findProfessionalMaxDailyBookings(any(), any())).thenReturn(null);
		when(bookingPolicyService.getSlotStepMinutes(any(), any())).thenReturn(15);

		service = new CompleteDigitalAgendaService(repository, bookingJdbcRepository, bookingConfirmationService,
				mock(CalendarSyncService.class), auditService, channelDispatchService, availabilityService,
				mock(ReminderSchedulingService.class), bookingPolicyService,
				new BusinessMetrics(new SimpleMeterRegistry()));
	}

	private OffsetDateTime futureStartsAt() {
		return OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(16).withMinute(0).withSecond(0).withNano(0);
	}

	private OffsetDateTime pastStartsAt() {
		return OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
	}

	private void mockHappyPath(OffsetDateTime startsAt) {
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		LocalDate date = startsAt.atZoneSameInstant(clOffset).toLocalDate();
		int dayOfWeek = startsAt.atZoneSameInstant(clOffset).getDayOfWeek().getValue();

		when(repository.findLocation(BUSINESS_ID, LOCATION_ID)).thenReturn(LOCATION);
		when(repository.findService(BUSINESS_ID, LOCATION_ID, SERVICE_ID)).thenReturn(SERVICE);
		when(repository.isHoliday(BUSINESS_ID, LOCATION_ID, date)).thenReturn(false);
		when(repository.findBusinessHours(BUSINESS_ID, LOCATION_ID, dayOfWeek))
				.thenReturn(List.of(new TimeWindowRecord(LocalTime.of(9, 0), LocalTime.of(18, 0))));
		when(repository.findProfessionalHours(BUSINESS_ID, LOCATION_ID, PROFESSIONAL_ID, dayOfWeek))
				.thenReturn(List.of(new TimeWindowRecord(LocalTime.of(9, 0), LocalTime.of(18, 0))));
		when(repository.findProfessionalCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID))
				.thenReturn(List.of(PROFESSIONAL));
		when(repository.findRoomCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, ROOM_ID)).thenReturn(List.of(ROOM));
		when(repository.hasConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
		when(repository.hasBlock(any(), any(), any(), any(), any(), any())).thenReturn(false);
	}

	@Test
	void acceptsValidSlot() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);

		assertThatCode(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, ROOM_ID,
				startsAt, null)).doesNotThrowAnyException();
	}

	@Test
	void createTemporaryBookingReturnsPersistedResultForIdempotentRetry() {
		OffsetDateTime startsAt = futureStartsAt();
		UUID bookingId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		String requestHash = stableUuid("TEMPORARY_BOOKING_CREATE", BUSINESS_ID, null, null, "56911112222", LOCATION_ID,
				SERVICE_ID, PROFESSIONAL_ID, ROOM_ID, startsAt, DURATION);
		when(repository.findLocation(BUSINESS_ID, LOCATION_ID)).thenReturn(LOCATION);
		when(repository.findService(BUSINESS_ID, LOCATION_ID, SERVICE_ID)).thenReturn(SERVICE);
		when(repository.findProfessionalCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID))
				.thenReturn(List.of(PROFESSIONAL));
		when(repository.findRoomCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, ROOM_ID)).thenReturn(List.of(ROOM));
		when(repository.findBookingOperationIdempotency(BUSINESS_ID, "TEMPORARY_BOOKING_CREATE", "idem-1"))
				.thenReturn(new BookingOperationIdempotencyRecord("TEMPORARY_BOOKING_CREATE", "idem-1", requestHash,
						"COMPLETED", bookingId));
		when(bookingJdbcRepository.findBookingDetail(BUSINESS_ID, bookingId)).thenReturn(bookingDetail(bookingId));

		BookingDetailResponse result = service.createTemporaryBooking(
				new AuthenticatedUser(userId, BUSINESS_ID, "Demo", "Admin", "Demo", "admin@test.cl", "UTC",
						List.of("ADMIN"), List.of()),
				new CreateTemporaryAgendaBookingRequest(LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, ROOM_ID, startsAt,
						"Cliente Test", "56911112222", null, null, null, null, "Notas", 60, false, false, "idem-1",
						null, null, null, null));

		assertThat(result.id()).isEqualTo(bookingId);
		verify(repository, never()).reserveBookingOperationIdempotency(any(), anyString(), anyString(), anyString(),
				anyString());
		verify(repository, never()).hasConflict(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createTemporaryBookingRejectsMissingInformedConsentWhenServiceRequiresIt() {
		OffsetDateTime startsAt = futureStartsAt();
		ServiceRecord consentService = new ServiceRecord(SERVICE_ID, "Laser avanzado", DURATION, true, false,
				BigDecimal.ZERO, PREPARATION, CLEANUP, true, BigDecimal.valueOf(10000), false, true);
		mockHappyPath(startsAt);
		when(repository.findService(BUSINESS_ID, LOCATION_ID, SERVICE_ID)).thenReturn(consentService);
		when(repository.reserveBookingOperationIdempotency(any(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(true);

		assertThatThrownBy(() -> service.createTemporaryBooking(
				new AuthenticatedUser(UUID.randomUUID(), BUSINESS_ID, "Demo", "Admin", "Demo", "admin@test.cl", "UTC",
						List.of("ADMIN"), List.of()),
				new CreateTemporaryAgendaBookingRequest(LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, ROOM_ID, startsAt,
						"Cliente Test", "56911112222", null, null, null, null, "Notas", 60, false, false, null, false,
						null, null, null)))
				.isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("informedConsentAccepted"));
		verify(repository, never()).hasConflict(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void rejectsNullStartsAt() {
		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, null, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("startsAt"));
	}

	@Test
	void rejectsPastStartsAt() {
		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, pastStartsAt(), null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("startsAt"));
	}

	@Test
	void rejectsHoliday() {
		OffsetDateTime startsAt = futureStartsAt();
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		LocalDate date = startsAt.atZoneSameInstant(clOffset).toLocalDate();
		mockHappyPath(startsAt);
		when(repository.isHoliday(BUSINESS_ID, LOCATION_ID, date)).thenReturn(true);

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_HOLIDAY"));
	}

	@Test
	void rejectsDayWithNoBusinessHours() {
		OffsetDateTime startsAt = futureStartsAt();
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		int dayOfWeek = startsAt.atZoneSameInstant(clOffset).getDayOfWeek().getValue();
		mockHappyPath(startsAt);
		when(repository.findBusinessHours(BUSINESS_ID, LOCATION_ID, dayOfWeek)).thenReturn(List.of());

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_OUTSIDE_BUSINESS_HOURS"));
	}

	@Test
	void rejectsOutsideBusinessHours() {
		OffsetDateTime startsAt = futureStartsAt();
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		int dayOfWeek = startsAt.atZoneSameInstant(clOffset).getDayOfWeek().getValue();
		mockHappyPath(startsAt);
		when(repository.findBusinessHours(BUSINESS_ID, LOCATION_ID, dayOfWeek))
				.thenReturn(List.of(new TimeWindowRecord(LocalTime.of(9, 0), LocalTime.of(10, 0))));

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_OUTSIDE_BUSINESS_HOURS"));
	}

	@Test
	void rejectsDayWithNoProfessionalHours() {
		OffsetDateTime startsAt = futureStartsAt();
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		int dayOfWeek = startsAt.atZoneSameInstant(clOffset).getDayOfWeek().getValue();
		mockHappyPath(startsAt);
		when(repository.findProfessionalHours(BUSINESS_ID, LOCATION_ID, PROFESSIONAL_ID, dayOfWeek))
				.thenReturn(List.of());

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_PROFESSIONAL_NOT_AVAILABLE"));
	}

	@Test
	void rejectsOutsideProfessionalHours() {
		OffsetDateTime startsAt = futureStartsAt();
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		int dayOfWeek = startsAt.atZoneSameInstant(clOffset).getDayOfWeek().getValue();
		mockHappyPath(startsAt);
		when(repository.findProfessionalHours(BUSINESS_ID, LOCATION_ID, PROFESSIONAL_ID, dayOfWeek))
				.thenReturn(List.of(new TimeWindowRecord(LocalTime.of(9, 0), LocalTime.of(10, 0))));

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_PROFESSIONAL_NOT_AVAILABLE"));
	}

	@Test
	void rejectsProfessionalNotCandidate() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.findProfessionalCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID))
				.thenReturn(List.of());

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_PROFESSIONAL_NOT_CANDIDATE"));
	}

	@Test
	void rejectsNullRoomWhenServiceRequiresRoom() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, null,
				startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("roomId"));
	}

	@Test
	void rejectsRoomNotCandidate() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.findRoomCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, ROOM_ID)).thenReturn(List.of());

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_ROOM_NOT_CANDIDATE"));
	}

	@Test
	void rejectsConflictingSlot() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.hasConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_SLOT_NOT_AVAILABLE"));
	}

	@Test
	void rejectsBlockedSlot() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.hasBlock(any(), any(), any(), any(), any(), any())).thenReturn(true);

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_SLOT_BLOCKED"));
	}

	@Test
	void acceptsSlotWhenExcludeBookingIdAvoidsSelfConflict() {
		UUID bookingId = UUID.randomUUID();
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.hasConflict(eq(BUSINESS_ID), eq(bookingId), any(), any(), any(), any(), any()))
				.thenReturn(false);

		assertThatCode(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, ROOM_ID,
				startsAt, bookingId)).doesNotThrowAnyException();
	}

	@Test
	void acceptsSlotWithoutRoomWhenServiceDoesNotRequireRoom() {
		OffsetDateTime startsAt = futureStartsAt();
		ZoneOffset clOffset = ZoneOffset.ofHours(-3);
		LocalDate date = startsAt.atZoneSameInstant(clOffset).toLocalDate();
		int dayOfWeek = startsAt.atZoneSameInstant(clOffset).getDayOfWeek().getValue();

		ServiceRecord serviceNoRoom = new ServiceRecord(SERVICE_ID, "Consulta", 60, false, false, BigDecimal.ZERO, 5,
				5);

		when(repository.findLocation(BUSINESS_ID, LOCATION_ID)).thenReturn(LOCATION);
		when(repository.findService(BUSINESS_ID, LOCATION_ID, SERVICE_ID)).thenReturn(serviceNoRoom);
		when(repository.isHoliday(BUSINESS_ID, LOCATION_ID, date)).thenReturn(false);
		when(repository.findBusinessHours(BUSINESS_ID, LOCATION_ID, dayOfWeek))
				.thenReturn(List.of(new TimeWindowRecord(LocalTime.of(9, 0), LocalTime.of(18, 0))));
		when(repository.findProfessionalHours(BUSINESS_ID, LOCATION_ID, PROFESSIONAL_ID, dayOfWeek))
				.thenReturn(List.of(new TimeWindowRecord(LocalTime.of(9, 0), LocalTime.of(18, 0))));
		when(repository.findProfessionalCandidates(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID))
				.thenReturn(List.of(PROFESSIONAL));
		when(repository.hasConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
		when(repository.hasBlock(any(), any(), any(), any(), any(), any())).thenReturn(false);

		assertThatCode(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID, null,
				startsAt, null)).doesNotThrowAnyException();
	}

	@Test
	void rejectsSimultaneousCreationInSameSlot() {
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.hasConflict(any(), eq(null), any(), any(), any(), any(), any())).thenReturn(true);

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_SLOT_NOT_AVAILABLE"));
	}

	@Test
	void rejectsRescheduleToConflictingSlot() {
		UUID bookingId = UUID.randomUUID();
		OffsetDateTime startsAt = futureStartsAt();
		mockHappyPath(startsAt);
		when(repository.hasConflict(eq(BUSINESS_ID), eq(bookingId), any(), any(), any(), any(), any()))
				.thenReturn(true);

		assertThatThrownBy(() -> service.assertSlotBookable(BUSINESS_ID, LOCATION_ID, SERVICE_ID, PROFESSIONAL_ID,
				ROOM_ID, startsAt, bookingId)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("AGENDA_SLOT_NOT_AVAILABLE"));
	}

	private BookingDetailResponse bookingDetail(UUID bookingId) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		return new BookingDetailResponse(bookingId, "Depilacion laser", "PENDIENTE_CONFIRMACION", now, DURATION,
				LOCATION_ID, "Sucursal Test", "Sucursal Test", SERVICE_ID, PROFESSIONAL_ID, ROOM_ID, null, null, now,
				now, null, "Cliente Test", "56911112222", null, null, null, null, null, true, BigDecimal.valueOf(10000),
				"PENDING", List.of(), List.of(), List.of(), List.of(), List.of());
	}

	private String stableUuid(Object... values) {
		StringBuilder builder = new StringBuilder();
		for (Object value : values) {
			builder.append(value == null ? "" : value).append('|');
		}
		return UUID.nameUUIDFromBytes(builder.toString().getBytes(StandardCharsets.UTF_8)).toString();
	}
}
