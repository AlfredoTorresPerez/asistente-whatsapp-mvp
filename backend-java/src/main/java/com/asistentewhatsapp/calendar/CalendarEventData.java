package com.asistentewhatsapp.calendar;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CalendarEventData(
    String summary,
    String description,
    String location,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    String timezone,
    String attendeeEmail,
    String attendeeName,
    UUID businessId,
    UUID bookingId,
    String googleEventId) {

    public CalendarEventData {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
    }
}
