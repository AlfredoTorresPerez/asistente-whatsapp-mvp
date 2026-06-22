package com.asistentewhatsapp.agenda.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AgendaAvailabilityResponse(
        UUID locationId,
        String locationName,
        UUID serviceId,
        String serviceName,
        LocalDate date,
        int durationMinutes,
        boolean requiresRoom,
        boolean requiresDeposit,
        List<AgendaSlotResponse> slots) {
}
