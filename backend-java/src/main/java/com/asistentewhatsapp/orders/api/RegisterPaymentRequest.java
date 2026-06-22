package com.asistentewhatsapp.orders.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RegisterPaymentRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 30) String method,
        OffsetDateTime paidAt,
        @Size(max = 120) String reference,
        @Size(max = 1000) String notes) {
}
