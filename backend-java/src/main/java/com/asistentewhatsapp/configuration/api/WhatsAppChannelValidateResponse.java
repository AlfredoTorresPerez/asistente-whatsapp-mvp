package com.asistentewhatsapp.configuration.api;

import java.util.List;

public record WhatsAppChannelValidateResponse(boolean valid, String providerType, String message,
		List<String> warnings) {
}
