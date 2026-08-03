package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record RoomRequest(@NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 140) String name,
		@NotBlank @Size(max = 80) String roomType, Integer capacity, @Size(max = 4000) String description,
		@Size(max = 7) @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Selecciona un color valido.") String color,
		@Size(max = 4000) String notes, Boolean active, UUID locationId, List<UUID> locationIds) {

	public List<UUID> effectiveLocationIds() {
		if (locationIds != null && !locationIds.isEmpty()) {
			return locationIds;
		}
		if (locationId != null) {
			return List.of(locationId);
		}
		return List.of();
	}
}
