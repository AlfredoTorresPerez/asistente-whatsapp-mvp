package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Map;

public record CreateBookingPaymentLinkRequest(String provider,
		@DecimalMin(value = "0.0", inclusive = false) BigDecimal amount, String currency,
		@Min(5) Integer expirationMinutes, Boolean sendWhatsApp, Boolean sendEmail, Map<String, Object> metadata) {
}
