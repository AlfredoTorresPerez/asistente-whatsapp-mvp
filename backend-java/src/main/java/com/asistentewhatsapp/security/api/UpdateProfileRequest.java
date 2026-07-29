package com.asistentewhatsapp.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank(message = "El nombre es obligatorio.") @Size(max = 80, message = "El nombre no puede superar 80 caracteres.") String firstName,
		@NotBlank(message = "El apellido es obligatorio.") @Size(max = 80, message = "El apellido no puede superar 80 caracteres.") String lastName,
		@Pattern(regexp = "^$|^\\+[1-9]\\d{7,14}$", message = "Ingresa un telefono valido en formato internacional.") String phone,
		@NotBlank(message = "La zona horaria es obligatoria.") @Size(max = 60, message = "La zona horaria no puede superar 60 caracteres.") String timezone) {
}
