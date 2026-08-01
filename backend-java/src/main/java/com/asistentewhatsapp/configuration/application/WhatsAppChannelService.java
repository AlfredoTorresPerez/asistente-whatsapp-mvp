package com.asistentewhatsapp.configuration.application;

import com.asistentewhatsapp.administration.application.AdminAccessGuard;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelStatus;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelResponse;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelResponse.ChannelEventItem;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelResponse.MetaCloudConfig;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelTestMessageRequest;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelTestMessageResponse;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelUpdateRequest;
import com.asistentewhatsapp.configuration.api.WhatsAppChannelValidateResponse;
import com.asistentewhatsapp.configuration.infrastructure.WhatsAppChannelJdbcRepository;
import com.asistentewhatsapp.configuration.infrastructure.WhatsAppChannelJdbcRepository.ChannelRecord;
import com.asistentewhatsapp.configuration.infrastructure.WhatsAppChannelJdbcRepository.EventRecord;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppChannelService {

	private final WhatsAppChannelJdbcRepository repository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;

	public WhatsAppChannelService(WhatsAppChannelJdbcRepository repository,
			AuditLogJdbcRepository auditLogJdbcRepository) {
		this.repository = repository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
	}

	@Transactional(readOnly = true)
	public WhatsAppChannelResponse getChannel(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		UUID businessId = authenticatedUser.businessId();

		ChannelRecord channel = repository.findChannel(businessId).orElse(null);
		if (channel == null) {
			return buildEmptyChannelResponse(businessId);
		}
		return toChannelResponse(channel, repository.findRecentEvents(businessId, 20));
	}

	@Transactional
	public WhatsAppChannelResponse updateChannel(AuthenticatedUser authenticatedUser,
			WhatsAppChannelUpdateRequest request) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		UUID businessId = authenticatedUser.businessId();
		ChannelRecord channel = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND",
						"No se encontro el canal WhatsApp configurado."));

		repository.updateChannelConfig(channel.id(), request.displayPhoneNumber(), request.normalizedPhoneNumber(),
				request.phoneNumberId(), request.businessAccountId(), request.graphApiVersion(),
				request.webhookCallbackUrl());

		auditLogJdbcRepository.insert(businessId, authenticatedUser.userId(), "WHATSAPP_CHANNEL_UPDATED",
				"CHANNEL_ACCOUNT", channel.id(), "Se actualizo la configuracion del canal WhatsApp.",
				Map.of("providerType", channel.providerName()), OffsetDateTime.now(ZoneOffset.UTC));

		ChannelRecord updated = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND", ""));
		return toChannelResponse(updated, repository.findRecentEvents(businessId, 20));
	}

	@Transactional
	public WhatsAppChannelValidateResponse validateConfiguration(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		UUID businessId = authenticatedUser.businessId();
		ChannelRecord channel = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND",
						"No se encontro canal configurado para validar."));

		repository.updateHealthCheck(businessId, OffsetDateTime.now(ZoneOffset.UTC));

		if (WhatsAppChannelStatus.PROVIDER_META_CLOUD_API.equals(channel.providerName())) {
			return validateCloudApi(channel);
		}
		return new WhatsAppChannelValidateResponse(true, channel.providerName(),
				"Canal local configurado correctamente.", List.of());
	}

	@Transactional
	public WhatsAppChannelTestMessageResponse sendTestMessage(AuthenticatedUser authenticatedUser,
			WhatsAppChannelTestMessageRequest request) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WHATSAPP_TEST_MESSAGE_UNAVAILABLE",
				"El envio de mensajes de prueba se encuentra en implementacion. Usa el simulador disponible.");
	}

	@Transactional
	public WhatsAppChannelResponse activateChannel(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		UUID businessId = authenticatedUser.businessId();
		ChannelRecord channel = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND",
						"No se encontro canal configurado para activar."));

		repository.updateActive(businessId, true);

		auditLogJdbcRepository.insert(businessId, authenticatedUser.userId(), "WHATSAPP_CHANNEL_ACTIVATED",
				"CHANNEL_ACCOUNT", channel.id(), "Se activo el canal WhatsApp.",
				Map.of("providerType", channel.providerName()), OffsetDateTime.now(ZoneOffset.UTC));

		ChannelRecord updated = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND", ""));
		return toChannelResponse(updated, repository.findRecentEvents(businessId, 20));
	}

	@Transactional
	public WhatsAppChannelResponse deactivateChannel(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		UUID businessId = authenticatedUser.businessId();
		ChannelRecord channel = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND",
						"No se encontro canal configurado para desactivar."));

		repository.updateActive(businessId, false);

		auditLogJdbcRepository.insert(businessId, authenticatedUser.userId(), "WHATSAPP_CHANNEL_DEACTIVATED",
				"CHANNEL_ACCOUNT", channel.id(), "Se desactivo el canal WhatsApp.",
				Map.of("providerType", channel.providerName()), OffsetDateTime.now(ZoneOffset.UTC));

		ChannelRecord updated = repository.findChannel(businessId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND", ""));
		return toChannelResponse(updated, repository.findRecentEvents(businessId, 20));
	}

	private WhatsAppChannelValidateResponse validateCloudApi(ChannelRecord channel) {
		if (!WhatsAppChannelStatus.CREDENTIAL_CONFIGURED.equals(channel.credentialStatus())) {
			return new WhatsAppChannelValidateResponse(false, WhatsAppChannelStatus.PROVIDER_META_CLOUD_API,
					"La credencial de acceso no esta configurada. Configura el token en las variables de entorno.",
					List.of("Credencial no configurada"));
		}
		if (channel.phoneNumberId() == null || channel.phoneNumberId().isBlank()) {
			return new WhatsAppChannelValidateResponse(false, WhatsAppChannelStatus.PROVIDER_META_CLOUD_API,
					"El identificador tecnico del numero no esta configurado.", List.of("Falta phone_number_id"));
		}
		return new WhatsAppChannelValidateResponse(true, WhatsAppChannelStatus.PROVIDER_META_CLOUD_API,
				"La configuracion de Meta Cloud API es valida.", List.of());
	}

	private WhatsAppChannelResponse buildEmptyChannelResponse(UUID businessId) {
		String businessName = repository.findBusinessName(businessId);
		return new WhatsAppChannelResponse(null, "No configurado", businessName, null, null, null, null, null, null,
				null, null, null, null, false, null, null, null, null, null, null, null, List.of());
	}

	private WhatsAppChannelResponse toChannelResponse(ChannelRecord record, List<EventRecord> events) {
		String providerType = record.providerName();
		boolean isCloud = WhatsAppChannelStatus.PROVIDER_META_CLOUD_API.equals(providerType);

		String providerLabel = isCloud ? "Meta WhatsApp Cloud API" : "Modo simulado local";

		MetaCloudConfig cloudConfig = null;
		if (isCloud) {
			cloudConfig = new MetaCloudConfig(record.phoneNumberId(), record.providerAccountId(),
					record.graphApiVersion(), record.webhookCallbackUrl(), record.webhookStatus(),
					toWebhookLabel(record.webhookStatus()), record.credentialStatus(),
					toCredentialLabel(record.credentialStatus()), record.tokenExpiresAt(), record.active());
		}

		List<ChannelEventItem> eventItems = events.stream()
				.map(e -> new ChannelEventItem(e.deliveryId(), e.eventType(), toEventTitle(e.eventType()),
						e.eventType(), resolveActor(e.eventType()), toEventTone(e.eventType()), e.receivedAt()))
				.toList();

		return new WhatsAppChannelResponse(providerType, providerLabel, record.businessName(),
				record.displayPhoneNumber(), record.normalizedPhoneNumber(), record.registrationStatus(),
				toRegistrationLabel(record.registrationStatus()), record.operationalStatus(),
				toOperationalLabel(record.operationalStatus()), record.webhookStatus(),
				toWebhookLabel(record.webhookStatus()), record.credentialStatus(),
				toCredentialLabel(record.credentialStatus()), record.active(), record.lastHealthCheckAt(),
				record.lastMessageReceivedAt(), record.lastMessageSentAt(), record.lastErrorCode(),
				record.lastErrorMessage(), record.updatedAt(), cloudConfig, eventItems);
	}

	private String toRegistrationLabel(String status) {
		if (status == null)
			return "No configurado";
		return switch (status) {
			case "NOT_CONFIGURED" -> "No configurado";
			case "PENDING" -> "Pendiente";
			case "REGISTERED" -> "Registrado";
			case "ERROR" -> "Error";
			default -> status;
		};
	}

	private String toOperationalLabel(String status) {
		if (status == null)
			return "Inactivo";
		return switch (status) {
			case "INACTIVE" -> "Inactivo";
			case "CONFIGURING" -> "Configurando";
			case "CONNECTED" -> "Conectado";
			case "DEGRADED" -> "Degradado";
			case "DISCONNECTED" -> "Desconectado";
			case "ERROR" -> "Error";
			default -> status;
		};
	}

	private String toWebhookLabel(String status) {
		if (status == null)
			return "No configurado";
		return switch (status) {
			case "NOT_CONFIGURED" -> "No configurado";
			case "PENDING_VALIDATION" -> "Validacion pendiente";
			case "VERIFIED" -> "Verificado";
			case "SUBSCRIBED" -> "Suscrito";
			case "ERROR" -> "Error";
			default -> status;
		};
	}

	private String toCredentialLabel(String status) {
		if (status == null)
			return "No configurado";
		return switch (status) {
			case "NOT_CONFIGURED" -> "No configurado";
			case "CONFIGURED" -> "Configurado";
			case "EXPIRING" -> "Por vencer";
			case "EXPIRED" -> "Vencido";
			case "INVALID" -> "Invalido";
			default -> status;
		};
	}

	private String toEventTitle(String eventType) {
		if (eventType == null)
			return "Evento del canal";
		return switch (eventType) {
			case "CHANNEL_CONFIGURED" -> "Canal configurado";
			case "NUMBER_REGISTERED" -> "Numero registrado";
			case "WEBHOOK_VERIFIED" -> "Webhook validado";
			case "CHANNEL_ACTIVATED" -> "Canal activado";
			case "CHANNEL_DEACTIVATED" -> "Canal desactivado";
			case "CONNECTION_VALIDATED" -> "Conexion validada";
			case "MESSAGE_RECEIVED" -> "Mensaje recibido";
			case "MESSAGE_SENT" -> "Mensaje enviado";
			case "AUTH_ERROR" -> "Error de autenticacion";
			case "CREDENTIAL_EXPIRED" -> "Credencial vencida";
			case "DELIVERY_ERROR" -> "Error de entrega";
			case "SESSION_STATUS_CHANGED" -> "Estado de sesion actualizado";
			case "QR_UPDATED" -> "QR generado";
			case "MESSAGE_ACK_UPDATED" -> "Entrega actualizada";
			default -> "Evento del canal";
		};
	}

	private String resolveActor(String eventType) {
		if (eventType == null)
			return "Sistema";
		return switch (eventType) {
			case "MESSAGE_RECEIVED", "MESSAGE_SENT", "DELIVERY_ERROR" -> "WhatsApp";
			case "QR_UPDATED", "SESSION_STATUS_CHANGED", "MESSAGE_ACK_UPDATED" -> "Canal WhatsApp";
			default -> "Sistema";
		};
	}

	private String toEventTone(String eventType) {
		if (eventType == null)
			return "neutral";
		return switch (eventType) {
			case "CHANNEL_ACTIVATED", "NUMBER_REGISTERED", "WEBHOOK_VERIFIED", "CONNECTION_VALIDATED" -> "success";
			case "MESSAGE_RECEIVED", "MESSAGE_SENT" -> "info";
			case "CHANNEL_CONFIGURED", "QR_UPDATED" -> "neutral";
			case "AUTH_ERROR", "CREDENTIAL_EXPIRED", "DELIVERY_ERROR", "CHANNEL_DEACTIVATED" -> "danger";
			case "SESSION_STATUS_CHANGED", "MESSAGE_ACK_UPDATED" -> "neutral";
			default -> "neutral";
		};
	}
}
