package com.asistentewhatsapp.conversations.api;

public record ConversationMetricsResponse(long activeConversations, long unattendedConversations, long newProspects,
		long activeOrders) {
}
