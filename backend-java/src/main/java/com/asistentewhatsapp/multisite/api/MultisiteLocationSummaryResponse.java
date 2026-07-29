package com.asistentewhatsapp.multisite.api;

import java.util.UUID;

public record MultisiteLocationSummaryResponse(UUID locationId, String locationCode, String locationName,
		boolean active, long conversations, long leads, long bookings, long orders, long productsWithStock,
		long professionals) {
}
