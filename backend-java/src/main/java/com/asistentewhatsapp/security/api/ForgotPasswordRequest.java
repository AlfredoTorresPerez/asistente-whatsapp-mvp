package com.asistentewhatsapp.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "Ingresa un correo valido.")
        String email) {
}

