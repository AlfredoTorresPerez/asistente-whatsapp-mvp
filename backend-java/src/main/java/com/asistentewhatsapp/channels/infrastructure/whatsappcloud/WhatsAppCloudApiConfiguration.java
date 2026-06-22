package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WhatsAppCloudApiProperties.class)
public class WhatsAppCloudApiConfiguration {
}
