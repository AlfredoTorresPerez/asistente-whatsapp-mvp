package com.asistentewhatsapp.bookings.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingRequest(
		@NotBlank(message = "subject es obligatorio") @Size(max = 160, message = "subject no puede superar 160 caracteres") String subject,
		UUID customerId,
		@Size(max = 80, message = "customerFirstName no puede superar 80 caracteres") String customerFirstName,
		@Size(max = 80, message = "customerLastName no puede superar 80 caracteres") String customerLastName,
		@Size(max = 160, message = "customerName no puede superar 160 caracteres") String customerName,
		@Size(max = 30, message = "customerPhone no puede superar 30 caracteres") String customerPhone,
		@Email(message = "customerEmail debe tener un formato valido") @Size(max = 255, message = "customerEmail no puede superar 255 caracteres") String customerEmail,
		@Size(max = 30, message = "status no puede superar 30 caracteres") String status, UUID assignedUserId,
		@NotNull(message = "startsAt es obligatorio") OffsetDateTime startsAt, Integer durationMinutes, UUID locationId,
		@Size(max = 160, message = "location no puede superar 160 caracteres") String location,
		@Size(max = 2000, message = "notes no puede superar 2000 caracteres") String notes) {
}
