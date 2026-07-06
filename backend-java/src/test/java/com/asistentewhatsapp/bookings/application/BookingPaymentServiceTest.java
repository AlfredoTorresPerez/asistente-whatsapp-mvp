package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.bookings.api.BookingPaymentWebhookResponse;
import com.asistentewhatsapp.bookings.api.BookingPaymentResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingPaymentLinkRequest;
import com.asistentewhatsapp.bookings.api.RegisterBookingManualPaymentRequest;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentBookingRecord;
import com.asistentewhatsapp.bookings.application.BookingPaymentProviderRegistry;
import com.asistentewhatsapp.bookings.domain.BookingPaymentProvider;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentRecord;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingPaymentServiceTest {

    @Test
    void approvedWebhookRecordsPaymentAndDoesNotConfirmExpiredBooking() {
        BookingPaymentJdbcRepository repository = mock(BookingPaymentJdbcRepository.class);
        AuditService auditService = mock(AuditService.class);
        BookingPaymentProperties properties = new BookingPaymentProperties();
        properties.setWebhookSignatureEnabled(false);
        properties.setDispatchPostPaymentWhatsApp(false);
        properties.setDispatchPostPaymentEmail(false);
        BookingPaymentService service = service(repository, properties, auditService);
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        when(repository.findBookingForUpdate(businessId, bookingId))
                .thenReturn(new BookingPaymentBookingRecord(bookingId, businessId, BookingStateMachine.EXPIRED, true, BigDecimal.valueOf(10000), "PENDING"))
                .thenReturn(new BookingPaymentBookingRecord(bookingId, businessId, BookingStateMachine.EXPIRED, true, BigDecimal.valueOf(10000), "PAID"));
        when(repository.findExisting(eq(businessId), eq("MERCADOPAGO"), eq("pay-1"), eq("idem-1"))).thenReturn(Optional.empty());
        when(repository.insertPayment(eq(businessId), eq(bookingId), eq("MERCADOPAGO"), eq("pay-1"), eq("idem-1"),
                eq(BigDecimal.valueOf(10000)), eq("CLP"), eq("APPROVED"), any(), any(), any()))
                .thenReturn(payment(paymentId, businessId, bookingId, "APPROVED"));

        BookingPaymentWebhookResponse response = service.handleWebhook("""
                {
                  "businessId": "%s",
                  "bookingId": "%s",
                  "provider": "mercadopago",
                  "providerPaymentId": "pay-1",
                  "idempotencyKey": "idem-1",
                  "amount": 10000,
                  "currency": "clp",
                  "status": "approved"
                }
                """.formatted(businessId, bookingId), null, null);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.bookingStatus()).isEqualTo(BookingStateMachine.EXPIRED);
        assertThat(response.bookingConfirmed()).isFalse();
        assertThat(response.duplicate()).isFalse();
        verify(repository).recalculateBookingPaymentStatus(businessId, bookingId);
        verify(auditService).record(eq(businessId), eq(null), eq("BOOKING_PAYMENT_APPROVED"), eq("BOOKING"), eq(bookingId), any(), any());
    }

    @Test
    void duplicateApprovedWebhookIsIdempotentAndDoesNotAuditAgain() {
        BookingPaymentJdbcRepository repository = mock(BookingPaymentJdbcRepository.class);
        AuditService auditService = mock(AuditService.class);
        BookingPaymentProperties properties = new BookingPaymentProperties();
        properties.setWebhookSignatureEnabled(false);
        properties.setDispatchPostPaymentWhatsApp(false);
        properties.setDispatchPostPaymentEmail(false);
        BookingPaymentService service = service(repository, properties, auditService);
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        BookingPaymentRecord existing = payment(UUID.randomUUID(), businessId, bookingId, "APPROVED");
        when(repository.findBookingForUpdate(businessId, bookingId))
                .thenReturn(new BookingPaymentBookingRecord(bookingId, businessId, BookingStateMachine.PENDING_PAYMENT, true, BigDecimal.valueOf(10000), "PAID"));
        when(repository.findExisting(eq(businessId), eq("MERCADOPAGO"), eq("pay-1"), eq("idem-1"))).thenReturn(Optional.of(existing));

        BookingPaymentWebhookResponse response = service.handleWebhook("""
                {
                  "businessId": "%s",
                  "bookingId": "%s",
                  "provider": "mercadopago",
                  "providerPaymentId": "pay-1",
                  "idempotencyKey": "idem-1",
                  "amount": 10000,
                  "currency": "CLP",
                  "status": "paid"
                }
                """.formatted(businessId, bookingId), null, null);

        assertThat(response.duplicate()).isTrue();
        verify(repository, never()).insertPayment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(repository, never()).recalculateBookingPaymentStatus(any(), any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void signatureValidationCanBeEnabled() {
        BookingPaymentProperties properties = new BookingPaymentProperties();
        properties.setWebhookSignatureEnabled(true);
        properties.setWebhookSecret("secret");
        BookingPaymentService service = service(mock(BookingPaymentJdbcRepository.class), properties, mock(AuditService.class));

        assertThatThrownBy(() -> service.handleWebhook("{}", "1", "sha256=bad"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_WEBHOOK_TIMESTAMP_EXPIRED");
    }

    @Test
    void checkoutLinkReusesActivePendingCheckout() {
        BookingPaymentJdbcRepository repository = mock(BookingPaymentJdbcRepository.class);
        AuditService auditService = mock(AuditService.class);
        BookingPaymentProperties properties = new BookingPaymentProperties();
        properties.setDispatchEmail(false);
        BookingPaymentService service = service(repository, properties, auditService);
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        AuthenticatedUser user = user(businessId);
        BookingPaymentRecord activeCheckout = new BookingPaymentRecord(
                UUID.randomUUID(),
                businessId,
                bookingId,
                "SIMULATED",
                null,
                "checkout-key",
                BigDecimal.valueOf(10000),
                "CLP",
                "PENDING",
                null,
                null,
                null,
                null,
                "http://localhost/pay/1",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30),
                false,
                OffsetDateTime.now(ZoneOffset.UTC));
        when(repository.findBookingForUpdate(businessId, bookingId))
                .thenReturn(new BookingPaymentBookingRecord(bookingId, businessId, BookingStateMachine.PENDING_PAYMENT, true, BigDecimal.valueOf(10000), "PENDING"));
        when(repository.findActiveCheckout(eq(businessId), eq(bookingId), any())).thenReturn(Optional.of(activeCheckout));

        BookingPaymentResponse response = service.createCheckoutLink(user, bookingId, new CreateBookingPaymentLinkRequest(null, null, null, null, false, false, null));

        assertThat(response.id()).isEqualTo(activeCheckout.id());
        assertThat(response.checkoutUrl()).isEqualTo("http://localhost/pay/1");
        verify(repository, never()).insertCheckoutPayment(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateManualPaymentByIdempotencyKeyDoesNotAuditAgain() {
        BookingPaymentJdbcRepository repository = mock(BookingPaymentJdbcRepository.class);
        AuditService auditService = mock(AuditService.class);
        BookingPaymentService service = service(repository, new BookingPaymentProperties(), auditService);
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        AuthenticatedUser user = user(businessId);
        BookingPaymentRecord existing = payment(UUID.randomUUID(), businessId, bookingId, "APPROVED");
        when(repository.findBookingForUpdate(businessId, bookingId))
                .thenReturn(new BookingPaymentBookingRecord(bookingId, businessId, BookingStateMachine.PENDING_PAYMENT, true, BigDecimal.valueOf(10000), "PAID"));
        when(repository.findExisting(businessId, "MANUAL", "trx-1", "manual-key")).thenReturn(Optional.of(existing));

        BookingPaymentResponse response = service.registerManualPayment(user, bookingId,
                new RegisterBookingManualPaymentRequest("manual", "trx-1", "manual-key", BigDecimal.valueOf(10000), "CLP", "APPROVED", null, null, null));

        assertThat(response.id()).isEqualTo(existing.id());
        verify(repository, never()).insertManualPayment(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(repository, never()).recalculateBookingPaymentStatus(any(), any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void schedulerExpiresPendingCheckoutAndRecalculatesBookingPaymentStatus() {
        BookingPaymentJdbcRepository repository = mock(BookingPaymentJdbcRepository.class);
        AuditService auditService = mock(AuditService.class);
        BookingPaymentProperties properties = new BookingPaymentProperties();
        properties.setExpirationBatchSize(25);
        BookingPaymentService service = service(repository, properties, auditService);
        UUID businessId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        BookingPaymentRecord pending = new BookingPaymentRecord(
                UUID.randomUUID(),
                businessId,
                bookingId,
                "SIMULATED",
                null,
                "checkout-key",
                BigDecimal.valueOf(10000),
                "CLP",
                "PENDING",
                null,
                null,
                null,
                null,
                "http://localhost/pay/1",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
                false,
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(31));
        BookingPaymentRecord expired = new BookingPaymentRecord(
                pending.id(),
                businessId,
                bookingId,
                "SIMULATED",
                null,
                "checkout-key",
                BigDecimal.valueOf(10000),
                "CLP",
                "EXPIRED",
                null,
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "http://localhost/pay/1",
                pending.checkoutExpiresAt(),
                false,
                pending.createdAt());
        when(repository.findExpiredPendingCheckouts(any(), eq(25))).thenReturn(List.of(pending));
        when(repository.updatePaymentStatus(eq(pending.id()), eq("EXPIRED"), any(), any(), any())).thenReturn(expired);

        service.expireDuePaymentLinks();

        verify(repository).recalculateBookingPaymentStatus(businessId, bookingId);
        verify(auditService).record(eq(businessId), eq(null), eq("BOOKING_PAYMENT_LINK_EXPIRED"), eq("BOOKING"), eq(bookingId), any(), any());
    }

    private static BookingPaymentRecord payment(UUID paymentId, UUID businessId, UUID bookingId, String status) {
        return new BookingPaymentRecord(
                paymentId,
                businessId,
                bookingId,
                "MERCADOPAGO",
                "pay-1",
                "idem-1",
                BigDecimal.valueOf(10000),
                "CLP",
                status,
                "APPROVED".equals(status) ? OffsetDateTime.now(ZoneOffset.UTC) : null,
                null,
                null,
                null,
                null,
                null,
                false,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static AuthenticatedUser user(UUID businessId) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                businessId,
                "Negocio",
                "Ada",
                "Lovelace",
                "ada@example.com",
                "America/Santiago",
                List.of("ADMIN"));
    }

    private static BookingPaymentService service(
            BookingPaymentJdbcRepository repository,
            BookingPaymentProperties properties,
            AuditService auditService) {
        BookingPaymentProviderRegistry registry = mock(BookingPaymentProviderRegistry.class);
        BookingPaymentProvider defaultProvider = mock(BookingPaymentProvider.class);
        when(defaultProvider.supportsWebhook()).thenReturn(false);
        when(registry.getDefaultProvider()).thenReturn(defaultProvider);
        return new BookingPaymentService(
                repository,
                properties,
                registry,
                auditService,
                new ObjectMapper(),
                mock(ChannelDispatchService.class),
                mock(BookingEmailService.class),
                mock(CalendarSyncService.class));
    }
}
