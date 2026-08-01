package com.asistentewhatsapp.bookings.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.bookings.domain.SincronizadorReservaMotorReglas;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationLinkRecord;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.infrastructure.WhatsAppChannelJdbcRepository;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.shared.email.TransactionalEmailService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

@DisplayName("BookingConfirmationServiceCalendar - Integración de calendario en confirmación de reservas")
class BookingConfirmationServiceCalendarTest {

	private final BookingConfirmationJdbcRepository repository = mock(BookingConfirmationJdbcRepository.class);
	private final BookingConfirmationProperties properties = new BookingConfirmationProperties();
	private final TokenHashService tokenHashService = mock(TokenHashService.class);
	private final AuditService auditService = mock(AuditService.class);
	private final ChannelDispatchService channelDispatchService = mock(ChannelDispatchService.class);
	private final CompleteAgendaJdbcRepository completeAgendaJdbcRepository = mock(CompleteAgendaJdbcRepository.class);
	private final WhatsAppChannelJdbcRepository whatsAppWebChannelJdbcRepository = mock(
			WhatsAppChannelJdbcRepository.class);
	private final BookingEmailService bookingEmailService = mock(BookingEmailService.class);
	private final BookingPaymentService bookingPaymentService = mock(BookingPaymentService.class);
	private final CalendarSyncService calendarSyncService = mock(CalendarSyncService.class);
	private final BookingConfirmationNotificationsService notificationsService = mock(
			BookingConfirmationNotificationsService.class);
	private final SincronizadorReservaMotorReglas sincronizadorReservaMotorReglas = mock(
			SincronizadorReservaMotorReglas.class);
	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
	private final ReminderSchedulingService reminderSchedulingService = mock(ReminderSchedulingService.class);
	private final TransactionalEmailService transactionalEmailService = mock(TransactionalEmailService.class);
	private final BookingPolicyService bookingPolicyService = mock(BookingPolicyService.class);
	private final BookingConfirmationService service;
	private final ConfirmationLinkRecord link;

	BookingConfirmationServiceCalendarTest() {
		properties.setDispatchWhatsApp(true);
		service = new BookingConfirmationService(repository, properties, tokenHashService, auditService,
				channelDispatchService, completeAgendaJdbcRepository, whatsAppWebChannelJdbcRepository,
				bookingEmailService, bookingPaymentService, calendarSyncService, notificationsService,
				reminderSchedulingService, sincronizadorReservaMotorReglas, transactionManager,
				transactionalEmailService, bookingPolicyService);
		link = confirmationLink();
	}

	@BeforeEach
	void setUp() {
		when(tokenHashService.sha256("valid-token")).thenReturn("hash-valid");
		when(repository.findByTokenHash("hash-valid")).thenReturn(link);
		when(repository.findByTokenHashForUpdate("hash-valid")).thenReturn(link);
		when(bookingPaymentService.hasApprovedRequiredDeposit(any(), any())).thenReturn(false);
		when(repository.hasOverlappingActiveBooking(any(), any(), any(), any(), any())).thenReturn(false);
	}

	@Test
	@DisplayName("Cuando una reserva se confirma, CalendarSyncService.syncConfirmed es llamado vía notificationsService")
	void confirmCallsSyncConfirmed() {
		service.confirm("valid-token");
		verify(notificationsService).syncCalendar(link.bookingId(), link.businessId());
	}

	@Test
	@DisplayName("Falló de CalendarSyncService no revierte la confirmación de la reserva")
	void calendarSyncFailureDoesNotRollbackConfirmation() {
		doThrow(new RuntimeException("Error de calendario")).when(notificationsService).syncCalendar(any(), any());

		service.confirm("valid-token");

		verify(repository).markConfirmed(link.linkId());
		verify(repository).updateBookingStatus(link.businessId(), link.bookingId(), BookingStateMachine.CONFIRMED);
	}

	@Test
	@DisplayName("Falló de CalendarSyncService no impide otros efectos secundarios")
	void calendarSyncFailureDoesNotBlockOtherSideEffects() {
		doThrow(new RuntimeException("Error de calendario")).when(notificationsService).syncCalendar(any(), any());

		service.confirm("valid-token");

		verify(notificationsService).scheduleReminders(any(), any(), any());
		verify(notificationsService).sendConfirmationWhatsApp(link);
		verify(notificationsService).sendConfirmationEmail(link);
		verify(notificationsService).auditRecord(any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
	}

	private ConfirmationLinkRecord confirmationLink() {
		UUID businessId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		return new ConfirmationLinkRecord(UUID.randomUUID(), businessId, bookingId, "OPENED",
				"http://localhost/confirm/token", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1), null,
				OffsetDateTime.now(ZoneOffset.UTC), null, "Reserva", BookingStateMachine.PENDING_CONFIRMATION,
				"Servicio", "Profesional", "Sala", OffsetDateTime.now(ZoneOffset.UTC).plusDays(2), 60,
				UUID.randomUUID(), "Sucursal", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Sucursal",
				"Cliente", "+56911111111", "cliente@example.com", false, BigDecimal.valueOf(0), null, null);
	}
}
