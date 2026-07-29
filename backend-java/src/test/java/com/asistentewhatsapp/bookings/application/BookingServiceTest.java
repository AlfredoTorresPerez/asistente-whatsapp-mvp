package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.bookings.api.CancelBookingRequest;
import com.asistentewhatsapp.bookings.api.CreateBookingRequest;

import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final AuthenticatedUser USER = new AuthenticatedUser(UUID.randomUUID(), BUSINESS_ID, "Negocio",
			"Admin", "Test", "admin@test.com", "America/Santiago", List.of("ADMIN"), List.of());

	private BookingJdbcRepository bookingJdbcRepository;
	private BusinessLocationJdbcRepository businessLocationJdbcRepository;
	private BookingConfirmationJdbcRepository bookingConfirmationJdbcRepository;
	private CompleteAgendaJdbcRepository agendaRepository;
	private BookingService bookingService;

	@BeforeEach
	void setUp() {
		bookingJdbcRepository = mock(BookingJdbcRepository.class);
		businessLocationJdbcRepository = mock(BusinessLocationJdbcRepository.class);
		bookingConfirmationJdbcRepository = mock(BookingConfirmationJdbcRepository.class);
		agendaRepository = mock(CompleteAgendaJdbcRepository.class);

		when(businessLocationJdbcRepository.countActive(any())).thenReturn(0L);

		bookingService = new BookingService(bookingJdbcRepository, businessLocationJdbcRepository,
				bookingConfirmationJdbcRepository, agendaRepository, mock(AvailabilityService.class),
				mock(AuditService.class));
	}

	@Test
	void rejectsCreateWithPastStartsAt() {
		OffsetDateTime pastDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
		CreateBookingRequest request = new CreateBookingRequest("Test", null, null, null, "Cliente", "56912345678",
				null, null, null, pastDate, 60, null, null, null);

		assertThatThrownBy(() -> bookingService.create(USER, request)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("startsAt"));
	}

	@Test
	void rejectsCreateWithPastHourToday() {
		OffsetDateTime pastHour = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
		CreateBookingRequest request = new CreateBookingRequest("Test", null, null, null, "Cliente", "56912345678",
				null, null, null, pastHour, 60, null, null, null);

		assertThatThrownBy(() -> bookingService.create(USER, request)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("startsAt"));
	}

	@Test
	void rejectsCancelWithoutReason() {
		UUID bookingId = UUID.randomUUID();
		CancelBookingRequest request = new CancelBookingRequest("");

		when(bookingJdbcRepository.findBookingDetail(any(), any()))
				.thenReturn(mockBookingDetail("PENDIENTE_CONFIRMACION", "Test"));

		assertThatThrownBy(() -> bookingService.cancel(USER, bookingId, request)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("reason"));
	}

	@Test
	void rejectsCancelWithBlankReason() {
		UUID bookingId = UUID.randomUUID();
		CancelBookingRequest request = new CancelBookingRequest("   ");

		when(bookingJdbcRepository.findBookingDetail(any(), any()))
				.thenReturn(mockBookingDetail("PENDIENTE_CONFIRMACION", "Test"));

		assertThatThrownBy(() -> bookingService.cancel(USER, bookingId, request)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getFieldErrors().containsKey("reason"));
	}

	private BookingDetailResponse mockBookingDetail(String status, String subject) {
		OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
		return new BookingDetailResponse(UUID.randomUUID(), subject, status, futureDate, 60, UUID.randomUUID(),
				"Sucursal Test", "Sucursal Test", null, null, null, null, UUID.randomUUID(), "Cliente Test",
				"56912345678", "test@test.com", null, null, null, null, false, BigDecimal.ZERO, "NOT_REQUIRED",
				List.of(), List.of(), List.of(), List.of(), List.of());
	}
}
