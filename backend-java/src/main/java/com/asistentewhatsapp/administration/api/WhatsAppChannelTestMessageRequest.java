package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WhatsAppChannelTestMessageRequest(
		@NotBlank(message = "Ingresa el telefono de destino.") @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Ingresa un telefono valido en formato internacional.") String recipientPhone,
		@NotBlank(message = "Ingresa un mensaje de prueba.") @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres.") String body) {
}
