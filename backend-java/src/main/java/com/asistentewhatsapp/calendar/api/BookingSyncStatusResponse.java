package com.asistentewhatsapp.calendar.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingSyncStatusResponse(UUID id, UUID bookingId, String provider, String externalEventId,
		String syncStatus, String syncAction, String errorMessage, int retryCount, OffsetDateTime lastSyncAttemptAt,
		OffsetDateTime lastSuccessfulSyncAt) {
}
