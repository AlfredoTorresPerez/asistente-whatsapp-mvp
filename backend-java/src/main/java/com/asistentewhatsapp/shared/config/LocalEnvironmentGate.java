package com.asistentewhatsapp.shared.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Compuerta de arranque del ambiente local: impide que la aplicacion inicie en
 * una combinacion de configuracion que permita trafico externo no autorizado.
 *
 * <p>
 * Mismo patron que {@code EmailConfigValidator}: corre al arranque y lanza
 * {@link IllegalStateException} con la lista de propiedades infractoras. Nunca
 * imprime valores de secretos, solo nombres de propiedades.
 *
 * <p>
 * Reglas por modalidad:
 * <ul>
 * <li>Perfil {@code local-safe}: ningun trafico externo (WhatsApp, OpenAI,
 * correo real, calendario Google, pagos).</li>
 * <li>Perfil {@code local-meta-controlled}: integracion real controlada con
 * Meta, exige doble confirmacion (ack) y lista permitida de telefonos.</li>
 * </ul>
 */
@Component
@Profile({"local-safe", "local-meta-controlled"})
public class LocalEnvironmentGate implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalEnvironmentGate.class);

	private static final List<String> SAFE_MODE_KEYS = List.of("app.channels.whatsapp.provider",
			"app.channels.whatsapp-cloud-api.enabled", "app.channels.openwa.enabled",
			"app.ai.agents.auto-reply-enabled", "app.ai.agents.safe-mode-enabled", "app.ai.openai.enabled",
			"app.email.enabled", "app.email.mirror.enabled", "spring.mail.host", "app.calendar.google.enabled",
			"app.booking-payment.provider");

	private static final List<String> META_CONTROLLED_KEYS = List.of("app.channels.whatsapp.provider",
			"app.channels.whatsapp-cloud-api.enabled", "app.channels.whatsapp-cloud-api.dry-run-enabled",
			"app.channels.whatsapp-cloud-api.webhook-signature-required",
			"app.channels.whatsapp-cloud-api.access-token", "app.channels.whatsapp-cloud-api.phone-number-id",
			"app.channels.whatsapp-cloud-api.business-account-id", "app.channels.whatsapp-cloud-api.app-secret",
			"app.channels.whatsapp-cloud-api.webhook-verify-token",
			"app.channels.whatsapp-cloud-api.credential-encryption-secret",
			"app.channels.whatsapp-cloud-api.allowed-test-phones", "app.local-meta-controlled.acknowledged");

	private final Environment environment;

	public LocalEnvironmentGate(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		boolean metaControlled = environment
				.acceptsProfiles(org.springframework.core.env.Profiles.of("local-meta-controlled"));
		boolean safe = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("local-safe"));

		if (!metaControlled && !safe) {
			return;
		}

		List<String> issues = metaControlled
				? LocalEnvironmentPolicy.validateMetaControlled(collect(META_CONTROLLED_KEYS))
				: LocalEnvironmentPolicy.validateSafeMode(collect(SAFE_MODE_KEYS));

		if (!issues.isEmpty()) {
			String summary = String.join(" | ", issues);
			LOGGER.error("LOCAL_ENV_GATE_REJECTED mode={} issues=[{}]", metaControlled ? "meta-controlled" : "safe",
					summary);
			throw new IllegalStateException(
					"Configuracion de entorno local rechazada por la compuerta de arranque: " + summary);
		}

		LOGGER.info("LOCAL_ENV_GATE_OK mode={} provider={} cloudApi={} autoReply={} safeMode={} openAi={} mirror={}",
				metaControlled ? "meta-controlled" : "safe",
				environment.getProperty("app.channels.whatsapp.provider", "SIMULATED"),
				environment.getProperty("app.channels.whatsapp-cloud-api.enabled", "false"),
				environment.getProperty("app.ai.agents.auto-reply-enabled", "false"),
				environment.getProperty("app.ai.agents.safe-mode-enabled", "false"),
				environment.getProperty("app.ai.openai.enabled", "false"),
				environment.getProperty("app.email.mirror.enabled", "false"));
	}

	private Map<String, String> collect(List<String> keys) {
		Map<String, String> resolved = new LinkedHashMap<>();
		for (String key : keys) {
			resolved.put(key, environment.getProperty(key, ""));
		}
		return resolved;
	}
}
