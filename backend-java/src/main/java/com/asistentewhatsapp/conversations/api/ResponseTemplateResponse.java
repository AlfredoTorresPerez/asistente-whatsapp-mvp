package com.asistentewhatsapp.conversations.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResponseTemplateResponse(UUID id, String name, String category, String body, boolean active,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
