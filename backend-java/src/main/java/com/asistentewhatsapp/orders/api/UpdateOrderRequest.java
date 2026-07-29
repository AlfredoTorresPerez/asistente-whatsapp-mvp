package com.asistentewhatsapp.orders.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateOrderRequest(String status, @DecimalMin("0.00") BigDecimal discountAmount, LocalDate dueDate,
		@Size(max = 2000) String notes, @Valid List<CreateOrderItemRequest> items) {
}
