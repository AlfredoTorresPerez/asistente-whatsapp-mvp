package com.asistentewhatsapp.bookings.api;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

public record BookingSummaryResponse(UUID id, String subject, String status, OffsetDateTime startsAt,
		int durationMinutes, UUID locationId, String location, String locationName, UUID customerId,
		String customerName, String customerPhone, UUID leadId, UUID conversationId, UUID assignedUserId,
		String assignedUserName, boolean requiresDeposit, BigDecimal depositAmount, String paymentStatus,
		String calendarSyncStatus) {
}
