package com.asistentewhatsapp.configuration.api;

public record WhatsAppConfigurationPreferencesResponse(boolean newMessageNotifications, boolean autoReassignment,
		boolean agentSignature, boolean outOfHoursMessage) {
}
