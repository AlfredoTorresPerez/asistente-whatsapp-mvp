package com.asistentewhatsapp.aesthetic.infrastructure.openai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAiIntentProperties.class)
public class OpenAiIntentConfiguration {
}
