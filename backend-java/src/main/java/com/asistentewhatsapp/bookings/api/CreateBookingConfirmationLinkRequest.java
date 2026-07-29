package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateBookingConfirmationLinkRequest(
		@Min(value = 5, message = "expirationMinutes debe ser al menos 5") @Max(value = 1440, message = "expirationMinutes no puede superar 1440") Integer expirationMinutes,
		Boolean sendWhatsApp) {
}
