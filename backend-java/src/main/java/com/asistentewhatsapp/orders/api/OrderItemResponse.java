package com.asistentewhatsapp.orders.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID id, UUID productId, String productName, String sku, int quantity,
		BigDecimal unitPrice, BigDecimal lineTotal) {
}
