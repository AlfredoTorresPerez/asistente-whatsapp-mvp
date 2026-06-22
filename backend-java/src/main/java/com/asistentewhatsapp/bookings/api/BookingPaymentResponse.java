package com.asistentewhatsapp.bookings.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingPaymentResponse(
        UUID id,
        UUID bookingId,
        String provider,
        String providerPaymentId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String status,
        String checkoutUrl,
        OffsetDateTime checkoutExpiresAt,
        boolean manual,
        OffsetDateTime approvedAt,
        OffsetDateTime rejectedAt,
        OffsetDateTime expiredAt,
        OffsetDateTime refundedAt,
        OffsetDateTime createdAt) {
}

