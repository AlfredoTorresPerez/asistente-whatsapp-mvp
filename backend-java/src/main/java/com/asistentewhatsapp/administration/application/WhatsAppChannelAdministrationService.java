package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.WhatsAppChannelActionResponse;
import com.asistentewhatsapp.administration.api.WhatsAppChannelRecentEvent;
import com.asistentewhatsapp.administration.api.WhatsAppChannelStatusResponse;
import com.asistentewhatsapp.administration.api.WhatsAppChannelTestMessageRequest;
import com.asistentewhatsapp.administration.api.WhatsAppChannelTestMessageResponse;
import com.asistentewhatsapp.channels.application.WhatsAppChannelProperties;
import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionAction;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionStatus;
import com.asistentewhatsapp.channels.infrastructure.WhatsAppChannelJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppChannelAdministrationService {

	private final WhatsAppChannelJdbcRepository repository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;
	private final List<CanalWhatsApp> canalesWhatsApp;
	private final WhatsAppChannelProperties properties;

	public WhatsAppChannelAdministrationService(WhatsAppChannelJdbcRepository repository,
			AuditLogJdbcRepository auditLogJdbcRepository, List<CanalWhatsApp> canalesWhatsApp,
			WhatsAppChannelProperties properties) {
		this.repository = repository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
		this.canalesWhatsApp = canalesWhatsApp;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public WhatsAppChannelStatusResponse getStatus(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
		WhatsAppSessionStatus session = canalWhatsApp.getStatus();
		WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount = repository
				.findChannelAccountByBusinessId(authenticatedUser.businessId()).orElse(null);
		String phoneNumberId = repository.findPhoneNumberIdByBusinessId(authenticatedUser.businessId()).orElse(null);

		String phoneNumber = resolveDisplayPhone(channelAccount != null ? channelAccount.phoneNumber() : null);
		if (phoneNumber == null) {
			phoneNumber = resolveDisplayPhone(session.phoneNumber());
		}
		OffsetDateTime lastEventAt = session.lastEventAt() != null
				? session.lastEventAt()
				: channelAccount != null ? channelAccount.lastEventAt() : null;

		List<WhatsAppChannelRecentEvent> recentEvents = repository.findRecentEvents(authenticatedUser.businessId(), 10)
				.stream().map(event -> new WhatsAppChannelRecentEvent(event.deliveryId(), event.eventType(),
						event.processingStatus(), event.receivedAt(), event.processedAt()))
				.toList();
		int recentErrorCount = repository.countRecentEventErrors(authenticatedUser.businessId(), 24 * 60);

		return new WhatsAppChannelStatusResponse(canalWhatsApp.provider().name(),
				normalizeSessionStatus(session.connectionStatus()), phoneNumber, phoneNumberId, session.adapterMode(),
				lastEventAt, true, recentEvents.size(), recentErrorCount, recentEvents,
				buildStatusMessage(canalWhatsApp.provider()));
	}

	@Transactional
	public WhatsAppChannelActionResponse connect(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
		WhatsAppSessionAction response = canalWhatsApp.connect();
		OffsetDateTime acceptedAt = response.acceptedAt() == null
				? OffsetDateTime.now(ZoneOffset.UTC)
				: response.acceptedAt();
		String displayPhone = resolveDisplayPhone(response.phoneNumber());

		persistSessionState(authenticatedUser.businessId(), response, displayPhone, acceptedAt);
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
				"WHATSAPP_CHANNEL_CONNECT_REQUESTED", "CHANNEL_ACCOUNT", null,
				"Se solicito la conexion del canal WhatsApp del proveedor activo.",
				Map.of("provider", canalWhatsApp.provider().name(), "connectionStatus",
						normalizeSessionStatus(response.connectionStatus())),
				acceptedAt);
		return new WhatsAppChannelActionResponse(normalizeSessionStatus(response.connectionStatus()), displayPhone,
				acceptedAt, canalWhatsApp.provider().name());
	}

	@Transactional
	public WhatsAppChannelActionResponse disconnect(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
		WhatsAppSessionAction response = canalWhatsApp.disconnect();
		OffsetDateTime acceptedAt = response.acceptedAt() == null
				? OffsetDateTime.now(ZoneOffset.UTC)
				: response.acceptedAt();
		String displayPhone = resolveDisplayPhone(response.phoneNumber());

		persistSessionState(authenticatedUser.businessId(), response, displayPhone, acceptedAt);
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
				"WHATSAPP_CHANNEL_DISCONNECTED", "CHANNEL_ACCOUNT", null,
				"Se solicito la desconexion del canal WhatsApp del proveedor activo.",
				Map.of("provider", canalWhatsApp.provider().name(), "connectionStatus",
						normalizeSessionStatus(response.connectionStatus())),
				acceptedAt);
		return new WhatsAppChannelActionResponse(normalizeSessionStatus(response.connectionStatus()), displayPhone,
				acceptedAt, canalWhatsApp.provider().name());
	}

	@Transactional
	public WhatsAppChannelTestMessageResponse sendTestMessage(AuthenticatedUser authenticatedUser,
			WhatsAppChannelTestMessageRequest request) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
		WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount = loadChannelAccount(
				authenticatedUser.businessId());
		String normalizedPhone = normalizePhone(request.recipientPhone());
		WhatsAppChannelJdbcRepository.CustomerRecord customer = repository
				.findCustomerByPhone(authenticatedUser.businessId(), normalizedPhone)
				.orElseGet(() -> new WhatsAppChannelJdbcRepository.CustomerRecord(repository
						.insertCustomer(authenticatedUser.businessId(), normalizedPhone, "Contacto de prueba"),
						"Contacto de prueba", normalizedPhone, normalizedPhone));

		WhatsAppChannelJdbcRepository.ConversationRecord conversation = repository
				.findLatestConversation(authenticatedUser.businessId(), channelAccount.id(), customer.id()).orElseGet(
						() -> new WhatsAppChannelJdbcRepository.ConversationRecord(
								repository.insertConversation(authenticatedUser.businessId(), channelAccount.id(),
										customer.id(), authenticatedUser.userId(), customer.displayName(),
										normalizedPhone, OffsetDateTime.now(ZoneOffset.UTC)),
								authenticatedUser.userId(), 0, null, null));

		UUID messageId = repository.insertOutboundMessage(authenticatedUser.businessId(), conversation.id(),
				authenticatedUser.userId(), request.body().trim(), OffsetDateTime.now(ZoneOffset.UTC));

		try {
			ChannelDelivery channelDelivery = canalWhatsApp
					.send(new OutboundMessage(authenticatedUser.businessId(), normalizedPhone, request.body().trim()));
			OffsetDateTime acceptedAt = OffsetDateTime.ofInstant(channelDelivery.acceptedAt(), ZoneOffset.UTC);
			repository.updateOutboundMessageAccepted(messageId, channelDelivery.externalMessageId(),
					normalizeDeliveryStatus(channelDelivery.status()), acceptedAt);
			repository.insertMessageDeliveryLog(authenticatedUser.businessId(), messageId,
					normalizeDeliveryStatus(channelDelivery.status()), channelDelivery.externalMessageId(),
					Map.of("channelType", channelDelivery.channelType().name(), "acceptedAt", acceptedAt), acceptedAt);
			repository.updateConversationOutboundActivity(conversation.id(), request.body().trim(), acceptedAt);

			auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
					"WHATSAPP_CHANNEL_TEST_MESSAGE_SENT", "MESSAGE", messageId,
					"Se envio un mensaje de prueba por el canal WhatsApp activo.",
					Map.of("recipientPhone", normalizedPhone, "externalMessageId", channelDelivery.externalMessageId()),
					acceptedAt);

			return new WhatsAppChannelTestMessageResponse(conversation.id(), messageId,
					channelDelivery.externalMessageId(), normalizeDeliveryStatus(channelDelivery.status()), acceptedAt);
		} catch (MessagingChannelUnavailableException exception) {
			OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC);
			repository.updateOutboundMessageFailed(messageId, "WHATSAPP_CHANNEL_UNAVAILABLE", failedAt);
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WHATSAPP_CHANNEL_UNAVAILABLE",
					"No fue posible enviar el mensaje de prueba por el canal WhatsApp activo.");
		}
	}

	private CanalWhatsApp resolveCanalWhatsApp() {
		WhatsAppChannelProperties.Provider configured = properties.getProvider();
		WhatsAppChannelProvider expected = switch (configured) {
			case META_CLOUD_API -> WhatsAppChannelProvider.META_CLOUD_API;
			case SIMULATED -> WhatsAppChannelProvider.SIMULATED;
		};
		return canalesWhatsApp.stream().filter(channel -> channel.provider() == expected).findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WHATSAPP_CHANNEL_DISABLED",
						"El puerto interno CanalWhatsApp no esta disponible para este ambiente."));
	}

	private void persistSessionState(UUID businessId, WhatsAppSessionAction response, String displayPhone,
			OffsetDateTime acceptedAt) {
		WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount = repository
				.findChannelAccountByBusinessId(businessId).orElse(null);
		UUID channelAccountId;
		if (channelAccount == null) {
			channelAccountId = repository.upsertSimulatedChannelAccount(businessId, "simulated", "56900000000",
					"simulated-phone-id", "DISCONNECTED");
		} else {
			channelAccountId = channelAccount.id();
		}
		repository.updateChannelAccount(channelAccountId, normalizeSessionStatus(response.connectionStatus()),
				displayPhone, response.qrCode(), acceptedAt);
	}

	private WhatsAppChannelJdbcRepository.ChannelAccountRecord loadChannelAccount(UUID businessId) {
		return repository.findChannelAccountByBusinessId(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND",
						"No se encontro la configuracion de canal WhatsApp para la empresa actual."));
	}

	private String buildStatusMessage(WhatsAppChannelProvider provider) {
		return switch (provider) {
			case META_CLOUD_API -> "WhatsApp Cloud API de Meta. Verifique el webhook, el token y el numero vinculados.";
			case SIMULATED ->
				"Modo simulado: los mensajes no se envian por WhatsApp. Use el proveedor META_CLOUD_API para envios reales.";
		};
	}

	private String resolveDisplayPhone(String rawPhone) {
		if (rawPhone == null || rawPhone.isBlank()) {
			return null;
		}
		String digits = rawPhone.replaceAll("\\D", "");
		return digits.isBlank() ? null : digits;
	}

	private String normalizeSessionStatus(String sessionStatus) {
		if (sessionStatus == null || sessionStatus.isBlank()) {
			return "ERROR";
		}
		return switch (sessionStatus) {
			case "CONNECTED", "DISCONNECTED", "QR_PENDING", "SYNCING", "ERROR" -> sessionStatus;
			default -> "ERROR";
		};
	}

	private String normalizeDeliveryStatus(String deliveryStatus) {
		if (deliveryStatus == null || deliveryStatus.isBlank()) {
			return "QUEUED";
		}
		return switch (deliveryStatus) {
			case "PENDING", "QUEUED", "PROVIDER_ACCEPTED", "SENT", "DELIVERED", "READ", "FAILED", "SIMULATED",
					"DRY_RUN" ->
				deliveryStatus;
			default -> "QUEUED";
		};
	}

	private String normalizePhone(String rawPhone) {
		return rawPhone.trim().replace(" ", "");
	}
}
