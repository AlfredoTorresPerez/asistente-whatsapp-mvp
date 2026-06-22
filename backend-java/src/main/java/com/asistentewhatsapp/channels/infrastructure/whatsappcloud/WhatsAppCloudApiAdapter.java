package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionAction;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionStatus;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
public class WhatsAppCloudApiAdapter implements CanalWhatsApp {

    private final WhatsAppCloudApiProperties properties;
    private final RestClient restClient;

    public WhatsAppCloudApiAdapter(RestClient.Builder restClientBuilder, WhatsAppCloudApiProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(properties.baseUrl()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public WhatsAppChannelProvider provider() {
        return WhatsAppChannelProvider.CLOUD_API;
    }

    @Override
    public WhatsAppSessionStatus getStatus() {
        String status = isOperationallyConfigured() || properties.dryRunEnabled() ? "CONNECTED" : "ERROR";
        String adapterMode = properties.dryRunEnabled() ? "WHATSAPP_CLOUD_API_DRY_RUN" : "WHATSAPP_CLOUD_API";
        return new WhatsAppSessionStatus(
                "cloud-api",
                status,
                properties.defaultPhoneNumber(),
                null,
                adapterMode,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public WhatsAppSessionAction connect() {
        WhatsAppSessionStatus status = getStatus();
        return new WhatsAppSessionAction(
                status.sessionId(),
                status.connectionStatus(),
                status.phoneNumber(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public WhatsAppSessionAction refreshQr() {
        WhatsAppSessionStatus status = getStatus();
        return new WhatsAppSessionAction(
                status.sessionId(),
                status.connectionStatus(),
                status.phoneNumber(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public WhatsAppSessionAction disconnect() {
        return new WhatsAppSessionAction(
                "cloud-api",
                "DISCONNECTED",
                properties.defaultPhoneNumber(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public ChannelDelivery send(OutboundMessage outboundMessage) {
        if (properties.dryRunEnabled()) {
            return new ChannelDelivery(
                    MessageChannelType.WHATSAPP,
                    "cloud-dry-run-" + UUID.randomUUID(),
                    "SIMULATED",
                    Instant.now());
        }

        if (!isOperationallyConfigured()) {
            throw new MessagingChannelUnavailableException(
                    "El adaptador WhatsApp Cloud API no esta configurado con accessToken y phoneNumberId.");
        }

        try {
            CloudApiSendMessageResponse response = restClient.post()
                    .uri("/{apiVersion}/{phoneNumberId}/messages", properties.apiVersion(), properties.phoneNumberId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", normalizePhone(outboundMessage.recipientPhone()),
                            "type", "text",
                            "text", Map.of("body", outboundMessage.body())))
                    .retrieve()
                    .body(CloudApiSendMessageResponse.class);

            String externalMessageId = response != null && response.messages() != null && !response.messages().isEmpty()
                    ? response.messages().getFirst().id()
                    : "cloud-api-" + UUID.randomUUID();

            return new ChannelDelivery(
                    MessageChannelType.WHATSAPP,
                    externalMessageId,
                    "PROVIDER_ACCEPTED",
                    Instant.now());
        } catch (RestClientException exception) {
            throw new MessagingChannelUnavailableException(
                    "No fue posible entregar el mensaje a WhatsApp Cloud API.");
        }
    }

    private boolean isOperationallyConfigured() {
        return hasText(properties.accessToken()) && hasText(properties.phoneNumberId());
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return hasText(baseUrl) ? baseUrl : "https://graph.facebook.com";
    }

    private static String normalizePhone(String rawPhone) {
        return String.valueOf(rawPhone).replaceAll("\\D", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CloudApiSendMessageResponse(List<CloudApiMessage> messages) {
    }

    private record CloudApiMessage(String id) {
    }
}
