package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.shared.exception.UnsupportedMessagingChannelException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelDispatchServiceTest {

    @Test
    void dispatchUsesExplicitWebProvider() {
        CanalWhatsApp webChannel = whatsAppChannel(WhatsAppChannelProvider.WHATSAPP_WEB, "web-1", "SENT");
        CanalWhatsApp cloudChannel = whatsAppChannel(WhatsAppChannelProvider.CLOUD_API, "cloud-1", "SENT");
        WhatsAppChannelProperties properties = properties(WhatsAppChannelProperties.Provider.WEB);
        ChannelDispatchService service = new ChannelDispatchService(
                List.of(webChannel, cloudChannel),
                List.of(webChannel, cloudChannel),
                properties);

        ChannelDispatchResponse response = service.dispatch(request());

        assertThat(response.externalMessageId()).isEqualTo("web-1");
    }

    @Test
    void dispatchUsesExplicitCloudProvider() {
        CanalWhatsApp webChannel = whatsAppChannel(WhatsAppChannelProvider.WHATSAPP_WEB, "web-1", "SENT");
        CanalWhatsApp cloudChannel = whatsAppChannel(WhatsAppChannelProvider.CLOUD_API, "cloud-1", "SENT");
        WhatsAppChannelProperties properties = properties(WhatsAppChannelProperties.Provider.CLOUD_API);
        ChannelDispatchService service = new ChannelDispatchService(
                List.of(webChannel, cloudChannel),
                List.of(webChannel, cloudChannel),
                properties);

        ChannelDispatchResponse response = service.dispatch(request());

        assertThat(response.externalMessageId()).isEqualTo("cloud-1");
    }

    @Test
    void dispatchFailsWhenConfiguredProviderIsMissing() {
        CanalWhatsApp webChannel = whatsAppChannel(WhatsAppChannelProvider.WHATSAPP_WEB, "web-1", "SENT");
        WhatsAppChannelProperties properties = properties(WhatsAppChannelProperties.Provider.CLOUD_API);
        ChannelDispatchService service = new ChannelDispatchService(
                List.of(webChannel),
                List.of(webChannel),
                properties);

        assertThatThrownBy(() -> service.dispatch(request()))
                .isInstanceOf(UnsupportedMessagingChannelException.class)
                .hasMessageContaining("CLOUD_API");
    }

    @Test
    void dispatchFailsWhenWhatsAppProviderIsDisabled() {
        CanalWhatsApp webChannel = whatsAppChannel(WhatsAppChannelProvider.WHATSAPP_WEB, "web-1", "SENT");
        ChannelDispatchService service = new ChannelDispatchService(
                List.of(webChannel),
                List.of(webChannel),
                properties(WhatsAppChannelProperties.Provider.DISABLED));

        assertThatThrownBy(() -> service.dispatch(request()))
                .isInstanceOf(UnsupportedMessagingChannelException.class)
                .hasMessageContaining("deshabilitado");
    }

    @Test
    void dispatchPersistsSimulatedStatusFromProvider() {
        CanalWhatsApp webChannel = whatsAppChannel(WhatsAppChannelProvider.WHATSAPP_WEB, "sim-1", "SIMULATED");
        ChannelDispatchService service = new ChannelDispatchService(
                List.of(webChannel),
                List.of(webChannel),
                properties(WhatsAppChannelProperties.Provider.WEB));

        ChannelDispatchResponse response = service.dispatch(request());

        assertThat(response.status()).isEqualTo("SIMULATED");
    }

    private CanalWhatsApp whatsAppChannel(
            WhatsAppChannelProvider provider,
            String externalMessageId,
            String status) {
        CanalWhatsApp channel = mock(CanalWhatsApp.class);
        when(channel.type()).thenReturn(MessageChannelType.WHATSAPP);
        when(channel.provider()).thenReturn(provider);
        when(channel.send(any(OutboundMessage.class)))
                .thenReturn(new ChannelDelivery(MessageChannelType.WHATSAPP, externalMessageId, status, Instant.now()));
        return channel;
    }

    private WhatsAppChannelProperties properties(WhatsAppChannelProperties.Provider provider) {
        WhatsAppChannelProperties properties = new WhatsAppChannelProperties();
        properties.setProvider(provider);
        return properties;
    }

    private ChannelDispatchRequest request() {
        return new ChannelDispatchRequest(
                UUID.randomUUID(),
                MessageChannelType.WHATSAPP,
                "+56911112222",
                "Hola");
    }
}
