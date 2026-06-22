package com.asistentewhatsapp.administration.api;

import java.util.UUID;

public record AdminSummaryResponse(
        CompanySummary company,
        UsersSummary users,
        WhatsAppWebSummary whatsappWeb,
        SecuritySummary security) {

    public record CompanySummary(UUID id, String companyName) {
    }

    public record UsersSummary(long total, long active) {
    }

    public record WhatsAppWebSummary(String status) {
    }

    public record SecuritySummary(int sessionTimeoutMinutes) {
    }
}
