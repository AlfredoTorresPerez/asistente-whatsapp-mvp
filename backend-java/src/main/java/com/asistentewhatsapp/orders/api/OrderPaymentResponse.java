package com.asistentewhatsapp.orders.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderPaymentResponse(UUID id, BigDecimal amount, String method, OffsetDateTime paidAt, String reference,
		String notes) {
}
