package com.asistentewhatsapp.bookings.api;

import java.util.UUID;

public record BookingPaymentWebhookResponse(UUID paymentId, UUID bookingId, String paymentStatus, String bookingStatus,
		boolean duplicate, boolean bookingConfirmed) {
}
