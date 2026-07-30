package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomResponse(UUID id, UUID locationId, String locationName, String code, String name, String roomType,
		int capacity, String description, String color, String notes, boolean active, OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
