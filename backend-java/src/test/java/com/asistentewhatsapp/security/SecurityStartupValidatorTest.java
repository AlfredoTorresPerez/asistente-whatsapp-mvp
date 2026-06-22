package com.asistentewhatsapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityStartupValidatorTest {

    @Test
    void productionFailsWhenJwtSecretIsMissing() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.environment", "production");
        SecurityStartupValidator validator = new SecurityStartupValidator(jwtProperties, environment);

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void localAllowsDevelopmentSecret() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("change-this-secret-in-local-development-please-2026");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.environment", "local");
        SecurityStartupValidator validator = new SecurityStartupValidator(jwtProperties, environment);

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
