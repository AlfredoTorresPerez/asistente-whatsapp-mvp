package com.asistentewhatsapp.bookings.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record BookingPaymentWebhookRequest(UUID businessId, UUID bookingId, String provider, String providerPaymentId,
		String idempotencyKey, BigDecimal amount, String currency, String status, OffsetDateTime occurredAt,
		Map<String, Object> metadata) {
}
