package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingRescheduleResponse(
        UUID bookingId,
        String bookingStatus,
        String linkStatus,
        String subject,
        String serviceName,
        String currentLocationName,
        String proposedLocationName,
        String currentProfessionalName,
        String proposedProfessionalName,
        String currentRoomName,
        String proposedRoomName,
        OffsetDateTime currentStartsAt,
        OffsetDateTime proposedStartsAt,
        OffsetDateTime proposedEndsAt,
        String customerName,
        String maskedCustomerPhone,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        String reason) {
}
