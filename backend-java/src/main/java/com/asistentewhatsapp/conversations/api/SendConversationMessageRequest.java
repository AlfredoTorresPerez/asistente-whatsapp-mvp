package com.asistentewhatsapp.conversations.api;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendConversationMessageRequest(
		@Size(max = 1000, message = "body no puede superar 1000 caracteres") String body, UUID templateId,
		@Size(max = 120, message = "idempotencyKey no puede superar 120 caracteres") String idempotencyKey,
		@Size(max = 160, message = "aiSource no puede superar 160 caracteres") String aiSource) {
}
