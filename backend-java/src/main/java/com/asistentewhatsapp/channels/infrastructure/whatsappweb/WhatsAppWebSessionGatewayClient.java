package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WhatsAppWebSessionGatewayClient {

	private final RestClient restClient;

	public WhatsAppWebSessionGatewayClient(RestClient.Builder restClientBuilder,
			WhatsAppWebClientProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("X-API-Key", properties.apiKey()).build();
	}

	public SessionStatusResponse getStatus() {
		return exchange(
				() -> restClient.get().uri("/api/v1/session/status").retrieve().body(SessionStatusResponse.class),
				"No fue posible consultar el estado del adaptador WhatsApp Web.");
	}

	public SessionActionResponse connect() {
		return exchange(
				() -> restClient.post().uri("/api/v1/session/connect").retrieve().body(SessionActionResponse.class),
				"No fue posible iniciar la conexion del adaptador WhatsApp Web.");
	}

	public SessionActionResponse refreshQr() {
		return exchange(
				() -> restClient.post().uri("/api/v1/session/refresh-qr").retrieve().body(SessionActionResponse.class),
				"No fue posible solicitar un nuevo QR al adaptador WhatsApp Web.");
	}

	public SessionActionResponse disconnect() {
		return exchange(
				() -> restClient.post().uri("/api/v1/session/disconnect").retrieve().body(SessionActionResponse.class),
				"No fue posible desconectar la sesion experimental WhatsApp Web.");
	}

	public SendTextResponse sendText(String businessId, String recipientPhone, String body) {
		return exchange(() -> restClient.post().uri("/api/v1/messages/send")
				.body(new SendTextRequest(businessId, recipientPhone, body)).retrieve().body(SendTextResponse.class),
				"No fue posible enviar el mensaje al adaptador WhatsApp Web.");
	}

	private <T> T exchange(WhatsAppWebCall<T> callback, String fallbackMessage) {
		try {
			T response = callback.execute();
			if (response == null) {
				throw new MessagingChannelUnavailableException(fallbackMessage);
			}
			return response;
		} catch (RestClientException exception) {
			throw new MessagingChannelUnavailableException(fallbackMessage);
		}
	}

	@FunctionalInterface
	private interface WhatsAppWebCall<T> {
		T execute();
	}

	public record SessionStatusResponse(String sessionId, String connectionStatus, String phoneNumber, String qrCode,
			String adapterMode, java.time.OffsetDateTime lastEventAt) {
	}

	public record SessionActionResponse(String sessionId, String connectionStatus, String phoneNumber, String qrCode,
			java.time.OffsetDateTime acceptedAt) {
	}

	public record SendTextRequest(String businessId, String to, String body) {
	}

	public record SendTextResponse(String messageId, String status, java.time.OffsetDateTime acceptedAt) {
	}
}
