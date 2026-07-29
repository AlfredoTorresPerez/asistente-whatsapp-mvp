package com.asistentewhatsapp.customerbookings.api;

import java.util.List;
import java.util.UUID;

public record CustomerBookingReschedulePreviewResponse(CustomerBookingItemResponse booking,
		List<ServiceOption> services, List<LocationOption> locations) {

	public record ServiceOption(UUID id, String name, String categoryName, int durationMinutes, boolean requiresRoom) {
	}

	public record LocationOption(UUID id, String name, String address, String commune) {
	}
}
