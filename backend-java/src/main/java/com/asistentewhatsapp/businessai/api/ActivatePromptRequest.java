package com.asistentewhatsapp.businessai.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ActivatePromptRequest(@NotNull UUID promptId) {
}
