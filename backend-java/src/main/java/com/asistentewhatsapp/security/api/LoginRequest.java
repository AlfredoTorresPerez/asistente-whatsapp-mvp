package com.asistentewhatsapp.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "El correo es obligatorio.") @Email(message = "Ingresa un correo valido.") String email,
		@NotBlank(message = "La contrasena es obligatoria.") @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.") String password) {
}
