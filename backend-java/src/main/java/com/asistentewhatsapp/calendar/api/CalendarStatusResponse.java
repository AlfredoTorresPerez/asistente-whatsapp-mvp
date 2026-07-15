package com.asistentewhatsapp.calendar.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CalendarStatusResponse(
    UUID accountId,
    String provider,
    String emailMasked,
    String calendarId,
    String calendarSummary,
    boolean active,
    OffsetDateTime connectedAt,
    OffsetDateTime lastSyncAt,
    boolean requiresReconnect,
    String authorizationStatus) {
}
