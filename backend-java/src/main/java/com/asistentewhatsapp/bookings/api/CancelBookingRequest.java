package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
		@NotBlank(message = "El motivo de cancelacion es obligatorio.") @Size(max = 2000, message = "reason no puede superar 2000 caracteres") String reason) {
}
