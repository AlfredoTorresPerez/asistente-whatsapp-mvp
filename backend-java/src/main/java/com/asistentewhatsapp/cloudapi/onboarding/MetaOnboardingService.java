package com.asistentewhatsapp.cloudapi.onboarding;

import com.asistentewhatsapp.cloudapi.infrastructure.CloudApiTokenEncryptionService;
import com.asistentewhatsapp.cloudapi.onboarding.MetaGraphApiClient.MetaPhoneNumberInfo;
import com.asistentewhatsapp.cloudapi.onboarding.MetaGraphApiClient.MetaTokenExchangeResponse;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetaOnboardingService {

	private static final Logger LOG = LoggerFactory.getLogger(MetaOnboardingService.class);
	private static final String DEFAULT_PIN = "123456";

	private final MetaOnboardingRepository repository;
	private final MetaGraphApiClient graphApiClient;
	private final CloudApiTokenEncryptionService tokenEncryption;

	private final String metaAppId;
	private final String metaAppSecret;
	private final String environment;
	private final List<String> allowedRedirectUris;

	public MetaOnboardingService(MetaOnboardingRepository repository, MetaGraphApiClient graphApiClient,
			CloudApiTokenEncryptionService tokenEncryption,
			@Value("${app.channels.whatsapp-cloud-api.app-id:}") String metaAppId,
			@Value("${app.channels.whatsapp-cloud-api.app-secret:}") String metaAppSecret,
			@Value("${app.environment:local}") String environment,
			@Value("${app.channels.whatsapp-cloud-api.webhook-public-url:}") String webhookPublicUrl) {
		this.repository = repository;
		this.graphApiClient = graphApiClient;
		this.tokenEncryption = tokenEncryption;
		this.metaAppId = metaAppId;
		this.metaAppSecret = metaAppSecret;
		this.environment = environment;
		this.allowedRedirectUris = List.of(webhookPublicUrl, "https://localhost", "https://localhost:5173",
				"http://localhost:5173", "http://localhost:3000");
	}

	private void validateRedirectUri(String redirectUri) {
		if (redirectUri == null || redirectUri.isBlank())
			return;
		boolean allowed = allowedRedirectUris.stream()
				.anyMatch(uri -> redirectUri.startsWith(uri) || uri.startsWith(redirectUri));
		if (!allowed) {
			LOG.warn("Rejected redirectUri not in whitelist: {}", redirectUri);
			throw new ApiException(HttpStatus.FORBIDDEN, "REDIRECT_URI_NOT_ALLOWED",
					"El redirect_uri no esta en la lista permitida para este ambiente.");
		}
	}

	@Transactional
	public OnboardingResult completeOnboarding(UUID businessId, CompleteOnboardingRequest request) {
		if (metaAppId == null || metaAppId.isBlank()) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "META_CONFIG_MISSING",
					"Meta App ID no configurado en el servidor.");
		}
		if (metaAppSecret == null || metaAppSecret.isBlank()) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "META_CONFIG_MISSING",
					"Meta App Secret no configurado en el servidor.");
		}

		validateRedirectUri(request.redirectUri());

		ChannelAccountRecord channel = repository.findOrCreateCentralChannel(businessId);

		MetaTokenExchangeResponse tokenResponse = graphApiClient.exchangeCodeForToken(request.code(), metaAppId,
				metaAppSecret, request.redirectUri());

		String systemAccessToken = tokenResponse.accessToken();
		OffsetDateTime tokenExpiresAt = tokenResponse.expiresAt();

		if (request.wabaId() == null || request.wabaId().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_WABA_ID",
					"El identificador del WABA es requerido.");
		}

		graphApiClient.fetchWabaInfo(systemAccessToken, request.wabaId());

		if (request.phoneNumberId() == null || request.phoneNumberId().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_PHONE_NUMBER_ID",
					"El identificador del numero telefonico es requerido.");
		}

		MetaPhoneNumberInfo phoneInfo = graphApiClient.fetchPhoneNumber(systemAccessToken, request.phoneNumberId());

		if (!request.phoneNumberId().equals(phoneInfo.id())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_NUMBER_MISMATCH",
					"El Phone Number ID no coincide con el WABA seleccionado.");
		}

		repository.validatePhoneNumberIdNotUsed(request.phoneNumberId(), businessId);

		graphApiClient.subscribeAppToWaba(systemAccessToken, request.wabaId());

		graphApiClient.registerPhoneNumber(systemAccessToken, request.phoneNumberId(), DEFAULT_PIN);

		graphApiClient.setTwoStepPin(systemAccessToken, request.phoneNumberId(), DEFAULT_PIN);

		String encryptedToken = tokenEncryption.encrypt(systemAccessToken);

		repository.updateAfterOnboarding(channel.id(), request.phoneNumberId(), request.wabaId(), encryptedToken,
				tokenExpiresAt);

		repository.updatePhoneMetadata(channel.id(), phoneInfo.displayPhoneNumber(), phoneInfo.normalizedPhoneNumber(),
				phoneInfo.verifiedName());

		repository.updateWebhookStatus(channel.id(), "SUBSCRIBED");

		repository.updateHealthCheck(channel.id());

		LOG.info("Onboarding completado exitosamente para negocio {} canal {}", businessId, channel.id());

		return new OnboardingResult("META_CLOUD_API", phoneInfo.displayPhoneNumber(), "CONNECTED",
				maskString(request.phoneNumberId()), maskString(request.wabaId()), "SUBSCRIBED");
	}

	public OnboardingStatus getStatus(UUID businessId) {
		ChannelAccountRecord channel = repository.findCloudApiChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND",
						"No hay canal META_CLOUD_API configurado para este negocio."));

		String webhookStatus = channel.webhookStatus() != null ? channel.webhookStatus() : "DISABLED";

		return new OnboardingStatus("META_CLOUD_API", channel.displayPhoneNumber(), channel.normalizedPhoneNumber(),
				channel.registrationStatus(), channel.operationalStatus(), webhookStatus, channel.credentialStatus(),
				channel.active(), maskString(channel.phoneNumberId()), maskString(channel.providerAccountId()),
				"CENTRALIZED", channel.lastErrorCode(), channel.lastError(), channel.lastHealthCheckAt(),
				channel.tokenExpiresAt(), channel.updatedAt());
	}

	@Transactional
	public OnboardingResult revalidate(UUID businessId) {
		ChannelAccountRecord channel = repository.findCloudApiChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND",
						"No hay canal META_CLOUD_API configurado para este negocio."));

		if (!channel.active()) {
			throw new ApiException(HttpStatus.CONFLICT, "CHANNEL_INACTIVE",
					"El canal esta desactivado. Activalo antes de revalidar.");
		}

		if (channel.encryptedAccessToken() == null || channel.encryptedAccessToken().isBlank()) {
			throw new ApiException(HttpStatus.CONFLICT, "NO_TOKEN",
					"No hay token almacenado. Completa el onboarding primero.");
		}

		if (channel.phoneNumberId() == null || channel.phoneNumberId().isBlank()) {
			throw new ApiException(HttpStatus.CONFLICT, "NO_PHONE_NUMBER_ID",
					"No hay Phone Number ID. Completa el onboarding primero.");
		}

		String decryptedToken;
		try {
			decryptedToken = tokenEncryption.decrypt(channel.encryptedAccessToken());
		} catch (Exception e) {
			LOG.error("Failed to decrypt token for business {}", businessId);
			repository.updateOperationalStatus(channel.id(), "ERROR", "TOKEN_DECRYPT_FAILED");
			repository.updateHealthCheck(channel.id());
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_DECRYPT_FAILED",
					"No se pudo descifrar el token de acceso.");
		}

		try {
			graphApiClient.fetchWabaInfo(decryptedToken, channel.providerAccountId());

			MetaPhoneNumberInfo phoneInfo = graphApiClient.fetchPhoneNumber(decryptedToken, channel.phoneNumberId());

			repository.updatePhoneMetadata(channel.id(), phoneInfo.displayPhoneNumber(),
					phoneInfo.normalizedPhoneNumber(), phoneInfo.verifiedName());

			repository.updateOperationalStatus(channel.id(), "CONNECTED", null);
			repository.updateWebhookStatus(channel.id(), "SUBSCRIBED");
			repository.updateHealthCheck(channel.id());

			LOG.info("Revalidacion exitosa para negocio {}", businessId);

			return new OnboardingResult("META_CLOUD_API", phoneInfo.displayPhoneNumber(), "CONNECTED",
					maskString(channel.phoneNumberId()), maskString(channel.providerAccountId()), "SUBSCRIBED");
		} catch (ApiException e) {
			repository.updateHealthCheck(channel.id());
			if (e.getStatus() == HttpStatus.BAD_GATEWAY) {
				repository.updateOperationalStatus(channel.id(), "ERROR", "META_UNREACHABLE");
				throw new ApiException(HttpStatus.BAD_GATEWAY, "META_UNREACHABLE",
						"No se pudo contactar a Meta para revalidar. Verifica credenciales.");
			}
			throw e;
		} catch (Exception e) {
			LOG.error("Error revalidating channel for business {}", businessId, e);
			repository.updateOperationalStatus(channel.id(), "ERROR", "REVALIDATE_FAILED");
			repository.updateHealthCheck(channel.id());
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "REVALIDATE_FAILED",
					"Error al revalidar el canal.");
		}
	}

	@Transactional
	public void disconnect(UUID businessId) {
		ChannelAccountRecord channel = repository.findCloudApiChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND",
						"No hay canal META_CLOUD_API configurado para este negocio."));

		repository.disconnectChannel(channel.id());

		LOG.info("Canal META_CLOUD_API desconectado (solo local) para negocio {} canal {}", businessId, channel.id());
	}

	@Transactional
	public void disconnectFromMeta(UUID businessId) {
		ChannelAccountRecord channel = repository.findCloudApiChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND",
						"No hay canal META_CLOUD_API configurado para este negocio."));

		if (channel.encryptedAccessToken() != null && !channel.encryptedAccessToken().isBlank()) {
			try {
				String token = tokenEncryption.decrypt(channel.encryptedAccessToken());
				String wabaId = channel.providerAccountId();
				if (wabaId != null && !wabaId.isBlank()) {
					try {
						graphApiClient.unsubscribeAppFromWaba(token, wabaId);
					} catch (Exception e) {
						LOG.warn("Failed to unsubscribe app from WABA {}: {}", wabaId, e.getMessage());
					}
				}
			} catch (Exception e) {
				LOG.warn("Failed to decrypt token for Meta disconnect: {}", e.getMessage());
			}
		}

		repository.disconnectChannel(channel.id());

		LOG.info("Canal META_CLOUD_API desconectado (local y Meta) para negocio {} canal {}", businessId, channel.id());
	}

	private static String maskString(String value) {
		if (value == null || value.length() < 8)
			return value;
		return value.substring(0, 4) + "\u2022\u2022\u2022\u2022" + value.substring(value.length() - 4);
	}

	public record CompleteOnboardingRequest(String code, String redirectUri, String wabaId, String phoneNumberId) {
	}

	public record OnboardingResult(String providerType, String displayPhoneNumber, String operationalStatus,
			String maskedPhoneNumberId, String maskedWabaId, String webhookStatus) {
	}

	public record OnboardingStatus(String providerType, String displayPhoneNumber, String normalizedPhoneNumber,
			String registrationStatus, String operationalStatus, String webhookStatus, String credentialStatus,
			boolean active, String maskedPhoneNumberId, String maskedWabaId, String routingMode, String lastErrorCode,
			String lastErrorMessage, OffsetDateTime lastHealthCheckAt, OffsetDateTime tokenExpiresAt,
			OffsetDateTime updatedAt) {
	}
}
