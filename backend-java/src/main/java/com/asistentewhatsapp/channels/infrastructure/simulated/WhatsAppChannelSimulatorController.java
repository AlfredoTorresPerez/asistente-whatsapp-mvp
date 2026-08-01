package com.asistentewhatsapp.channels.infrastructure.simulated;

import com.asistentewhatsapp.channels.application.WhatsAppInboundMessageService;
import com.asistentewhatsapp.channels.domain.WhatsAppInboundMessageEvent;
import com.asistentewhatsapp.channels.domain.WhatsAppMessageType;
import com.asistentewhatsapp.channels.infrastructure.WhatsAppChannelJdbcRepository;
import com.asistentewhatsapp.shared.api.StatusResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulador de mensajes entrantes de WhatsApp para pruebas E2E y demos locales.
 * Recibe un mensaje y lo procesa a traves del mismo pipeline del webhook de
 * WhatsApp Cloud API (persistencia, idempotencia, agentes IA), sin requerir
 * credenciales de Meta ni red externa.
 */
@RestController
@RequestMapping(value = "/api/v1/test", produces = MediaType.APPLICATION_JSON_VALUE)
public class WhatsAppChannelSimulatorController {

	private static final String DEFAULT_PHONE_NUMBER_ID = "simulated-phone-id";

	private final WhatsAppChannelJdbcRepository repository;
	private final WhatsAppInboundMessageService inboundMessageService;

	public WhatsAppChannelSimulatorController(WhatsAppChannelJdbcRepository repository,
			WhatsAppInboundMessageService inboundMessageService) {
		this.repository = repository;
		this.inboundMessageService = inboundMessageService;
	}

	@PostMapping("/whatsapp-inbound")
	public StatusResponse simulateInbound(@RequestBody SimulatedIncomingMessageRequest request) {
		if (request.from() == null || request.from().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'from' es obligatorio.");
		}
		if (request.body() == null || request.body().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'body' es obligatorio.");
		}

		String from = request.from().startsWith("+") ? request.from() : "+" + request.from();
		String phoneNumberId = request.phoneNumberId() != null && !request.phoneNumberId().isBlank()
				? request.phoneNumberId()
				: DEFAULT_PHONE_NUMBER_ID;

		WhatsAppChannelJdbcRepository.ChannelAccountRecord channelAccount = repository
				.findChannelAccountByPhoneNumberId(phoneNumberId)
				.orElseGet(() -> resolveSimulatedChannel(request, phoneNumberId));

		UUID businessId = channelAccount.businessId();
		String messageId = request.externalMessageId() != null && !request.externalMessageId().isBlank()
				? request.externalMessageId()
				: "sim-" + UUID.randomUUID();
		String deliveryId = "sim-" + UUID.randomUUID();
		OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);

		WhatsAppInboundMessageEvent event = new WhatsAppInboundMessageEvent(messageId, from, null, request.body(),
				WhatsAppMessageType.TEXT, occurredAt, null, null, null, null, null, null, null);

		repository.insertChannelEventLog(businessId, channelAccount.id(), deliveryId, "WHATSAPP_SIMULATED_MESSAGE",
				"{\"from\":\"" + from + "\"}", occurredAt);
		inboundMessageService.processInboundMessage(event, businessId, channelAccount.id(), deliveryId);

		return new StatusResponse("ACCEPTED");
	}

	private WhatsAppChannelJdbcRepository.ChannelAccountRecord resolveSimulatedChannel(
			SimulatedIncomingMessageRequest request, String phoneNumberId) {
		UUID businessId = null;
		if (request.businessId() != null && !request.businessId().isBlank()) {
			try {
				businessId = UUID.fromString(request.businessId());
			} catch (IllegalArgumentException exception) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessId invalido.");
			}
		}
		if (businessId == null) {
			businessId = repository.findFirstChannelAccount()
					.map(WhatsAppChannelJdbcRepository.ChannelAccountRecord::businessId).orElse(null);
		}
		if (businessId == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"No hay canales configurados para simular mensajes. Proporcione businessId o registre un canal.");
		}

		String sessionKey = request.sessionKey() != null && !request.sessionKey().isBlank()
				? request.sessionKey()
				: "simulated";
		repository.upsertSimulatedChannelAccount(businessId, sessionKey, "56900000000", phoneNumberId, "SIMULATED");
		return repository.findChannelAccountByPhoneNumberId(phoneNumberId)
				.orElseThrow(() -> new IllegalStateException("No se pudo crear el canal simulado."));
	}

	public record SimulatedIncomingMessageRequest(String sessionKey, String from, String body, String externalMessageId,
			String businessId, String phoneNumberId) {
	}
}
