package com.asistentewhatsapp.leads.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddLeadNoteRequest(
        @NotBlank(message = "noteText es obligatorio")
        @Size(max = 2000, message = "noteText no puede superar 2000 caracteres")
        String noteText) {
}
