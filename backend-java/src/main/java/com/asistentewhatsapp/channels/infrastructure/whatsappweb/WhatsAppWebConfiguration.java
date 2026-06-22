package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WhatsAppWebClientProperties.class)
public class WhatsAppWebConfiguration {
}
