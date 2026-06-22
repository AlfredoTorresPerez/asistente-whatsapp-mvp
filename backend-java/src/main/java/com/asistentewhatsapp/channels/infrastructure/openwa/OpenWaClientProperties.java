package com.asistentewhatsapp.channels.infrastructure.openwa;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels.openwa")
public record OpenWaClientProperties(
        boolean enabled,
        String baseUrl,
        String apiKey) {
}
