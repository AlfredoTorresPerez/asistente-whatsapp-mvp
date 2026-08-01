package com.asistentewhatsapp.security;

import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecretsStartupValidator implements ApplicationRunner {

	private static final String[] PLACEHOLDER_PATTERNS = {"change-this", "replace-with", "your-", "placeholder",
			"change-me"};

	private final Environment environment;

	public SecretsStartupValidator(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (isLocalLikeEnvironment()) {
			return;
		}

		String[][] secretsToCheck = {{"APP_DB_PASSWORD", "Base de datos"}, {"APP_JWT_SECRET", "JWT"},
				{"APP_MERCADOPAGO_ACCESS_TOKEN", "MercadoPago"},
				{"APP_BOOKING_PAYMENT_WEBHOOK_SECRET", "Pagos webhook"},};

		boolean cloudApiEnabled = "true"
				.equalsIgnoreCase(environment.getProperty("app.channels.whatsapp-cloud-api.enabled"));
		if (cloudApiEnabled) {
			String[][] cloudSecrets = {{"APP_WHATSAPP_CLOUD_API_APP_ID", "WhatsApp Cloud API App ID"},
					{"APP_WHATSAPP_CLOUD_API_APP_SECRET", "WhatsApp Cloud API App Secret"},
					{"APP_WHATSAPP_CLOUD_API_WEBHOOK_VERIFY_TOKEN", "WhatsApp Cloud API Webhook Verify Token"},
					{"APP_WHATSAPP_CLOUD_API_CREDENTIAL_ENCRYPTION_SECRET", "WhatsApp Cloud API Encryption Secret"},};
			String[][] extended = new String[secretsToCheck.length + cloudSecrets.length][];
			System.arraycopy(secretsToCheck, 0, extended, 0, secretsToCheck.length);
			System.arraycopy(cloudSecrets, 0, extended, secretsToCheck.length, cloudSecrets.length);
			secretsToCheck = extended;
		}

		for (String[] secret : secretsToCheck) {
			String value = environment.getProperty(secret[0]);
			if (value == null || value.isBlank() || isPlaceholder(value)) {
				throw new IllegalStateException(secret[0] + " (" + secret[1]
						+ ") es obligatorio y no puede ser placeholder en ambientes no locales.");
			}
		}
	}

	private boolean isPlaceholder(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		for (String pattern : PLACEHOLDER_PATTERNS) {
			if (lower.contains(pattern)) {
				return true;
			}
		}
		return false;
	}

	private boolean isLocalLikeEnvironment() {
		String appEnvironment = environment.getProperty("app.environment", "local").toLowerCase(Locale.ROOT);
		return appEnvironment.equals("local") || appEnvironment.equals("dev") || appEnvironment.equals("development")
				|| appEnvironment.equals("demo") || appEnvironment.equals("test");
	}
}
