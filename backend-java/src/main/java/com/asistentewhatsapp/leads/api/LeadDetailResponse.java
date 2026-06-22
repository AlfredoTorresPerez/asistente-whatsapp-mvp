package com.asistentewhatsapp.leads.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LeadDetailResponse(
        UUID id,
        UUID customerId,
        UUID conversationId,
        String firstName,
        String lastName,
        String displayName,
        String phone,
        String email,
        String stage,
        String sourceType,
        String notes,
        UUID assignedUserId,
        String assignedUserName,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<LeadNoteResponse> noteHistory) {
}
