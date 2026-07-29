package com.asistentewhatsapp.landing.api;

import java.math.BigDecimal;
import java.util.UUID;

public record LandingLocationItemResponse(UUID id, String name, String address, String city, String commune,
		String phone, String whatsappNumber, String timezone, BigDecimal latitude, BigDecimal longitude,
		Integer dailyBookingCapacity, Double distanceKm, boolean preferred) {
}
