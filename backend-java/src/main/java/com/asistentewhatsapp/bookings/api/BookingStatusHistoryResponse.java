package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingStatusHistoryResponse(UUID id, String previousStatus, String newStatus, String reason,
		UUID actorUserId, String source, OffsetDateTime createdAt) {
}
