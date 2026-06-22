package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AgendaAvailabilityRequest(
        @NotNull(message = "locationId es obligatorio")
        UUID locationId,
        @NotNull(message = "serviceId es obligatorio")
        UUID serviceId,
        UUID professionalId,
        UUID roomId,
        @NotNull(message = "date es obligatorio")
        LocalDate date,
        @Size(max = 20, message = "preference no puede superar 20 caracteres")
        String preference,
        Integer maxSlots) {
}
