package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WhatsAppCloudApiProperties.class)
public class WhatsAppCloudApiConfiguration {
}
