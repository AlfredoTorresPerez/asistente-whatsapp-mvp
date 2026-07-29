package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanySettingsRequest(
		@NotBlank(message = "Ingresa la razon social de la empresa.") @Size(max = 150, message = "La razon social no puede superar los 150 caracteres.") String companyName,
		@NotBlank(message = "Ingresa el nombre comercial.") @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres.") String businessName,
		@NotBlank(message = "Selecciona una zona horaria.") @Size(max = 60, message = "La zona horaria no puede superar los 60 caracteres.") String timezone,
		@NotBlank(message = "Selecciona una moneda.") @Size(min = 3, max = 3, message = "La moneda debe tener exactamente 3 caracteres.") String currency,
		@NotBlank(message = "Ingresa un correo de contacto.") @Email(message = "Ingresa un correo de contacto valido.") @Size(max = 255, message = "El correo de contacto no puede superar los 255 caracteres.") String contactEmail,
		@Pattern(regexp = "^$|^\\+?[1-9]\\d{7,14}$", message = "Ingresa un telefono de soporte valido en formato internacional.") @Size(max = 30, message = "El telefono de soporte no puede superar los 30 caracteres.") String supportPhone,
		@Size(max = 255, message = "La direccion no puede superar los 255 caracteres.") String address) {
}
