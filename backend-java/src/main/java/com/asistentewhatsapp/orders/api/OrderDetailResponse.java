package com.asistentewhatsapp.orders.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerName,
        String customerPhone,
        UUID leadId,
        UUID conversationId,
        String status,
        String paymentStatus,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        String currency,
        LocalDate dueDate,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderItemResponse> items,
        List<OrderPaymentResponse> payments,
        String receiptPreview) {
}
