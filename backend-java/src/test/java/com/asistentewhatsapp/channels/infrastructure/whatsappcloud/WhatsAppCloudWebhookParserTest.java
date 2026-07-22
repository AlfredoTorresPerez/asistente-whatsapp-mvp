package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.channels.application.WhatsAppDeliveryStatusService;
import com.asistentewhatsapp.channels.application.WhatsAppInboundMessageService;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppCloudWebhookParserTest {

    @Mock
    private WhatsAppWebChannelJdbcRepository repository;
    @Mock
    private WhatsAppInboundMessageService inboundMessageService;
    @Mock
    private WhatsAppDeliveryStatusService deliveryStatusService;
    @Mock
    private WhatsAppCloudApiMetrics metrics;

    private WhatsAppCloudWebhookParser parser;
    private ObjectMapper objectMapper;

    private final UUID businessId = UUID.randomUUID();
    private final UUID channelAccountId = UUID.randomUUID();
    private final WhatsAppWebChannelJdbcRepository.ChannelAccountRecord accountRecord =
            new WhatsAppWebChannelJdbcRepository.ChannelAccountRecord(
                    channelAccountId, businessId, "cloud-session",
                    "CONNECTED", "56911112222", null, null, OffsetDateTime.now());

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        parser = new WhatsAppCloudWebhookParser(
                objectMapper, repository, inboundMessageService, deliveryStatusService, metrics);
    }

    private String loadFixture(String name) throws IOException {
        InputStream is = getClass().getResourceAsStream(
                "/fixtures/whatsappcloud/" + name);
        if (is == null) {
            throw new IOException("Fixture not found: " + name);
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void textMessageIsProcessed() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        String body = loadFixture("webhook-text-message.json");
        parser.parseAndProcess(body);
        verify(inboundMessageService, times(1))
                .processInboundMessage(any(), eq(businessId), eq(channelAccountId), anyString());
    }

    @Test
    void interactiveButtonIsProcessed() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        String body = loadFixture("webhook-interactive-button.json");
        parser.parseAndProcess(body);
        verify(inboundMessageService, times(1))
                .processInboundMessage(any(), eq(businessId), eq(channelAccountId), anyString());
    }

    @Test
    void statusSentIsProcessed() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        String body = loadFixture("webhook-status-sent.json");
        parser.parseAndProcess(body);
        verify(deliveryStatusService, times(1))
                .processDeliveryStatus(any(), eq(businessId));
    }

    @Test
    void statusFailedIsProcessed() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        String body = loadFixture("webhook-status-failed.json");
        parser.parseAndProcess(body);
        verify(deliveryStatusService, times(1))
                .processDeliveryStatus(any(), eq(businessId));
    }

    @Test
    void unknownTypeDoesNotThrow() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        String body = loadFixture("webhook-unknown-type.json");
        parser.parseAndProcess(body);
        verify(inboundMessageService, times(1))
                .processInboundMessage(any(), eq(businessId), eq(channelAccountId), anyString());
    }

    @Test
    void mediaMessageIsProcessed() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        String body = loadFixture("webhook-media-image.json");
        parser.parseAndProcess(body);
        verify(inboundMessageService, times(1))
                .processInboundMessage(any(), eq(businessId), eq(channelAccountId), anyString());
    }

    @Test
    void duplicateEventIsIgnored() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.of(accountRecord));
        when(repository.insertChannelEventLog(any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(false);

        String body = loadFixture("webhook-text-message.json");
        parser.parseAndProcess(body);
        verify(inboundMessageService, never())
                .processInboundMessage(any(), any(), any(), anyString());
    }

    @Test
    void unknownPhoneNumberIdReturnsEarly() throws IOException {
        when(repository.findChannelAccountByPhoneNumberId("123456789012345"))
                .thenReturn(Optional.empty());

        String body = loadFixture("webhook-text-message.json");
        parser.parseAndProcess(body);
        verify(inboundMessageService, never())
                .processInboundMessage(any(), any(), any(), anyString());
    }
}
