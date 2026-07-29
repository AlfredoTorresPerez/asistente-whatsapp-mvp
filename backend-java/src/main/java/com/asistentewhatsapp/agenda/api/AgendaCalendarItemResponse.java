package com.asistentewhatsapp.agenda.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaCalendarItemResponse(UUID bookingId, String subject, String status, OffsetDateTime startsAt,
		OffsetDateTime endsAt, int durationMinutes, UUID locationId, String locationName, UUID serviceId,
		String serviceName, UUID professionalId, String professionalName, UUID roomId, String roomName,
		String customerName, String customerPhone, String sourceChannel, String startsAtLocal, String endsAtLocal,
		String dateLocal, String startTimeLocal, String endTimeLocal, String timezone, String type) {
}
