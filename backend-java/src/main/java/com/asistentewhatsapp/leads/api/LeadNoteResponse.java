package com.asistentewhatsapp.leads.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LeadNoteResponse(
        UUID id,
        UUID authorUserId,
        String authorUserName,
        String noteText,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
