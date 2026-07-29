package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingPublicActionLinkResponse(UUID id, UUID bookingId, String type, String status, String publicUrl,
		OffsetDateTime expiresAt, OffsetDateTime whatsappSentAt, OffsetDateTime emailSentAt) {
}
