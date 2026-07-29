package com.asistentewhatsapp.content.infrastructure;

import com.asistentewhatsapp.content.ContentItemType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContentItemRecord(UUID id, UUID businessId, ContentItemType type, String imagePath, String text,
		String status, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy,
		long version) {
}