package com.asistentewhatsapp.configuration.api;

import java.time.OffsetDateTime;

public record WhatsAppConfigurationLinkedDeviceResponse(String id, String deviceName, String operatorName,
		String location, String browser, String status, OffsetDateTime lastActivityAt) {
}
