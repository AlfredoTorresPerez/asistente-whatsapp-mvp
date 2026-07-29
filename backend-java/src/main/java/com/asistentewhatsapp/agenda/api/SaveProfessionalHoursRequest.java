package com.asistentewhatsapp.agenda.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SaveProfessionalHoursRequest(@NotNull UUID locationId, @NotNull UUID professionalId,
		@NotEmpty @Valid List<ProfessionalHourEntry> hours) {
	public record ProfessionalHourEntry(@NotNull Integer dayOfWeek, @NotNull LocalTime startTime,
			@NotNull LocalTime endTime) {
	}
}
