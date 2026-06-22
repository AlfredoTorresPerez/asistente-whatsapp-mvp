package com.asistentewhatsapp.conversations.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateConversationRequest(
        UUID customerId,
        @Size(max = 160, message = "customerName no puede superar 160 caracteres")
        String customerName,
        @Size(max = 30, message = "customerPhone no puede superar 30 caracteres")
        String customerPhone,
        @Email(message = "customerEmail debe ser un correo valido")
        @Size(max = 255, message = "customerEmail no puede superar 255 caracteres")
        String customerEmail,
        UUID ownerUserId,
        @Size(max = 1000, message = "initialMessage no puede superar 1000 caracteres")
        String initialMessage) {
}
