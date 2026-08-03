package com.asistentewhatsapp.customers.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerSearchResponse(UUID id, String firstName, String lastName, String displayName, String phone,
		String normalizedPhone, String email, OffsetDateTime createdAt) {
}
