package com.asistentewhatsapp.aesthetic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record IntentAnalysisRequest(UUID customerId, UUID conversationId,
		@NotBlank(message = "message es obligatorio") @Size(max = 4000, message = "message no debe superar 4000 caracteres") String message) {
}
