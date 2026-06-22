package com.asistentewhatsapp.bookings.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingConfirmationResponse(
        UUID bookingId,
        String bookingStatus,
        String linkStatus,
        String subject,
        String serviceName,
        String professionalName,
        String roomName,
        OffsetDateTime startsAt,
        int durationMinutes,
        UUID locationId,
        String location,
        String locationName,
        String customerName,
        String maskedCustomerPhone,
        boolean requiresDeposit,
        BigDecimal depositAmount,
        String paymentStatus,
        OffsetDateTime expiresAt,
        OffsetDateTime confirmedAt) {
}
