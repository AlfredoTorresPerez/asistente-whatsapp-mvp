package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.bookings.domain.SincronizadorReservaMotorReglas;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationLinkRecord;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class BookingConfirmationServiceTest {

    @Test
    void confirmBlocksBookingThatRequiresDepositWithoutApprovedPayment() {
        Fixture fixture = new Fixture();
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ConfirmationLinkRecord link = confirmationLink(businessId, bookingId, true, "PENDING", BookingStateMachine.PENDING_PAYMENT);
        when(fixture.tokenHashService.sha256("token")).thenReturn("hash");
        when(fixture.repository.findByTokenHash("hash")).thenReturn(link);
        when(fixture.repository.findByTokenHashForUpdate("hash")).thenReturn(link);
        when(fixture.bookingPaymentService.hasApprovedRequiredDeposit(businessId, bookingId)).thenReturn(false);

        assertThatThrownBy(() -> fixture.service.confirm("token"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BOOKING_PAYMENT_REQUIRED");

        verify(fixture.repository, never()).markConfirmed(any());
        verify(fixture.repository, never()).updateBookingStatus(any(), any(), eq(BookingStateMachine.CONFIRMED));
    }

    @Test
    void confirmAllowsBookingThatRequiresDepositWithApprovedPayment() {
        Fixture fixture = new Fixture();
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ConfirmationLinkRecord link = confirmationLink(businessId, bookingId, true, "PAID", BookingStateMachine.PENDING_PAYMENT);
        when(fixture.tokenHashService.sha256("token")).thenReturn("hash");
        when(fixture.repository.findByTokenHashForUpdate("hash")).thenReturn(link);
        when(fixture.repository.findByTokenHash("hash")).thenReturn(link);
        when(fixture.bookingPaymentService.hasApprovedRequiredDeposit(businessId, bookingId)).thenReturn(true);
        when(fixture.repository.hasOverlappingActiveBooking(eq(businessId), eq(bookingId), any(), any(), any())).thenReturn(false);

        fixture.service.confirm("token");

        verify(fixture.repository).markConfirmed(link.linkId());
        verify(fixture.repository).updateBookingStatus(businessId, bookingId, BookingStateMachine.CONFIRMED);
        verify(fixture.completeAgendaJdbcRepository).insertStatusHistory(
                eq(businessId),
                eq(bookingId),
                eq(BookingStateMachine.PENDING_PAYMENT),
                eq(BookingStateMachine.CONFIRMED),
                any(),
                eq(null),
                eq("PUBLIC_LINK"));
    }

    @Test
    void confirmInvokesNotificationsServiceSideEffects() {
        Fixture fixture = new Fixture();
        fixture.properties.setDispatchWhatsApp(true);
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ConfirmationLinkRecord link = confirmationLink(businessId, bookingId, false, "PAID", "PENDING_PAYMENT");
        when(fixture.tokenHashService.sha256("token")).thenReturn("hash");
        when(fixture.repository.findByTokenHashForUpdate("hash")).thenReturn(link);
        when(fixture.repository.findByTokenHash("hash")).thenReturn(link);
        when(fixture.bookingPaymentService.hasApprovedRequiredDeposit(businessId, bookingId)).thenReturn(false);
        when(fixture.repository.hasOverlappingActiveBooking(eq(businessId), eq(bookingId), any(), any(), any())).thenReturn(false);

        fixture.service.confirm("token");

        verify(fixture.notificationsService).scheduleReminders(link.businessId(), link.bookingId(), link.startsAt());
        verify(fixture.notificationsService).sendConfirmationWhatsApp(link);
        verify(fixture.notificationsService).sendConfirmationEmail(link);
        verify(fixture.notificationsService).auditRecord(link.businessId(), link.bookingId(), link.linkId(),
                link.bookingStatus(), link.linkStatus(), link.requiresDeposit(), link.depositAmount(), link.paymentStatus(), link.startsAt());
        verify(fixture.notificationsService).syncCalendar(link.bookingId(), link.businessId());
    }

    private static ConfirmationLinkRecord confirmationLink(UUID businessId, UUID bookingId, boolean requiresDeposit, String paymentStatus, String bookingStatus) {
        return new ConfirmationLinkRecord(
                UUID.randomUUID(),
                businessId,
                bookingId,
                "OPENED",
                "http://localhost/confirm/token",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "Reserva",
                bookingStatus,
                "Servicio",
                "Profesional",
                "Sala",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
                60,
                UUID.randomUUID(),
                "Sucursal",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sucursal",
                "Cliente",
                "+56911111111",
                "cliente@example.com",
                requiresDeposit,
                BigDecimal.valueOf(10000),
                paymentStatus,
                null);
    }

    private static final class Fixture {
        private final BookingConfirmationJdbcRepository repository = mock(BookingConfirmationJdbcRepository.class);
        private final BookingConfirmationProperties properties = new BookingConfirmationProperties();
        private final TokenHashService tokenHashService = mock(TokenHashService.class);
        private final AuditService auditService = mock(AuditService.class);
        private final ChannelDispatchService channelDispatchService = mock(ChannelDispatchService.class);
        private final CompleteAgendaJdbcRepository completeAgendaJdbcRepository = mock(CompleteAgendaJdbcRepository.class);
        private final WhatsAppWebChannelJdbcRepository whatsAppWebChannelJdbcRepository = mock(WhatsAppWebChannelJdbcRepository.class);
        private final BookingEmailService bookingEmailService = mock(BookingEmailService.class);
        private final BookingPaymentService bookingPaymentService = mock(BookingPaymentService.class);
        private final CalendarSyncService calendarSyncService = mock(CalendarSyncService.class);
        private final BookingConfirmationNotificationsService notificationsService = mock(BookingConfirmationNotificationsService.class);
        private final SincronizadorReservaMotorReglas sincronizadorReservaMotorReglas = mock(SincronizadorReservaMotorReglas.class);
        private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        private final BookingConfirmationService service = new BookingConfirmationService(
                repository,
                properties,
                tokenHashService,
                auditService,
                channelDispatchService,
                completeAgendaJdbcRepository,
                whatsAppWebChannelJdbcRepository,
                bookingEmailService,
                bookingPaymentService,
                calendarSyncService,
                notificationsService,
                sincronizadorReservaMotorReglas,
                transactionManager);
    }
}
