package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 2000, message = "reason no puede superar 2000 caracteres")
        String reason) {
}
