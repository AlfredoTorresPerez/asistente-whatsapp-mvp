package com.asistentewhatsapp.reports.api;

public record ReportsChannelResponse(
        String channel,
        long count,
        double percentage) {
}
