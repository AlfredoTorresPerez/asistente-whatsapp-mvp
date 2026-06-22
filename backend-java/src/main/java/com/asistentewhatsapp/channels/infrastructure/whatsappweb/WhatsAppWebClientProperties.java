package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels.whatsapp-web")
public record WhatsAppWebClientProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String defaultPhoneNumber,
        String webhookSecret,
        long webhookToleranceSeconds,
        boolean demoFallbackEnabled) {
}
