package com.asistentewhatsapp.configuration.application;

import com.asistentewhatsapp.administration.api.WhatsAppChannelStatusResponse;
import com.asistentewhatsapp.administration.application.WhatsAppChannelAdministrationService;
import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationChannelResponse;
import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationLinkedDeviceResponse;
import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationPreferencesRequest;
import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationPreferencesResponse;
import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationResponse;
import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationSessionHistoryResponse;
import com.asistentewhatsapp.configuration.infrastructure.WhatsAppConfigurationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppConfigurationService {

	private static final String LOCAL_LOCATION = "Ambiente local";

	private final WhatsAppConfigurationJdbcRepository repository;
	private final WhatsAppChannelAdministrationService whatsAppChannelAdministrationService;

	public WhatsAppConfigurationService(WhatsAppConfigurationJdbcRepository repository,
			WhatsAppChannelAdministrationService whatsAppChannelAdministrationService) {
		this.repository = repository;
		this.whatsAppChannelAdministrationService = whatsAppChannelAdministrationService;
	}

	@Transactional
	public WhatsAppConfigurationResponse getConfiguration(AuthenticatedUser authenticatedUser) {
		WhatsAppConfigurationJdbcRepository.BusinessRecord business = repository
				.findBusiness(authenticatedUser.businessId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
						"BUSINESS_NOT_FOUND", "No se encontro la empresa autenticada para cargar configuracion."));
		WhatsAppConfigurationJdbcRepository.ChannelAccountDetailRecord channelAccount = repository
				.findChannelAccount(authenticatedUser.businessId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WHATSAPP_CHANNEL_NOT_FOUND",
						"No se encontro el canal WhatsApp para la empresa actual."));
		WhatsAppConfigurationJdbcRepository.PreferencesRecord preferences = repository
				.findOrCreatePreferences(authenticatedUser.businessId());
		WhatsAppChannelStatusResponse status = whatsAppChannelAdministrationService.getStatus(authenticatedUser);
		String resolvedPhone = firstNotBlank(status.phoneNumber(), channelAccount.phoneNumber());
		OffsetDateTime lastSynchronizationAt = firstNotNull(status.lastEventAt(), channelAccount.lastEventAt(),
				channelAccount.updatedAt());

		return new WhatsAppConfigurationResponse(status.connectionStatus(), resolvedPhone,
				firstNotBlank(business.businessName(), business.companyName()), lastSynchronizationAt,
				calculateActiveSessionHours(status.connectionStatus(), channelAccount.connectedAt()),
				resolveConnectedFrom(status.provider()), null, status.active(), status.adapterMode(), status.message(),
				toPreferencesResponse(preferences),
				toMainChannel(channelAccount, resolvedPhone, preferences.outOfHoursMessage()),
				toLinkedDevices(channelAccount, status, lastSynchronizationAt), toSessionHistory(channelAccount,
						repository.findRecentSessionEvents(authenticatedUser.businessId(), 8)));
	}

	@Transactional
	public WhatsAppConfigurationResponse updatePreferences(AuthenticatedUser authenticatedUser,
			WhatsAppConfigurationPreferencesRequest request) {
		repository.updatePreferences(authenticatedUser.businessId(), request);
		return getConfiguration(authenticatedUser);
	}

	@Transactional
	public WhatsAppConfigurationResponse connect(AuthenticatedUser authenticatedUser) {
		whatsAppChannelAdministrationService.connect(authenticatedUser);
		return getConfiguration(authenticatedUser);
	}

	@Transactional
	public WhatsAppConfigurationResponse disconnect(AuthenticatedUser authenticatedUser) {
		whatsAppChannelAdministrationService.disconnect(authenticatedUser);
		return getConfiguration(authenticatedUser);
	}

	private WhatsAppConfigurationPreferencesResponse toPreferencesResponse(
			WhatsAppConfigurationJdbcRepository.PreferencesRecord preferences) {
		return new WhatsAppConfigurationPreferencesResponse(preferences.newMessageNotifications(),
				preferences.autoReassignment(), preferences.agentSignature(), preferences.outOfHoursMessage());
	}

	private WhatsAppConfigurationChannelResponse toMainChannel(
			WhatsAppConfigurationJdbcRepository.ChannelAccountDetailRecord channelAccount, String phoneNumber,
			boolean outOfHoursMessage) {
		return new WhatsAppConfigurationChannelResponse("GroupUp WhatsApp", phoneNumber,
				resolveChannelType(channelAccount.providerName()),
				outOfHoursMessage
						? "Lun a Vie: 8:00 a. m. - 6:00 p. m. / Sab: 9:00 a. m. - 1:00 p. m."
						: "Sin respuesta fuera de horario",
				channelAccount.active());
	}

	private List<WhatsAppConfigurationLinkedDeviceResponse> toLinkedDevices(
			WhatsAppConfigurationJdbcRepository.ChannelAccountDetailRecord channelAccount,
			WhatsAppChannelStatusResponse status, OffsetDateTime lastSynchronizationAt) {
		List<WhatsAppConfigurationLinkedDeviceResponse> devices = new ArrayList<>();
		devices.add(new WhatsAppConfigurationLinkedDeviceResponse(channelAccount.id().toString(), "Canal principal",
				"Administrador", LOCAL_LOCATION, resolveChannelType(status.provider()),
				toDeviceStatus(status.connectionStatus()), lastSynchronizationAt));
		return devices;
	}

	private List<WhatsAppConfigurationSessionHistoryResponse> toSessionHistory(
			WhatsAppConfigurationJdbcRepository.ChannelAccountDetailRecord channelAccount,
			List<WhatsAppConfigurationJdbcRepository.SessionEventRecord> events) {
		if (events.isEmpty()) {
			OffsetDateTime occurredAt = firstNotNull(channelAccount.lastEventAt(), channelAccount.connectedAt(),
					channelAccount.disconnectedAt(), channelAccount.updatedAt(), OffsetDateTime.now(ZoneOffset.UTC));
			return List.of(new WhatsAppConfigurationSessionHistoryResponse(channelAccount.id().toString(),
					toSessionStatusTitle(channelAccount.status()), "Sistema local",
					toHistoryTone(channelAccount.status()), occurredAt));
		}

		return events.stream()
				.map(event -> new WhatsAppConfigurationSessionHistoryResponse(event.deliveryId(),
						toEventTitle(event.eventType(), event.processingStatus()), "Canal WhatsApp",
						toEventTone(event.eventType(), event.processingStatus()), event.receivedAt()))
				.toList();
	}

	private long calculateActiveSessionHours(String sessionStatus, OffsetDateTime connectedAt) {
		if (!"CONNECTED".equals(sessionStatus) || connectedAt == null) {
			return 0L;
		}
		long hours = Duration.between(connectedAt, OffsetDateTime.now(ZoneOffset.UTC)).toHours();
		return Math.max(hours, 0L);
	}

	private String resolveConnectedFrom(String provider) {
		return switch (firstNotBlank(provider, "SIMULATED")) {
			case "META_CLOUD_API" -> "WhatsApp Cloud API de Meta";
			default -> "Modo simulado local";
		};
	}

	private String resolveChannelType(String providerName) {
		if ("META_CLOUD_API".equals(providerName)) {
			return "WhatsApp Cloud API (Meta)";
		}
		if ("SIMULATED".equals(providerName)) {
			return "Modo simulado";
		}
		return firstNotBlank(providerName, "WhatsApp");
	}

	private String toDeviceStatus(String sessionStatus) {
		return switch (firstNotBlank(sessionStatus, "DISCONNECTED")) {
			case "CONNECTED" -> "ONLINE";
			case "QR_PENDING", "SYNCING" -> "PENDING";
			case "ERROR" -> "ERROR";
			default -> "DISCONNECTED";
		};
	}

	private String toEventTitle(String eventType, String processingStatus) {
		String suffix = "FAILED".equals(processingStatus) ? " con error" : "";
		return switch (firstNotBlank(eventType, "EVENT")) {
			case "MESSAGE_RECEIVED" -> "Mensaje recibido" + suffix;
			case "MESSAGE_ACK_UPDATED" -> "Entrega actualizada" + suffix;
			case "SESSION_STATUS_CHANGED" -> "Estado de sesion actualizado" + suffix;
			default -> "Evento del canal" + suffix;
		};
	}

	private String toEventTone(String eventType, String processingStatus) {
		if ("FAILED".equals(processingStatus)) {
			return "danger";
		}
		return switch (firstNotBlank(eventType, "EVENT")) {
			case "SESSION_STATUS_CHANGED" -> "success";
			case "MESSAGE_RECEIVED", "MESSAGE_ACK_UPDATED" -> "neutral";
			default -> "warning";
		};
	}

	private String toSessionStatusTitle(String status) {
		return switch (firstNotBlank(status, "DISCONNECTED")) {
			case "CONNECTED" -> "Sesion iniciada";
			case "QR_PENDING" -> "QR pendiente de escaneo";
			case "SYNCING" -> "Sesion sincronizando";
			case "ERROR" -> "Sesion con error";
			default -> "Dispositivo desconectado";
		};
	}

	private String toHistoryTone(String status) {
		return switch (firstNotBlank(status, "DISCONNECTED")) {
			case "CONNECTED" -> "success";
			case "QR_PENDING", "SYNCING" -> "warning";
			case "ERROR" -> "danger";
			default -> "neutral";
		};
	}

	@SafeVarargs
	private final <T> T firstNotNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private String firstNotBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
