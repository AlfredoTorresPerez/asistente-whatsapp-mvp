package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgendaCancelRequest(
        @NotBlank(message = "reason es obligatorio") @Size(max = 500) String reason) {
}
