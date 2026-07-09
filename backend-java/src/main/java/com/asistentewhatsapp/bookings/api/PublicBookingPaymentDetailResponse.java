package com.asistentewhatsapp.bookings.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingPaymentDetailResponse(
        UUID id,
        UUID bookingId,
        String provider,
        String providerPaymentId,
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
        OffsetDateTime createdAt,
        String bookingStatus,
        String bookingPaymentStatus,
        String subject,
        String serviceName,
        String professionalName,
        String roomName,
        OffsetDateTime startsAt,
        int durationMinutes,
        String locationName,
        String customerName) {
}
