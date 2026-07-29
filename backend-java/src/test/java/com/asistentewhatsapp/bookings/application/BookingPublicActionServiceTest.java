package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.bookings.api.PublicBookingCancellationRequest;
import com.asistentewhatsapp.bookings.api.PublicBookingCancellationResponse;
import com.asistentewhatsapp.bookings.api.PublicBookingRescheduleResponse;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository.ActionBookingRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository.CancellationLinkRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository.RescheduleLinkRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.TokenHashService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingPublicActionServiceTest {

	private static final String PHONE = "56950954580";
	private static final String CANCEL_REASON = "No podre asistir";

	private Fixture fixture;

	@BeforeEach
	void setUp() {
		fixture = new Fixture();
		when(fixture.aestheticRepository.findServices(any(), anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(new com.asistentewhatsapp.shared.api.PagedResponse<>(java.util.List.of(), 0, 1000, 0, 0));
	}

	@Test
	void previewCancellationReturnsBookingDetails() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CancellationLinkRecord link = cancellationLink(businessId, bookingId, "ACTIVE", "PENDIENTE_CONFIRMACION", null);
		when(fixture.tokenHashService.sha256("valid-token")).thenReturn("hash");
		when(fixture.repository.findCancellationByTokenHash("hash", false)).thenReturn(link);

		PublicBookingCancellationResponse response = fixture.service.previewCancellation("valid-token");

		assertThat(response.bookingId()).isEqualTo(bookingId);
		assertThat(response.bookingStatus()).isEqualTo("PENDIENTE_CONFIRMACION");
		assertThat(response.linkStatus()).isEqualTo("ACTIVE");
		assertThat(response.maskedCustomerPhone()).isEqualTo("****4580");
		assertThat(response.serviceName()).isEqualTo("Limpieza facial");
		assertThat(response.customerName()).isEqualTo("Maria Perez");
		assertThat(response.cancellationReason()).isNull();
	}

	@Test
	void previewCancellationReturnsBookingDetailsWithPhone56950954580() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CancellationLinkRecord link = new CancellationLinkRecord(UUID.randomUUID(), businessId, bookingId, "ACTIVE",
				"http://localhost/cancel/token", OffsetDateTime.now(ZoneOffset.UTC).plusHours(24), null, null,
				"Manicure", "PENDIENTE_CONFIRMACION", OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(1), "Sucursal Centro", "Manicure clasico",
				"Profesional Test", "Sala 1", "Maria Perez", PHONE, "maria@test.com");
		when(fixture.tokenHashService.sha256("token-569")).thenReturn("hash-569");
		when(fixture.repository.findCancellationByTokenHash("hash-569", false)).thenReturn(link);

		PublicBookingCancellationResponse response = fixture.service.previewCancellation("token-569");

		assertThat(response.maskedCustomerPhone()).isEqualTo("****4580");
		assertThat(response.customerName()).isEqualTo("Maria Perez");
	}

	@Test
	void previewRescheduleReturnsBookingDetailsWithPhone56950954580() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		RescheduleLinkRecord link = rescheduleLink(businessId, bookingId, "ACTIVE", "CONFIRMADA");
		when(fixture.tokenHashService.sha256("resched-token")).thenReturn("resched-hash");
		when(fixture.repository.findRescheduleByTokenHash("resched-hash", false)).thenReturn(link);
		when(fixture.agendaRepository.findActiveBookingsByPhone(eq(businessId), eq("56950954580")))
				.thenReturn(List.of());

		PublicBookingRescheduleResponse response = fixture.service.previewReschedule("resched-token");

		assertThat(response.bookingId()).isEqualTo(bookingId);
		assertThat(response.linkStatus()).isEqualTo("ACTIVE");
		assertThat(response.maskedCustomerPhone()).isEqualTo("****4580");
		assertThat(response.customerName()).isEqualTo("Maria Perez");
		assertThat(response.bookings()).isEmpty();
	}

	@Test
	void confirmCancellationWithReasonChangesStatus() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CancellationLinkRecord link = cancellationLink(businessId, bookingId, "ACTIVE", "CONFIRMADA", null);
		CancellationLinkRecord cancelledLink = cancellationLink(businessId, bookingId, "USED", "CANCELADA",
				CANCEL_REASON);
		ActionBookingRecord booking = actionBooking(businessId, bookingId, "CONFIRMADA");
		when(fixture.tokenHashService.sha256(any())).thenReturn("hash");
		when(fixture.repository.findCancellationByTokenHash(any(), anyBoolean())).thenReturn(link)
				.thenReturn(cancelledLink);
		when(fixture.repository.findBookingForUpdate(businessId, bookingId)).thenReturn(booking);

		PublicBookingCancellationResponse response = fixture.service.confirmCancellation("valid-token",
				new PublicBookingCancellationRequest(CANCEL_REASON));

		assertThat(response.bookingStatus()).isEqualTo("CANCELADA");
		assertThat(response.cancellationReason()).isEqualTo(CANCEL_REASON);
		verify(fixture.agendaRepository).cancelBooking(eq(businessId), eq(bookingId), eq(null), eq(CANCEL_REASON));
		verify(fixture.repository).markCancellationUsed(link.linkId(), CANCEL_REASON);
	}

	@Test
	void confirmCancellationWithPhone56950954580RejectsAlreadyCancelled() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CancellationLinkRecord link = cancellationLink(businessId, bookingId, "USED", "CANCELADA", null);
		when(fixture.tokenHashService.sha256("used-token")).thenReturn("used-hash");
		when(fixture.repository.findCancellationByTokenHash("used-hash", true)).thenReturn(link);

		PublicBookingCancellationResponse response = fixture.service.confirmCancellation("used-token",
				new PublicBookingCancellationRequest(CANCEL_REASON));

		assertThat(response.linkStatus()).isEqualTo("USED");
		assertThat(response.bookingStatus()).isEqualTo("CANCELADA");
	}

	@Test
	void rejectCancellationWithBlankReasonOnConfirmedBooking() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CancellationLinkRecord link = cancellationLink(businessId, bookingId, "ACTIVE", "CONFIRMADA", null);
		CancellationLinkRecord cancelledLink = cancellationLink(businessId, bookingId, "USED", "CANCELADA",
				"Cancelacion confirmada por enlace publico.");
		ActionBookingRecord booking = actionBooking(businessId, bookingId, "CONFIRMADA");
		when(fixture.tokenHashService.sha256(any())).thenReturn("hash-no-reason");
		when(fixture.repository.findCancellationByTokenHash(any(), anyBoolean())).thenReturn(link)
				.thenReturn(cancelledLink);
		when(fixture.repository.findBookingForUpdate(businessId, bookingId)).thenReturn(booking);

		fixture.service.confirmCancellation("token-no-reason", new PublicBookingCancellationRequest(""));

		verify(fixture.agendaRepository).cancelBooking(eq(businessId), eq(bookingId), eq(null),
				eq("Cancelacion confirmada por enlace publico."));
	}

	@Test
	void previewExpiredCancellationLinkReturnsExpiredStatus() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		CancellationLinkRecord expiredLink = new CancellationLinkRecord(UUID.randomUUID(), businessId, bookingId,
				"ACTIVE", "http://localhost/cancel/token", OffsetDateTime.now(ZoneOffset.UTC).minusHours(1), null, null,
				"Limpieza facial", "CONFIRMADA", OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(1), "Sucursal Centro", "Limpieza facial",
				"Profesional Test", "Sala 1", "Maria Perez", PHONE, "maria@test.com");
		CancellationLinkRecord refreshedExpired = new CancellationLinkRecord(expiredLink.linkId(), businessId,
				bookingId, "EXPIRED", expiredLink.publicUrl(), OffsetDateTime.now(ZoneOffset.UTC).minusHours(1), null,
				null, "Manicure", "CONFIRMADA", OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1), "Sucursal Centro", "Manicure clasico",
				"Prof Test", "Sala 1", "Maria Perez", PHONE, "maria@test.com");
		when(fixture.tokenHashService.sha256("expired-token")).thenReturn("expired-hash");
		when(fixture.repository.findCancellationByTokenHash("expired-hash", false)).thenReturn(expiredLink);
		when(fixture.repository.findCancellationByTokenHash("expired-hash", false)).thenReturn(expiredLink)
				.thenReturn(refreshedExpired);

		PublicBookingCancellationResponse response = fixture.service.previewCancellation("expired-token");

		assertThat(response.linkStatus()).isEqualTo("EXPIRED");
		verify(fixture.repository).markExpiredCancellation(expiredLink.linkId());
	}

	private static CancellationLinkRecord cancellationLink(UUID businessId, UUID bookingId, String linkStatus,
			String bookingStatus, String cancellationReason) {
		return new CancellationLinkRecord(UUID.randomUUID(), businessId, bookingId, linkStatus,
				"http://localhost/cancel/token", OffsetDateTime.now(ZoneOffset.UTC).plusHours(24), null,
				cancellationReason, "Limpieza facial", bookingStatus, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(1), "Sucursal Centro", "Limpieza facial",
				"Profesional Test", "Sala 1", "Maria Perez", PHONE, "maria@test.com");
	}

	private static RescheduleLinkRecord rescheduleLink(UUID businessId, UUID bookingId, String linkStatus,
			String bookingStatus) {
		return new RescheduleLinkRecord(UUID.randomUUID(), businessId, bookingId, linkStatus,
				"http://localhost/reschedule/token", OffsetDateTime.now(ZoneOffset.UTC).plusDays(5),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(5).plusHours(1), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC).plusHours(24), null, null,
				"Depilacion laser", bookingStatus, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(1), "Sucursal Centro", "Depilacion laser",
				"Depilacion laser avanzada", "Ana Profesional", "Carlos Profesional", "Sala Laser", "Sala Laser VIP",
				"Sucursal Centro Propuesta", "Maria Perez", PHONE, "maria@test.com");
	}

	private static ActionBookingRecord actionBooking(UUID businessId, UUID bookingId, String status) {
		return new ActionBookingRecord(bookingId, businessId, "Limpieza facial", status,
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(1), 60, UUID.randomUUID(), "Sucursal Centro",
				UUID.randomUUID(), "Limpieza facial", UUID.randomUUID(), "Prof Test", UUID.randomUUID(), "Sala 1", null,
				0, "Maria Perez", PHONE, "maria@test.com");
	}

	private static final class Fixture {
		private final BookingActionLinkJdbcRepository repository = mock(BookingActionLinkJdbcRepository.class);
		private final CompleteAgendaJdbcRepository agendaRepository = mock(CompleteAgendaJdbcRepository.class);
		private final BusinessLocationJdbcRepository locationRepository = mock(BusinessLocationJdbcRepository.class);
		private final AestheticCenterJdbcRepository aestheticRepository = mock(AestheticCenterJdbcRepository.class);
		private final TokenHashService tokenHashService = mock(TokenHashService.class);
		private final CalendarSyncService calendarSyncService = mock(CalendarSyncService.class);
		private final AuditService auditService = mock(AuditService.class);
		private final ChannelDispatchService channelDispatchService = mock(ChannelDispatchService.class);
		private final BookingEmailService bookingEmailService = mock(BookingEmailService.class);
		private final AvailabilityService availabilityService = mock(AvailabilityService.class);
		private final ReminderSchedulingService reminderSchedulingService = mock(ReminderSchedulingService.class);
		private final BookingPolicyService bookingPolicyService = mock(BookingPolicyService.class);
		private final BookingPublicActionService service = new BookingPublicActionService(repository, agendaRepository,
				locationRepository, aestheticRepository, tokenHashService, calendarSyncService, auditService,
				channelDispatchService, bookingEmailService, availabilityService, reminderSchedulingService,
				bookingPolicyService, "http://localhost:5173/reservas/reprogramar",
				"http://localhost:5173/reservas/cancelar");
	}
}
