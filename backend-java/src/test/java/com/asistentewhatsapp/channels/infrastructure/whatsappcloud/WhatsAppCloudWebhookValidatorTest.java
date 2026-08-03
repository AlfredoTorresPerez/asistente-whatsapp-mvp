package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.shared.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppCloudWebhookValidatorTest {

	private WhatsAppCloudApiProperties properties(boolean signatureRequired, String appSecret, String verifyToken) {
		return new WhatsAppCloudApiProperties(true, "https://graph.facebook.com", "v23.0", null, appSecret, verifyToken,
				null, signatureRequired, null, "12345", null, null, "56911112222", false, 5, 15, List.of());
	}

	@Test
	void validVerificationReturnsChallenge() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(properties(false, null, "mytoken"));
		String result = validator.validateAndExtractChallenge("subscribe", "mytoken", "challenge123");
		assertThat(result).isEqualTo("challenge123");
	}

	@Test
	void invalidModeThrowsForbidden() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(properties(false, null, "mytoken"));
		assertThatThrownBy(() -> validator.validateAndExtractChallenge("unsubscribe", "mytoken", "challenge"))
				.isInstanceOf(ApiException.class).matches(e -> ((ApiException) e).getStatus() == HttpStatus.FORBIDDEN);
	}

	@Test
	void invalidTokenThrowsForbidden() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(properties(false, null, "mytoken"));
		assertThatThrownBy(() -> validator.validateAndExtractChallenge("subscribe", "wrongtoken", "challenge"))
				.isInstanceOf(ApiException.class).matches(e -> ((ApiException) e).getStatus() == HttpStatus.FORBIDDEN);
	}

	@Test
	void nullTokenThrowsForbidden() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(properties(false, null, null));
		assertThatThrownBy(() -> validator.validateAndExtractChallenge("subscribe", "token", "challenge"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void validSignaturePasses() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(
				properties(true, "my_app_secret_123", "mytoken"));
		String body = "{\"test\":\"data\"}";
		String signature = validator.computeSignature(body);
		validator.validateSignature(signature, body);
	}

	@Test
	void invalidSignatureThrows() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(
				properties(true, "my_app_secret_123", "mytoken"));
		assertThatThrownBy(() -> validator.validateSignature("sha256=invalid", "body")).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getStatus() == HttpStatus.UNAUTHORIZED);
	}

	@Test
	void missingSignatureWhenRequiredThrows() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(
				properties(true, "my_app_secret_123", "mytoken"));
		assertThatThrownBy(() -> validator.validateSignature(null, "body")).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getStatus() == HttpStatus.UNAUTHORIZED);
	}

	@Test
	void missingSecretWhenRequiredThrows() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(properties(true, null, "mytoken"));
		assertThatThrownBy(() -> validator.validateSignature("sha256=abc", "body")).isInstanceOf(ApiException.class);
	}

	@Test
	void signatureCalculatedOverModifiedBodyFails() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(
				properties(true, "my_app_secret_123", "mytoken"));
		String originalBody = "{\"a\":1}";
		String modifiedBody = "{\"a\":2}";
		String signature = validator.computeSignature(originalBody);
		assertThatThrownBy(() -> validator.validateSignature(signature, modifiedBody)).isInstanceOf(ApiException.class);
	}

	@Test
	void signatureNotRequiredWhenDisabled() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(properties(false, null, "mytoken"));
		validator.validateSignature(null, "body");
	}

	@Test
	void computeSignatureReturnsCorrectFormat() {
		WhatsAppCloudWebhookValidator validator = new WhatsAppCloudWebhookValidator(
				properties(true, "test_secret", "mytoken"));
		String signature = validator.computeSignature("test_body");
		assertThat(signature).startsWith("sha256=");
		assertThat(signature.length()).isGreaterThan("sha256=".length());
	}
}
