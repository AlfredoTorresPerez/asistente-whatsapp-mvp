package com.asistentewhatsapp.configuration.api;

import java.time.OffsetDateTime;
import java.util.List;

public record WhatsAppConfigurationResponse(String sessionStatus, String phoneNumber, String businessName,
		OffsetDateTime lastSynchronizationAt, long activeSessionHours, String connectedFrom, String qrCode,
		boolean adapterReachable, String adapterMode, String warningMessage,
		WhatsAppConfigurationPreferencesResponse preferences, WhatsAppConfigurationChannelResponse mainChannel,
		List<WhatsAppConfigurationLinkedDeviceResponse> linkedDevices,
		List<WhatsAppConfigurationSessionHistoryResponse> sessionHistory) {
}
