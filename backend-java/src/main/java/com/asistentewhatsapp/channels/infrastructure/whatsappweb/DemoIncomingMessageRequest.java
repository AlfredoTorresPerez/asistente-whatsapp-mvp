package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import jakarta.validation.constraints.NotBlank;

public record DemoIncomingMessageRequest(String sessionKey,
		@NotBlank(message = "El telefono de origen es obligatorio.") String from,
		@NotBlank(message = "El mensaje es obligatorio.") String body, String externalMessageId) {
}
