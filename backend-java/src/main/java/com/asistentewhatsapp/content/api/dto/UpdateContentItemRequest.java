package com.asistentewhatsapp.content.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContentItemRequest(@NotBlank(message = "El tipo es obligatorio") String type,

		@NotBlank(message = "El texto no puede estar vacio") @Size(max = 200, message = "El texto no puede superar 200 caracteres") String text,

		@NotBlank(message = "El estado es obligatorio") String status) {
}