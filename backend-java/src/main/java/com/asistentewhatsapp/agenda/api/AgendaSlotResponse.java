package com.asistentewhatsapp.agenda.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaSlotResponse(OffsetDateTime startsAt, OffsetDateTime endsAt, UUID locationId, String locationName,
		UUID serviceId, String serviceName, int durationMinutes, UUID professionalId, String professionalName,
		UUID roomId, String roomName, boolean available, String reason) {
}
