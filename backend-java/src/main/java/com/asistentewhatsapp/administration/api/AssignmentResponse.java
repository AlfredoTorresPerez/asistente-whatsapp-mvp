package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentResponse(UUID id, UUID serviceId, String serviceName, String serviceCode, UUID professionalId,
		String professionalName, UUID roomId, String roomName, String roomCode, String assignmentType, boolean active,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
