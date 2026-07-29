package com.asistentewhatsapp.content.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateContentItemStatusRequest(@NotBlank(message = "El estado es obligatorio") String status) {
}