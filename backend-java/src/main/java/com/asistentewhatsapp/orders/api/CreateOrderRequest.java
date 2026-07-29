package com.asistentewhatsapp.orders.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(UUID customerId, UUID leadId, UUID conversationId,
		@Size(max = 160) String customerName, @Size(max = 30) String customerPhone,
		@Size(max = 255) String customerEmail, String status, @DecimalMin("0.00") BigDecimal discountAmount,
		LocalDate dueDate, @Size(max = 2000) String notes, @Valid List<CreateOrderItemRequest> items) {
}
