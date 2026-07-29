package com.asistentewhatsapp.bookings.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingDetailResponse(UUID id, String subject, String status, OffsetDateTime startsAt,
		int durationMinutes, UUID locationId, String location, String locationName, String notes,
		OffsetDateTime completedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID customerId,
		String customerName, String customerPhone, String customerEmail, UUID leadId, UUID conversationId,
		UUID assignedUserId, String assignedUserName, boolean requiresDeposit, BigDecimal depositAmount,
		String paymentStatus, List<BookingStatusHistoryResponse> statusHistory,
		List<BookingPublicLinkSummaryResponse> publicLinks, List<BookingReminderResponse> reminders,
		List<BookingEmailLogResponse> emailLogs, List<BookingPaymentResponse> payments) {
}
