package com.asistentewhatsapp.configuration.api;

import jakarta.validation.constraints.NotNull;

public record WhatsAppConfigurationPreferencesRequest(
        @NotNull Boolean newMessageNotifications,
        @NotNull Boolean autoReassignment,
        @NotNull Boolean agentSignature,
        @NotNull Boolean outOfHoursMessage) {
}
