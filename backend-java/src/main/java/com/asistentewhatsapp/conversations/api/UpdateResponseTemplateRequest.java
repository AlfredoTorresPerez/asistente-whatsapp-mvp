package com.asistentewhatsapp.conversations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResponseTemplateRequest(
		@NotBlank(message = "name es obligatorio") @Size(max = 120, message = "name no puede superar 120 caracteres") String name,
		@NotBlank(message = "category es obligatoria") @Size(max = 50, message = "category no puede superar 50 caracteres") String category,
		@NotBlank(message = "body es obligatorio") @Size(max = 4000, message = "body no puede superar 4000 caracteres") String body) {
}
