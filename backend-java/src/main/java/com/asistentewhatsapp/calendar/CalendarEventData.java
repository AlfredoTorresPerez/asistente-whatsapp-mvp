package com.asistentewhatsapp.calendar;

import java.time.OffsetDateTime;

public record CalendarEventData(
    String summary,
    String description,
    String location,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    String timezone,
    String attendeeEmail,
    String attendeeName) {
}
