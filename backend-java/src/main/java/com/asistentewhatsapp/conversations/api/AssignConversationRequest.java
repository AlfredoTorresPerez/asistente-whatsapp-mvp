package com.asistentewhatsapp.conversations.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignConversationRequest(
        @NotNull(message = "userId es obligatorio")
        UUID userId) {
}
