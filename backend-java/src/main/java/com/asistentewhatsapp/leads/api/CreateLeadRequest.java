package com.asistentewhatsapp.leads.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateLeadRequest(
        @NotBlank(message = "firstName es obligatorio")
        @Size(max = 80, message = "firstName no puede superar 80 caracteres")
        String firstName,
        @NotBlank(message = "lastName es obligatorio")
        @Size(max = 80, message = "lastName no puede superar 80 caracteres")
        String lastName,
        @NotBlank(message = "phone es obligatorio")
        @Size(max = 30, message = "phone no puede superar 30 caracteres")
        String phone,
        @Email(message = "email debe tener un formato valido")
        @Size(max = 255, message = "email no puede superar 255 caracteres")
        String email,
        @Size(max = 2000, message = "notes no puede superar 2000 caracteres")
        String notes,
        @Size(max = 20, message = "stage no puede superar 20 caracteres")
        String stage,
        UUID assignedUserId) {
}
