package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppCloudApiAdapterTest {

    @Test
    void sendFailsSafelyWhenCredentialsAreMissing() {
        WhatsAppCloudApiAdapter adapter = new WhatsAppCloudApiAdapter(
                RestClient.builder(),
                new WhatsAppCloudApiProperties(
                        true,
                        "https://graph.facebook.com",
                        "v23.0",
                        "",
                        "",
                        "56911112222",
                        false));

        assertThatThrownBy(() -> adapter.send(message()))
                .isInstanceOf(MessagingChannelUnavailableException.class)
                .hasMessageContaining("accessToken");
    }

    @Test
    void dryRunReturnsSimulatedDelivery() {
        WhatsAppCloudApiAdapter adapter = new WhatsAppCloudApiAdapter(
                RestClient.builder(),
                new WhatsAppCloudApiProperties(
                        true,
                        "https://graph.facebook.com",
                        "v23.0",
                        "",
                        "",
                        "56911112222",
                        true));

        ChannelDelivery delivery = adapter.send(message());

        assertThat(delivery.channelType()).isEqualTo(MessageChannelType.WHATSAPP);
        assertThat(delivery.status()).isEqualTo("SIMULATED");
        assertThat(delivery.externalMessageId()).startsWith("cloud-dry-run-");
    }

    private OutboundMessage message() {
        return new OutboundMessage(UUID.randomUUID(), "+56911112222", "Hola");
    }
}
