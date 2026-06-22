package com.asistentewhatsapp.orders.api;

import java.time.Instant;

public record SendOrderSummaryResponse(
        String status,
        String externalMessageId,
        Instant acceptedAt,
        String body) {
}
