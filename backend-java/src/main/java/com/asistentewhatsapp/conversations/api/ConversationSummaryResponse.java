package com.asistentewhatsapp.conversations.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationSummaryResponse(UUID id, String customerName, String customerPhone, String status,
		int unreadCount, String lastMessagePreview, OffsetDateTime lastMessageAt, String channelType,
		UUID assignedUserId, String assignedUserName, UUID prospectId, UUID locationId, String locationName) {
}
