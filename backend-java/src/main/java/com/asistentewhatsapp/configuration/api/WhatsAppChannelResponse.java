package com.asistentewhatsapp.configuration.api;

import java.time.OffsetDateTime;
import java.util.List;

public record WhatsAppChannelResponse(String providerType, String providerLabel, String businessName,
		String displayPhoneNumber, String normalizedPhoneNumber, String registrationStatus, String registrationLabel,
		String operationalStatus, String operationalLabel, String webhookStatus, String webhookLabel,
		String credentialStatus, String credentialLabel, boolean active, OffsetDateTime lastHealthCheckAt,
		OffsetDateTime lastMessageReceivedAt, OffsetDateTime lastMessageSentAt, String lastErrorCode,
		String lastErrorMessage, OffsetDateTime updatedAt, MetaCloudConfig metaCloudConfig,
		List<ChannelEventItem> recentEvents) {

	public record MetaCloudConfig(String phoneNumberId, String businessAccountId, String graphApiVersion,
			String webhookCallbackUrl, String webhookStatus, String webhookLabel, String credentialStatus,
			String credentialLabel, OffsetDateTime tokenExpiresAt, boolean active) {
	}

	public record ChannelEventItem(String id, String eventType, String title, String description, String actor,
			String tone, OffsetDateTime occurredAt) {
	}
}
