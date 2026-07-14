package com.asistentewhatsapp.reports.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportsProspectRowResponse(
        UUID id,
        String name,
        String phone,
        OffsetDateTime lastContact,
        String stage,
        String responsible,
        OffsetDateTime nextAppointment,
        String location,
        String serviceInterest,
        String attentionStatus) {
}
