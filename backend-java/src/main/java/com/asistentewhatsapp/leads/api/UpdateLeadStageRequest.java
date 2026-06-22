package com.asistentewhatsapp.leads.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLeadStageRequest(
        @NotBlank(message = "stage es obligatorio")
        @Size(max = 20, message = "stage no puede superar 20 caracteres")
        String stage) {
}
