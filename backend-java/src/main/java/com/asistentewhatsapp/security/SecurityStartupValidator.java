package com.asistentewhatsapp.security;

import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator implements ApplicationRunner {

	private static final String LOCAL_DEVELOPMENT_SECRET = "change-this-secret-in-local-development-please-2026";

	private final JwtProperties jwtProperties;
	private final Environment environment;

	public SecurityStartupValidator(JwtProperties jwtProperties, Environment environment) {
		this.jwtProperties = jwtProperties;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (isLocalLikeEnvironment()) {
			return;
		}

		String secret = jwtProperties.getSecret();
		if (secret == null || secret.isBlank() || secret.length() < 32 || LOCAL_DEVELOPMENT_SECRET.equals(secret)) {
			throw new IllegalStateException(
					"APP_JWT_SECRET es obligatorio y debe ser robusto en ambientes no locales.");
		}
	}

	private boolean isLocalLikeEnvironment() {
		String appEnvironment = environment.getProperty("app.environment", "local").toLowerCase(Locale.ROOT);
		return appEnvironment.equals("local") || appEnvironment.equals("dev") || appEnvironment.equals("development")
				|| appEnvironment.equals("demo") || appEnvironment.equals("test");
	}
}
