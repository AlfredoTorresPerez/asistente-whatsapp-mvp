package com.asistentewhatsapp.leads.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LeadSummaryResponse(UUID id, UUID customerId, UUID conversationId, String firstName, String lastName,
		String displayName, String phone, String email, String stage, String sourceType, UUID assignedUserId,
		String assignedUserName, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
