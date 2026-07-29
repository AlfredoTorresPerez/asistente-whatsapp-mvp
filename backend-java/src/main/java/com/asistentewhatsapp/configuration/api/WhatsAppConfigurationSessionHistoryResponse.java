package com.asistentewhatsapp.configuration.api;

import java.time.OffsetDateTime;

public record WhatsAppConfigurationSessionHistoryResponse(String id, String title, String actor, String tone,
		OffsetDateTime occurredAt) {
}
