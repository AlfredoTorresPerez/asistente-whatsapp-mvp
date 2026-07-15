package com.asistentewhatsapp.shared.config;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EmailConfigValidator implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConfigValidator.class);

    private final Environment environment;

    public EmailConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean emailEnabled = environment.getProperty("app.email.enabled", boolean.class, false);
        if (!emailEnabled) {
            return;
        }

        String host = environment.getProperty("spring.mail.host", "");
        if (host == null || host.isBlank()) {
            LOGGER.warn("Email habilitado pero spring.mail.host está vacío. Usando simulación.");
            return;
        }

        if (isPlaceholder(host) || isLocalhost(host)) {
            String env = environment.getProperty("app.environment", "local").toLowerCase(Locale.ROOT);
            if (!"local".equals(env) && !"dev".equals(env) && !"development".equals(env) && !"demo".equals(env) && !"test".equals(env)) {
                throw new IllegalStateException(
                        "spring.mail.host=" + host + " no es válido para entorno " + env
                                + ". Configure un servidor SMTP real en SPRING_MAIL_HOST.");
            }
        }
    }

    private boolean isPlaceholder(String value) {
        return value.startsWith("${") || value.contains("change-this") || value.contains("placeholder");
    }

    private boolean isLocalhost(String value) {
        return value.contains("localhost") || value.contains("127.0.0.1") || value.contains("0.0.0.0");
    }
}
