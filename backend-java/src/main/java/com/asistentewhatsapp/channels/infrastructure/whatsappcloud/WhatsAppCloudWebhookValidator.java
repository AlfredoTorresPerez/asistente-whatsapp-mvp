package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.shared.exception.ApiException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
public class WhatsAppCloudWebhookValidator {

	private static final Logger LOG = LoggerFactory.getLogger(WhatsAppCloudWebhookValidator.class);
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String SIGNATURE_PREFIX = "sha256=";

	private final WhatsAppCloudApiProperties properties;

	public WhatsAppCloudWebhookValidator(WhatsAppCloudApiProperties properties) {
		this.properties = properties;
	}

	public String validateAndExtractChallenge(String mode, String verifyToken, String challenge) {
		if (!"subscribe".equals(mode)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "WEBHOOK_VERIFICATION_FAILED",
					"Modo de verificacion no soportado.");
		}

		if (verifyToken == null || properties.webhookVerifyToken() == null) {
			throw new ApiException(HttpStatus.FORBIDDEN, "WEBHOOK_VERIFICATION_FAILED",
					"Token de verificacion no proporcionado.");
		}

		if (!MessageDigest.isEqual(verifyToken.getBytes(StandardCharsets.UTF_8),
				properties.webhookVerifyToken().getBytes(StandardCharsets.UTF_8))) {
			LOG.warn("Webhook verify token mismatch");
			throw new ApiException(HttpStatus.FORBIDDEN, "WEBHOOK_VERIFICATION_FAILED",
					"Token de verificacion invalido.");
		}

		return challenge;
	}

	public void validateSignature(String signatureHeader, String rawBody) {
		boolean required = properties.webhookSignatureRequired();
		boolean hasSecret = properties.appSecret() != null && !properties.appSecret().isBlank();

		if (required && !hasSecret) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK_CONFIG_ERROR",
					"La validacion de firma esta habilitada pero APP_SECRET no esta configurado.");
		}

		if (!hasSecret) {
			LOG.warn("Webhook signature validation disabled: APP_SECRET not configured");
			return;
		}

		if (signatureHeader == null || signatureHeader.isBlank()) {
			if (required) {
				throw new ApiException(HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_MISSING",
						"Firma X-Hub-Signature-256 requerida pero no proporcionada.");
			}
			LOG.warn("Webhook signature missing, skipping validation (not required by config)");
			return;
		}

		String expectedSignature = computeSignature(rawBody);
		String providedSignature = signatureHeader.trim();

		if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
				providedSignature.getBytes(StandardCharsets.UTF_8))) {
			LOG.warn("Webhook HMAC signature validation failed");
			throw new ApiException(HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_INVALID",
					"La firma del webhook no es valida.");
		}
	}

	public String computeSignature(String rawBody) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(properties.appSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			byte[] signature = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
			return SIGNATURE_PREFIX + java.util.HexFormat.of().formatHex(signature);
		} catch (Exception exception) {
			throw new IllegalStateException("No se pudo calcular la firma HMAC del webhook.", exception);
		}
	}

	private static class MessageDigest {

		static boolean isEqual(byte[] a, byte[] b) {
			if (a == b)
				return true;
			if (a == null || b == null || a.length != b.length)
				return false;
			int result = 0;
			for (int i = 0; i < a.length; i++) {
				result |= a[i] ^ b[i];
			}
			return result == 0;
		}
	}
}
