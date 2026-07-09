package com.asistentewhatsapp.bookings.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BookingPaymentProvider {

    String providerName();

    CreateCheckoutResult createCheckout(
            UUID businessId,
            UUID bookingId,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String description,
            String returnUrl,
            String notificationUrl,
            int expirationMinutes);

    Optional<PaymentNotification> parseWebhook(String rawBody, Map<String, String> headers);

    RefundResult refund(
            UUID businessId,
            UUID bookingId,
            UUID paymentId,
            String providerPaymentId,
            BigDecimal amount,
            String reason);

    boolean supportsWebhook();

    record CreateCheckoutResult(
            String checkoutUrl,
            String providerPaymentId,
            String providerPreferenceId,
            String providerExternalReference,
            Map<String, Object> metadata,
            OffsetDateTime expiresAt) {
    }

    record PaymentNotification(
            String providerPaymentId,
            String providerPreferenceId,
            String providerExternalReference,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            String statusDetail,
            String rawStatus,
            String paymentMethodId,
            Integer installments,
            String payerEmail,
            OffsetDateTime occurredAt,
            Map<String, Object> metadata) {
    }

    record RefundResult(String providerRefundId, Map<String, Object> metadata) {
    }
}