package com.asistentewhatsapp.content.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContentItemDetailResponse(UUID id, String type, String typeLabel, String text, String status,
		String imageUrl, String imagePath, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
		UUID updatedBy, long version) {
}