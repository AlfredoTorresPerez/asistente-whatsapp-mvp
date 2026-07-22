package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.cloudapi.infrastructure.CloudApiTokenEncryptionService;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppCloudApiAdapterTest {

    @Mock
    private WhatsAppCloudApiMetrics metrics;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MetaOnboardingRepository onboardingRepository;
    @Mock
    private CloudApiTokenEncryptionService tokenEncryption;

    private final UUID businessId = UUID.randomUUID();

    private WhatsAppCloudApiProperties properties(
            String accessToken, String phoneNumberId, boolean dryRun) {
        return new WhatsAppCloudApiProperties(
                true, "https://graph.facebook.com", "v23.0",
                null, null, null, null, true, null,
                phoneNumberId, null, accessToken,
                "56911112222", dryRun, 5, 15);
    }

    private WhatsAppCloudApiAdapter createAdapter(boolean dryRun) {
        return new WhatsAppCloudApiAdapter(
                RestClient.builder(),
                properties("token", "12345", dryRun),
                metrics, objectMapper, onboardingRepository, tokenEncryption);
    }

    @Test
    void dryRunReturnsSimulatedDelivery() {
        WhatsAppCloudApiAdapter adapter = createAdapter(true);

        ChannelDelivery delivery = adapter.send(message());

        assertThat(delivery.channelType()).isEqualTo(MessageChannelType.WHATSAPP);
        assertThat(delivery.status()).isEqualTo("SIMULATED");
        assertThat(delivery.externalMessageId()).startsWith("cloud-dry-run-");
    }

    @Test
    void dryRunTemplateReturnsSimulatedId() {
        WhatsAppCloudApiAdapter adapter = createAdapter(true);

        String result = adapter.sendTemplate(
                businessId, "56911112222", "test_template", "es", null, null);

        assertThat(result).startsWith("cloud-dry-run-template-");
    }

    @Test
    void sendRejectsEmptyRecipient() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThatThrownBy(() -> adapter.send(
                new OutboundMessage(businessId, "", "Hola")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendRejectsEmptyBody() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThatThrownBy(() -> adapter.send(
                new OutboundMessage(businessId, "56911112222", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendFailsWhenNoChannelConfigured() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);
        when(onboardingRepository.findCloudApiChannel(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.send(message()))
                .isInstanceOf(MessagingChannelUnavailableException.class)
                .hasMessageContaining("No hay canal META_CLOUD_API activo");
    }

    @Test
    void getStatusReturnsConnected() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThat(adapter.getStatus().connectionStatus()).isEqualTo("CONNECTED");
    }

    @Test
    void connectReturnsStatusInfo() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThat(adapter.connect().connectionStatus()).isNotNull();
    }

    @Test
    void disconnectReturnsDisconnected() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThat(adapter.disconnect().connectionStatus()).isEqualTo("DISCONNECTED");
    }

    @Test
    void templateSendFailsWhenNoChannelConfigured() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);
        when(onboardingRepository.findCloudApiChannel(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.sendTemplate(
                businessId, "56911112222", "template", "es", null, null))
                .isInstanceOf(MessagingChannelUnavailableException.class)
                .hasMessageContaining("No hay canal META_CLOUD_API activo");
    }

    @Test
    void templateSendRejectsEmptyRecipient() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThatThrownBy(() -> adapter.sendTemplate(
                businessId, "", "template", "es", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void templateSendRejectsEmptyName() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThatThrownBy(() -> adapter.sendTemplate(
                businessId, "56911112222", "", "es", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void providerIsCloudApi() {
        WhatsAppCloudApiAdapter adapter = createAdapter(false);

        assertThat(adapter.provider()).isEqualTo(WhatsAppChannelProvider.META_CLOUD_API);
    }

    @Test
    void multiTenantSendsWithPerBusinessCredentials() {
        MetaOnboardingRepository.ChannelAccountRecord channel = createChannelRecord(
                businessId, "12345", "encrypted_token_abc", "56911112222");

        WhatsAppCloudApiAdapter adapter = createAdapter(false);
        when(onboardingRepository.findCloudApiChannel(businessId))
                .thenReturn(Optional.of(channel));
        when(tokenEncryption.decrypt("encrypted_token_abc")).thenReturn("decrypted_token");

        // The actual HTTP call will fail because there's no mock server,
        // but we just verify it reaches the right code path
        assertThatThrownBy(() -> adapter.send(message()))
                .hasMessageNotContaining("No hay canal META_CLOUD_API");
    }

    private MetaOnboardingRepository.ChannelAccountRecord createChannelRecord(
            UUID businessId, String phoneNumberId, String encryptedToken, String phoneNumber) {
        return new MetaOnboardingRepository.ChannelAccountRecord(
                UUID.randomUUID(), businessId, "META_CLOUD_API", "WHATSAPP",
                "CONNECTED", phoneNumber, phoneNumber, phoneNumber,
                phoneNumberId, "waba_123", "REGISTERED", "CONNECTED",
                "SUBSCRIBED", "CONFIGURED", true, encryptedToken, null,
                OffsetDateTime.now().plusDays(60), null, "CENTRALIZED",
                "META_CLOUD_API_CLOUD_API", null, null, OffsetDateTime.now(), 0,
                OffsetDateTime.now(), OffsetDateTime.now(), null, null);
    }

    private OutboundMessage message() {
        return new OutboundMessage(businessId, "+56911112222", "Hola");
    }
}
