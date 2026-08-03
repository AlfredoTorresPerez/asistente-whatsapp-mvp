package com.asistentewhatsapp.shared.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalEnvironmentPolicyTest {

	private final Map<String, String> safeBaseline = Map.of("app.channels.whatsapp.provider", "SIMULATED",
			"app.channels.whatsapp-cloud-api.enabled", "false", "app.ai.agents.auto-reply-enabled", "false",
			"app.ai.agents.safe-mode-enabled", "true", "app.ai.openai.enabled", "false", "app.email.enabled", "true",
			"app.email.mirror.enabled", "false", "spring.mail.host", "mailpit", "app.calendar.google.enabled", "false",
			"app.booking-payment.provider", "SIMULATED");

	@Test
	void safeModeWithCleanConfigurationHasNoIssues() {
		assertThat(LocalEnvironmentPolicy.validateSafeMode(safeBaseline)).isEmpty();
	}

	@Test
	void safeModeRejectsCloudApiEnabled() {
		Map<String, String> env = withOverride(safeBaseline, "app.channels.whatsapp-cloud-api.enabled", "true");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env))
				.anyMatch(issue -> issue.contains("whatsapp-cloud-api.enabled"));
	}

	@Test
	void safeModeRejectsMetaProvider() {
		Map<String, String> env = withOverride(safeBaseline, "app.channels.whatsapp.provider", "META_CLOUD_API");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("SIMULATED"));
	}

	@Test
	void safeModeRejectsAutoReplyEnabled() {
		Map<String, String> env = withOverride(safeBaseline, "app.ai.agents.auto-reply-enabled", "true");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env))
				.anyMatch(issue -> issue.contains("auto-reply-enabled"));
	}

	@Test
	void safeModeRejectsSafeModeDisabled() {
		Map<String, String> env = withOverride(safeBaseline, "app.ai.agents.safe-mode-enabled", "false");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("safe-mode-enabled"));
	}

	@Test
	void safeModeRejectsOpenAiEnabled() {
		Map<String, String> env = withOverride(safeBaseline, "app.ai.openai.enabled", "true");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("openai.enabled"));
	}

	@Test
	void safeModeRejectsEmailMirrorEnabled() {
		Map<String, String> env = withOverride(safeBaseline, "app.email.mirror.enabled", "true");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("mirror.enabled"));
	}

	@Test
	void safeModeRejectsExternalSmtpHost() {
		Map<String, String> env = withOverride(safeBaseline, "spring.mail.host", "smtp.gmail.com");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("spring.mail.host"));
	}

	@Test
	void safeModeAllowsLocalhostSmtp() {
		Map<String, String> env = withOverride(safeBaseline, "spring.mail.host", "localhost");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).isEmpty();
	}

	@Test
	void safeModeRejectsGoogleCalendar() {
		Map<String, String> env = withOverride(safeBaseline, "app.calendar.google.enabled", "true");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("calendar.google"));
	}

	@Test
	void safeModeRejectsExternalPaymentProvider() {
		Map<String, String> env = withOverride(safeBaseline, "app.booking-payment.provider", "MERCADOPAGO");
		assertThat(LocalEnvironmentPolicy.validateSafeMode(env)).anyMatch(issue -> issue.contains("booking-payment"));
	}

	@Test
	void safeModeIsLenientWithMissingKeys() {
		assertThat(LocalEnvironmentPolicy.validateSafeMode(Map.of())).isEmpty();
	}

	@Test
	void metaControlledWithFullConfigurationHasNoIssues() {
		Map<String, String> env = metaControlledBaseline();
		assertThat(LocalEnvironmentPolicy.validateMetaControlled(env)).isEmpty();
	}

	@Test
	void metaControlledRequiresAcknowledgment() {
		Map<String, String> env = withOverride(metaControlledBaseline(), "app.local-meta-controlled.acknowledged",
				"false");
		assertThat(LocalEnvironmentPolicy.validateMetaControlled(env))
				.anyMatch(issue -> issue.contains("acknowledged"));
	}

	@Test
	void metaControlledRequiresAllowlist() {
		Map<String, String> env = withOverride(metaControlledBaseline(),
				"app.channels.whatsapp-cloud-api.allowed-test-phones", "");
		assertThat(LocalEnvironmentPolicy.validateMetaControlled(env))
				.anyMatch(issue -> issue.contains("allowed-test-phones"));
	}

	@Test
	void metaControlledRequiresProviderMeta() {
		Map<String, String> env = withOverride(metaControlledBaseline(), "app.channels.whatsapp.provider", "SIMULATED");
		assertThat(LocalEnvironmentPolicy.validateMetaControlled(env))
				.anyMatch(issue -> issue.contains("META_CLOUD_API"));
	}

	@Test
	void metaControlledRequiresCompleteCredentials() {
		Map<String, String> env = withOverride(metaControlledBaseline(), "app.channels.whatsapp-cloud-api.access-token",
				"");
		List<String> issues = LocalEnvironmentPolicy.validateMetaControlled(env);
		assertThat(issues).anyMatch(issue -> issue.contains("access-token"));
	}

	@Test
	void metaControlledRequiresSignatureAndNoDryRun() {
		Map<String, String> env = withOverride(metaControlledBaseline(),
				"app.channels.whatsapp-cloud-api.webhook-signature-required", "false");
		env = withOverride(env, "app.channels.whatsapp-cloud-api.dry-run-enabled", "true");
		List<String> issues = LocalEnvironmentPolicy.validateMetaControlled(env);
		assertThat(issues).anyMatch(issue -> issue.contains("webhook-signature-required"));
		assertThat(issues).anyMatch(issue -> issue.contains("dry-run-enabled"));
	}

	private Map<String, String> metaControlledBaseline() {
		return Map.ofEntries(Map.entry("app.channels.whatsapp.provider", "META_CLOUD_API"),
				Map.entry("app.channels.whatsapp-cloud-api.enabled", "true"),
				Map.entry("app.channels.whatsapp-cloud-api.dry-run-enabled", "false"),
				Map.entry("app.channels.whatsapp-cloud-api.webhook-signature-required", "true"),
				Map.entry("app.channels.whatsapp-cloud-api.access-token", "token-test"),
				Map.entry("app.channels.whatsapp-cloud-api.phone-number-id", "12345"),
				Map.entry("app.channels.whatsapp-cloud-api.business-account-id", "waba-test"),
				Map.entry("app.channels.whatsapp-cloud-api.app-secret", "secret-test"),
				Map.entry("app.channels.whatsapp-cloud-api.webhook-verify-token", "verify-test"),
				Map.entry("app.channels.whatsapp-cloud-api.credential-encryption-secret", "enc-test"),
				Map.entry("app.channels.whatsapp-cloud-api.allowed-test-phones", "56911112222"),
				Map.entry("app.local-meta-controlled.acknowledged", "true"));
	}

	private Map<String, String> withOverride(Map<String, String> base, String key, String value) {
		Map<String, String> copy = new HashMap<>(base);
		copy.put(key, value);
		return copy;
	}
}
