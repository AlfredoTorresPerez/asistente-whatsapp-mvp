package com.asistentewhatsapp.conversations.api;

import jakarta.validation.constraints.NotNull;

public record UpdateTemplateStatusRequest(@NotNull(message = "active es obligatorio") Boolean active) {
}
