package com.asistentewhatsapp.calendar.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CalendarAccountResponse(
    UUID id,
    String provider,
    String emailMasked,
    String calendarId,
    String calendarSummary,
    boolean active,
    OffsetDateTime connectedAt,
    OffsetDateTime lastSyncAt,
    boolean requiresReconnect,
    OffsetDateTime revokedAt,
    String authorizationStatus) {

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return email;
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 4) {
            return localPart.charAt(0) + "***" + domain;
        }
        return localPart.substring(0, 2) + "***" + localPart.substring(localPart.length() - 2) + domain;
    }

    public static String determineAuthorizationStatus(boolean active, boolean requiresReconnect, OffsetDateTime revokedAt) {
        if (revokedAt != null) return "REVOKED";
        if (!active) return "DISCONNECTED";
        if (requiresReconnect) return "RECONNECT_NEEDED";
        return "CONNECTED";
    }
}
