package com.asistentewhatsapp.administration.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProfessionalResponse(UUID id, String fullName, String displayName, String specialty, String email,
		String phone, String description, String color, Integer maxDailyBookings, Integer qualificationLevel,
		String certificationRef, LocalDate certificationValidUntil, boolean active, List<UUID> locationIds,
		List<String> locationNames, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
