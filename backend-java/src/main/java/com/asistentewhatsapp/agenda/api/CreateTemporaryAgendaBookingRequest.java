package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTemporaryAgendaBookingRequest(@NotNull(message = "locationId es obligatorio") UUID locationId,
		@NotNull(message = "serviceId es obligatorio") UUID serviceId, UUID professionalId, UUID roomId,
		@NotNull(message = "startsAt es obligatorio") OffsetDateTime startsAt,
		@NotBlank(message = "customerName es obligatorio") @Size(max = 160) String customerName,
		@NotBlank(message = "customerPhone es obligatorio") @Size(max = 30) String customerPhone,
		@Email(message = "customerEmail debe tener formato valido") @Size(max = 255) String customerEmail,
		UUID customerId, UUID conversationId, UUID leadId, @Size(max = 2000) String notes, Integer expirationMinutes,
		Boolean generateConfirmationLink, Boolean sendWhatsApp, @Size(max = 255) String idempotencyKey,
		Boolean communicationsConsent, @Size(max = 30) String sourceChannel, Boolean informedConsentAccepted,
		LocalDate customerBirthDate, @Size(max = 160) String guardianName, @Size(max = 30) String guardianPhone) {

	public CreateTemporaryAgendaBookingRequest(UUID locationId, UUID serviceId, UUID professionalId, UUID roomId,
			OffsetDateTime startsAt, String customerName, String customerPhone, String customerEmail, UUID customerId,
			UUID conversationId, UUID leadId, String notes, Integer expirationMinutes, Boolean generateConfirmationLink,
			Boolean sendWhatsApp, String idempotencyKey, Boolean informedConsentAccepted, LocalDate customerBirthDate,
			String guardianName, String guardianPhone) {
		this(locationId, serviceId, professionalId, roomId, startsAt, customerName, customerPhone, customerEmail,
				customerId, conversationId, leadId, notes, expirationMinutes, generateConfirmationLink, sendWhatsApp,
				idempotencyKey, null, null, informedConsentAccepted, customerBirthDate, guardianName, guardianPhone);
	}
}
