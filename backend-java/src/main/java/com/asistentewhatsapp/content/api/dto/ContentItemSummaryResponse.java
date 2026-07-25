package com.asistentewhatsapp.content.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContentItemSummaryResponse(
        UUID id,
        String type,
        String typeLabel,
        String imagePath,
        String imageUrl,
        String textPreview,
        String status,
        OffsetDateTime updatedAt
) {
}