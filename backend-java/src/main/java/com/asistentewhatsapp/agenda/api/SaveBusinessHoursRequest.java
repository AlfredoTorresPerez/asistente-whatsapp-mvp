package com.asistentewhatsapp.agenda.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SaveBusinessHoursRequest(@NotNull UUID locationId, @NotEmpty @Valid List<BusinessHourEntry> hours) {
	public record BusinessHourEntry(@NotNull Integer dayOfWeek, @NotNull LocalTime startTime,
			@NotNull LocalTime endTime) {
	}
}
