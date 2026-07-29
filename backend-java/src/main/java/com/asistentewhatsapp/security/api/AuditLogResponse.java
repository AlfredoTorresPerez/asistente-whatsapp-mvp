package com.asistentewhatsapp.security.api;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID actorUserId, String actionType, String entityType, UUID entityId,
		String summary, Map<String, Object> metadata, OffsetDateTime occurredAt) {
}
