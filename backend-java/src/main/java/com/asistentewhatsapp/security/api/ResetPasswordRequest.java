package com.asistentewhatsapp.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El token es obligatorio.")
        String token,
        @NotBlank(message = "La nueva contrasena es obligatoria.")
        @Size(min = 8, max = 72, message = "La nueva contrasena debe tener entre 8 y 72 caracteres.")
        String newPassword,
        @NotBlank(message = "Debes confirmar la nueva contrasena.")
        String confirmPassword) {
}

