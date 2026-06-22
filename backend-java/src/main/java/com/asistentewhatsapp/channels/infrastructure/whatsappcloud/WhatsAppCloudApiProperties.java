package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels.whatsapp-cloud-api")
public record WhatsAppCloudApiProperties(
        boolean enabled,
        String baseUrl,
        String apiVersion,
        String phoneNumberId,
        String accessToken,
        String defaultPhoneNumber,
        boolean dryRunEnabled) {
}
