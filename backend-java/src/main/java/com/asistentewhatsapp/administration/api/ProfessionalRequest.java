package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ProfessionalRequest(@NotBlank @Size(max = 160) String fullName, @Size(max = 160) String displayName,
		@Size(max = 120) String specialty, @Size(max = 255) String email, @Size(max = 30) String phone,
		@Size(max = 4000) String description, @Size(max = 7) String color, Integer maxDailyBookings,
		Integer qualificationLevel, @Size(max = 120) String certificationRef, Boolean active, List<UUID> locationIds) {
}
