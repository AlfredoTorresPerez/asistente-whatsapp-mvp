package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingEmailLogResponse(UUID id, String recipientEmail, String subject, String templateKey, String status,
		boolean simulation, String failureReason, OffsetDateTime sentAt, OffsetDateTime createdAt) {
}
