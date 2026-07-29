package com.asistentewhatsapp.bookings.api;

public record CreateBookingCancellationLinkRequest(String reason, Integer expirationMinutes, Boolean sendWhatsApp,
		Boolean sendEmail) {
}
