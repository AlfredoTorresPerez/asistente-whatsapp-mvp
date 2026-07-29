package com.asistentewhatsapp.configuration.api;

import jakarta.validation.constraints.Size;

public record WhatsAppChannelUpdateRequest(@Size(max = 30) String displayPhoneNumber,
		@Size(max = 30) String normalizedPhoneNumber, @Size(max = 120) String phoneNumberId,
		@Size(max = 120) String businessAccountId, @Size(max = 20) String graphApiVersion,
		@Size(max = 255) String webhookCallbackUrl) {
}
