package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.asistentewhatsapp.customerbookings.api.CustomerBookingItemResponse;

public record PublicBookingRescheduleResponse(UUID bookingId, String bookingStatus, String linkStatus, String subject,
		String serviceName, String currentLocationName, String proposedLocationName, String currentProfessionalName,
		String proposedProfessionalName, String currentRoomName, String proposedRoomName,
		OffsetDateTime currentStartsAt, OffsetDateTime proposedStartsAt, OffsetDateTime proposedEndsAt,
		String customerName, String maskedCustomerPhone, OffsetDateTime expiresAt, OffsetDateTime usedAt, String reason,
		List<CustomerBookingItemResponse> bookings, List<ServiceOption> services, List<LocationOption> locations) {

	public record ServiceOption(UUID id, String name, String categoryName, int durationMinutes, boolean requiresRoom) {
	}

	public record LocationOption(UUID id, String name, String address, String commune) {
	}
}
