package com.asistentewhatsapp.landing.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicServiceBranchResponse(UUID id, String name, String address, String commune, String phone,
		int professionalCount, BigDecimal latitude, BigDecimal longitude, Integer dailyBookingCapacity,
		Double distanceKm, boolean preferred) {
}
