package com.asistentewhatsapp.multisite.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.UUID;

public record UpsertProfessionalScheduleRequest(
        @NotNull UUID professionalId,
        @NotNull UUID locationId,
        @Min(1) @Max(7) int dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Boolean active) {
}
