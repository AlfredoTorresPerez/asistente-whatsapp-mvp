package com.asistentewhatsapp.customerbookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerBookingItemResponse(UUID bookingId, UUID locationId, UUID serviceId, UUID professionalId,
		UUID roomId, String serviceName, String locationName, String professionalName, OffsetDateTime startsAt,
		OffsetDateTime endsAt, int durationMinutes, String status, String customerName, String maskedPhone) {
}
