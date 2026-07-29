package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicBookingCancellationResponse(UUID bookingId, String bookingStatus, String linkStatus, String subject,
		String serviceName, String locationName, String professionalName, String roomName, OffsetDateTime startsAt,
		OffsetDateTime endsAt, String customerName, String maskedCustomerPhone, OffsetDateTime expiresAt,
		OffsetDateTime usedAt, String cancellationReason) {
}
