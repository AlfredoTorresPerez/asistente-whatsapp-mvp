package com.asistentewhatsapp.bookings.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SincronizadorReservaMotorReglas {

	void sincronizarReserva(UUID businessId, UUID bookingId, String customerPhone, String customerName,
			String serviceName, String locationName, String professionalName, OffsetDateTime startsAt,
			int durationMinutes, String bookingStatus, UUID conversationId, String channelOrigin, String originIntent,
			String traceId);
}
