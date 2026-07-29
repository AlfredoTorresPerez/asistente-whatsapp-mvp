package com.asistentewhatsapp.conversations.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationDetailResponse(UUID id, String status, String channelType, int unreadCount,
		String lastMessagePreview, OffsetDateTime lastMessageAt, OffsetDateTime openedAt, OffsetDateTime closedAt,
		UUID assignedUserId, String assignedUserName, UUID prospectId, UUID locationId, String locationName,
		ConversationCustomerResponse customer, List<ConversationMessageResponse> messages) {
}
