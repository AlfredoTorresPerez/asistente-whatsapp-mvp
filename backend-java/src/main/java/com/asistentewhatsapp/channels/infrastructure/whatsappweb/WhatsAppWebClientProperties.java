package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "app.channels.whatsapp-web")
public record WhatsAppWebClientProperties(boolean enabled, String baseUrl, String apiKey, String defaultPhoneNumber,
		String webhookSecret, long webhookToleranceSeconds, boolean demoFallbackEnabled,
		Map<String, String> testPhoneMap, boolean logRawPayload, int qrTimeoutSeconds) {

	public WhatsAppWebClientProperties {
		if (testPhoneMap == null) {
			testPhoneMap = Map.of();
		}
	}
}
