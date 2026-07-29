package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record RegisterBookingManualPaymentRequest(String provider, String providerPaymentId, String idempotencyKey,
		@DecimalMin(value = "0.0", inclusive = false) BigDecimal amount, String currency, String status,
		OffsetDateTime occurredAt, String notes, Map<String, Object> metadata) {
}
