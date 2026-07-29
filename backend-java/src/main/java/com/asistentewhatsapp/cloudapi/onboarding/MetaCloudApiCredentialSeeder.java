package com.asistentewhatsapp.cloudapi.onboarding;

import com.asistentewhatsapp.channels.infrastructure.whatsappcloud.WhatsAppCloudApiProperties;
import com.asistentewhatsapp.cloudapi.infrastructure.CloudApiTokenEncryptionService;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
@Order(500)
public class MetaCloudApiCredentialSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MetaCloudApiCredentialSeeder.class);
	private static final UUID DEMO_BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private final MetaOnboardingRepository onboardingRepository;
	private final CloudApiTokenEncryptionService tokenEncryption;
	private final WhatsAppCloudApiProperties properties;
	private final MetaGraphApiClient graphApiClient;

	public MetaCloudApiCredentialSeeder(MetaOnboardingRepository onboardingRepository,
			CloudApiTokenEncryptionService tokenEncryption, WhatsAppCloudApiProperties properties,
			MetaGraphApiClient graphApiClient) {
		this.onboardingRepository = onboardingRepository;
		this.tokenEncryption = tokenEncryption;
		this.properties = properties;
		this.graphApiClient = graphApiClient;
	}

	@Override
	public void run(ApplicationArguments args) {
		ChannelAccountRecord channel = onboardingRepository.findCloudApiChannel(DEMO_BUSINESS_ID).orElse(null);
		if (channel == null) {
			log.warn("No META_CLOUD_API channel found for demo business; skipping credential seed.");
			return;
		}

		String accessToken = properties.accessToken();
		String phoneNumberId = properties.phoneNumberId();
		String businessAccountId = properties.businessAccountId();

		if ((phoneNumberId == null || phoneNumberId.isBlank()) && channel.phoneNumberId() != null) {
			phoneNumberId = channel.phoneNumberId();
		}
		if ((businessAccountId == null || businessAccountId.isBlank()) && channel.providerAccountId() != null) {
			businessAccountId = channel.providerAccountId();
		}

		if (channel.phoneNumberId() == null || channel.encryptedAccessToken() == null) {
			if (accessToken == null || accessToken.isBlank()) {
				log.warn("APP_WHATSAPP_CLOUD_API_ACCESS_TOKEN not configured in .env.local; cannot seed credentials.");
				return;
			}
			if (phoneNumberId == null || phoneNumberId.isBlank()) {
				log.warn("APP_WHATSAPP_CLOUD_API_PHONE_NUMBER_ID not configured; cannot seed credentials.");
				return;
			}
			String encryptedToken = tokenEncryption.encrypt(accessToken);
			onboardingRepository.updateAfterOnboarding(channel.id(), phoneNumberId, businessAccountId, encryptedToken,
					null);
			log.info("META_CLOUD_API credentials seeded from env vars for demo business (phoneNumberId={})",
					phoneNumberId);
		} else {
			log.info("META_CLOUD_API channel already has credentials in DB.");
			if ((accessToken == null || accessToken.isBlank()) && channel.encryptedAccessToken() != null) {
				try {
					accessToken = tokenEncryption.decrypt(channel.encryptedAccessToken());
				} catch (Exception e) {
					log.warn("Failed to decrypt stored access token: {}", e.getMessage());
				}
			}
		}

		if (accessToken != null && !accessToken.isBlank() && businessAccountId != null
				&& !businessAccountId.isBlank()) {
			try {
				graphApiClient.subscribeAppToWaba(accessToken, businessAccountId);
				onboardingRepository.updateWebhookStatus(channel.id(), "SUBSCRIBED");
				log.info("App subscribed to WABA {} successfully", businessAccountId);
			} catch (Exception e) {
				log.warn("Failed to subscribe app to WABA {}: {}", businessAccountId, e.getMessage());
			}
		}
	}
}
