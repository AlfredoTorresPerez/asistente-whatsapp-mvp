package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels.whatsapp-cloud-api")
public record WhatsAppCloudApiProperties(boolean enabled, String baseUrl, String apiVersion, String appId,
		String appSecret, String webhookVerifyToken, String webhookPublicUrl, boolean webhookSignatureRequired,
		String credentialEncryptionSecret, String phoneNumberId, String businessAccountId, String accessToken,
		String defaultPhoneNumber, boolean dryRunEnabled, int connectTimeoutSeconds, int readTimeoutSeconds,
		List<String> allowedTestPhones) {
}
