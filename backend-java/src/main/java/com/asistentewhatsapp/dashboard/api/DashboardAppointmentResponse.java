package com.asistentewhatsapp.dashboard.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DashboardAppointmentResponse(
        UUID id,
        String subject,
        String status,
        String customerName,
        OffsetDateTime startsAt,
        int durationMinutes,
        String location) {
}
