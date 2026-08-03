package com.asistentewhatsapp.shared.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validacion pura de las dos modalidades intencionales del ambiente local.
 *
 * <p>
 * No lee el contexto de Spring ni el sistema de archivos: recibe un mapa de
 * propiedades ya resueltas (formato {@code app.*}) y devuelve la lista de
 * problemas detectados. Esto permite probarla de forma aislada y reutilizarla
 * en la compuerta de arranque y en cualquier otra herramienta de diagnostico.
 */
public final class LocalEnvironmentPolicy {

	private LocalEnvironmentPolicy() {
	}

	/**
	 * Modalidad local simulada: ningun trafico externo (WhatsApp, OpenAI, correo
	 * real, calendario Google, pagos). El correo solo puede usar Mailpit local.
	 */
	public static List<String> validateSafeMode(Map<String, String> env) {
		List<String> issues = new ArrayList<>();

		String provider = env.get("app.channels.whatsapp.provider");
		if (provider != null && !provider.isBlank() && !"SIMULATED".equalsIgnoreCase(provider.trim())) {
			issues.add("app.channels.whatsapp.provider debe ser SIMULATED en modo local-safe (actual: " + provider
					+ "). El proveedor META_CLOUD_API requiere el perfil local-meta-controlled.");
		}

		if (isTrue(env.get("app.channels.whatsapp-cloud-api.enabled"))) {
			issues.add("app.channels.whatsapp-cloud-api.enabled debe ser false en modo local-safe.");
		}

		if (isTrue(env.get("app.channels.openwa.enabled"))) {
			issues.add("app.channels.openwa.enabled debe ser false en modo local-safe.");
		}

		if (isTrue(env.get("app.ai.agents.auto-reply-enabled"))) {
			issues.add("app.ai.agents.auto-reply-enabled debe ser false en modo local-safe.");
		}

		if (isExplicitlyFalse(env.get("app.ai.agents.safe-mode-enabled"))) {
			issues.add("app.ai.agents.safe-mode-enabled debe ser true en modo local-safe.");
		}

		if (isTrue(env.get("app.ai.openai.enabled"))) {
			issues.add("app.ai.openai.enabled debe ser false en modo local-safe.");
		}

		if (isTrue(env.get("app.email.mirror.enabled"))) {
			issues.add(
					"app.email.mirror.enabled debe ser false en modo local-safe (el correo solo debe usar Mailpit).");
		}

		if (isTrue(env.get("app.email.enabled")) && !isLocalMailHost(env.get("spring.mail.host"))) {
			issues.add("app.email.enabled=true con spring.mail.host=" + env.get("spring.mail.host")
					+ " no es valido en modo local-safe: el unico destino permitido es Mailpit (host local).");
		}

		if (isTrue(env.get("app.calendar.google.enabled"))) {
			issues.add("app.calendar.google.enabled debe ser false en modo local-safe.");
		}

		String payment = env.get("app.booking-payment.provider");
		if (payment != null && !payment.isBlank() && !"SIMULATED".equalsIgnoreCase(payment.trim())) {
			issues.add("app.booking-payment.provider debe ser SIMULATED en modo local-safe (actual: " + payment + ").");
		}

		return issues;
	}

	/**
	 * Modalidad local de integracion real controlada con WhatsApp Cloud API (Meta):
	 * requiere doble confirmacion explicita, credenciales completas y lista
	 * permitida de telefonos de prueba.
	 */
	public static List<String> validateMetaControlled(Map<String, String> env) {
		List<String> issues = new ArrayList<>();

		String provider = env.get("app.channels.whatsapp.provider");
		if (provider == null || provider.isBlank() || !"META_CLOUD_API".equalsIgnoreCase(provider.trim())) {
			issues.add("app.channels.whatsapp.provider debe ser META_CLOUD_API en modo local-meta-controlled.");
		}

		if (!isTrue(env.get("app.channels.whatsapp-cloud-api.enabled"))) {
			issues.add("app.channels.whatsapp-cloud-api.enabled debe ser true en modo local-meta-controlled.");
		}

		if (isTrue(env.get("app.channels.whatsapp-cloud-api.dry-run-enabled"))) {
			issues.add("app.channels.whatsapp-cloud-api.dry-run-enabled debe ser false en modo local-meta-controlled.");
		}

		if (!isTrue(env.get("app.channels.whatsapp-cloud-api.webhook-signature-required"))) {
			issues.add(
					"app.channels.whatsapp-cloud-api.webhook-signature-required debe ser true en modo local-meta-controlled.");
		}

		requireNonBlank(env, "app.channels.whatsapp-cloud-api.access-token", issues);
		requireNonBlank(env, "app.channels.whatsapp-cloud-api.phone-number-id", issues);
		requireNonBlank(env, "app.channels.whatsapp-cloud-api.business-account-id", issues);
		requireNonBlank(env, "app.channels.whatsapp-cloud-api.app-secret", issues);
		requireNonBlank(env, "app.channels.whatsapp-cloud-api.webhook-verify-token", issues);
		requireNonBlank(env, "app.channels.whatsapp-cloud-api.credential-encryption-secret", issues);

		String allowlist = env.get("app.channels.whatsapp-cloud-api.allowed-test-phones");
		if (allowlist == null || allowlist.isBlank()) {
			issues.add("app.channels.whatsapp-cloud-api.allowed-test-phones debe listar al menos un telefono de prueba"
					+ " en modo local-meta-controlled.");
		}

		if (!isTrue(env.get("app.local-meta-controlled.acknowledged"))) {
			issues.add("app.local-meta-controlled.acknowledged debe ser true: confirmacion explicita de que la"
					+ " integracion real con Meta es intencional y solo se usaran numeros de prueba autorizados.");
		}

		return issues;
	}

	private static void requireNonBlank(Map<String, String> env, String key, List<String> issues) {
		String value = env.get(key);
		if (value == null || value.isBlank()) {
			issues.add(key + " es obligatoria en modo local-meta-controlled.");
		}
	}

	private static boolean isLocalMailHost(String host) {
		if (host == null || host.isBlank()) {
			return false;
		}
		String normalized = host.trim().toLowerCase();
		return normalized.equals("mailpit") || normalized.contains("localhost") || normalized.contains("127.0.0.1")
				|| normalized.contains("0.0.0.0") || normalized.endsWith(".local");
	}

	private static boolean isTrue(String value) {
		return value != null && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
	}

	private static boolean isExplicitlyFalse(String value) {
		return value != null && ("false".equalsIgnoreCase(value.trim()) || "0".equals(value.trim()));
	}
}
