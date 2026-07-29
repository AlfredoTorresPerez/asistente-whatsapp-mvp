package com.asistentewhatsapp.channels.infrastructure.openwa;

import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.MessagingChannel;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.channels.openwa", name = "enabled", havingValue = "true")
public class OpenWaMessagingChannel implements MessagingChannel {

	private final RestClient restClient;

	public OpenWaMessagingChannel(RestClient.Builder restClientBuilder, OpenWaClientProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("X-API-Key", properties.apiKey()).build();
	}

	@Override
	public MessageChannelType type() {
		return MessageChannelType.WHATSAPP;
	}

	@Override
	public ChannelDelivery send(OutboundMessage outboundMessage) {
		try {
			OpenWaSendMessageResponse response = restClient.post().uri("/api/v1/messages/send")
					.body(new OpenWaSendMessageRequest(outboundMessage.businessId().toString(),
							outboundMessage.recipientPhone(), outboundMessage.body()))
					.retrieve().body(OpenWaSendMessageResponse.class);

			if (response == null) {
				throw new MessagingChannelUnavailableException("El adaptador OpenWA no devolvio una respuesta valida.");
			}

			return new ChannelDelivery(MessageChannelType.WHATSAPP, response.messageId(), response.status(),
					response.acceptedAt());
		} catch (RestClientException exception) {
			throw new MessagingChannelUnavailableException("No fue posible entregar el mensaje al adaptador OpenWA.");
		}
	}

	private record OpenWaSendMessageRequest(String businessId, String to, String body) {
	}

	private record OpenWaSendMessageResponse(String messageId, String status, Instant acceptedAt) {
	}
}
