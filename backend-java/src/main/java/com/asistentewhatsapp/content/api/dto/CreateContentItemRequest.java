package com.asistentewhatsapp.content.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContentItemRequest(
        @NotNull(message = "El tipo es obligatorio")
        @NotBlank(message = "El tipo es obligatorio")
        String type,

        @NotNull(message = "El texto es obligatorio")
        @NotBlank(message = "El texto no puede estar vacio")
        @Size(max = 200, message = "El texto no puede superar 200 caracteres")
        String text,

        @NotNull(message = "El estado es obligatorio")
        @NotBlank(message = "El estado es obligatorio")
        String status
) {
}