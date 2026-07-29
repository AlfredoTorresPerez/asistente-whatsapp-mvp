package com.asistentewhatsapp.multisite.api;

import java.time.LocalTime;
import java.util.UUID;

public record ProfessionalScheduleResponse(UUID id, UUID professionalId, String professionalName, UUID locationId,
		String locationName, int dayOfWeek, LocalTime startTime, LocalTime endTime, boolean active) {
}
