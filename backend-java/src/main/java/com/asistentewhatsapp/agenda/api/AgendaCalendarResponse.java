package com.asistentewhatsapp.agenda.api;

import java.time.OffsetDateTime;
import java.util.List;

public record AgendaCalendarResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        List<AgendaCalendarItemResponse> items) {
}
