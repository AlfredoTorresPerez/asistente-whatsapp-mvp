package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

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
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.channels.whatsapp-web", name = "enabled", havingValue = "true")
public class WhatsAppWebAdapter implements CanalWhatsApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppWebAdapter.class);

    private final WhatsAppWebSessionGatewayClient sessionGatewayClient;
    private final WhatsAppWebClientProperties properties;

    public WhatsAppWebAdapter(
            WhatsAppWebSessionGatewayClient sessionGatewayClient,
            WhatsAppWebClientProperties properties,
            Environment environment) {
        this.sessionGatewayClient = sessionGatewayClient;
        this.properties = properties;
        if (properties.demoFallbackEnabled() && !isLocalLikeEnvironment(environment)) {
            LOGGER.warn("APP_WHATSAPP_WEB_DEMO_FALLBACK_ENABLED esta activo fuera de un ambiente local/demo.");
        }
    }

    @Override
    public WhatsAppChannelProvider provider() {
        return WhatsAppChannelProvider.WHATSAPP_WEB;
    }

    @Override
    public WhatsAppSessionStatus getStatus() {
        WhatsAppWebSessionGatewayClient.SessionStatusResponse response = sessionGatewayClient.getStatus();
        return new WhatsAppSessionStatus(
                response.sessionId(),
                response.connectionStatus(),
                response.phoneNumber(),
                response.qrCode(),
                response.adapterMode(),
                response.lastEventAt());
    }

    @Override
    public WhatsAppSessionAction connect() {
        return toSessionAction(sessionGatewayClient.connect());
    }

    @Override
    public WhatsAppSessionAction refreshQr() {
        return toSessionAction(sessionGatewayClient.refreshQr());
    }

    @Override
    public WhatsAppSessionAction disconnect() {
        return toSessionAction(sessionGatewayClient.disconnect());
    }

    @Override
    public ChannelDelivery send(OutboundMessage outboundMessage) {
        try {
            WhatsAppWebSessionGatewayClient.SendTextResponse response = sessionGatewayClient.sendText(
                    outboundMessage.businessId().toString(),
                    outboundMessage.recipientPhone(),
                    outboundMessage.body());

            return new ChannelDelivery(
                    MessageChannelType.WHATSAPP,
                    response.messageId(),
                    response.status(),
                    response.acceptedAt() == null ? Instant.now() : response.acceptedAt().toInstant());
        } catch (MessagingChannelUnavailableException exception) {
            if (properties.demoFallbackEnabled()) {
                return new ChannelDelivery(
                        MessageChannelType.WHATSAPP,
                        "demo-whatsapp-web-" + UUID.randomUUID(),
                        "SIMULATED",
                        Instant.now());
            }

            throw exception;
        }
    }

    private WhatsAppSessionAction toSessionAction(WhatsAppWebSessionGatewayClient.SessionActionResponse response) {
        OffsetDateTime acceptedAt = response.acceptedAt() == null ? OffsetDateTime.now() : response.acceptedAt();
        return new WhatsAppSessionAction(
                response.sessionId(),
                response.connectionStatus(),
                response.phoneNumber(),
                response.qrCode(),
                acceptedAt);
    }

    private boolean isLocalLikeEnvironment(Environment environment) {
        String appEnvironment = environment.getProperty("app.environment", "local").toLowerCase(Locale.ROOT);
        return appEnvironment.equals("local")
                || appEnvironment.equals("dev")
                || appEnvironment.equals("development")
                || appEnvironment.equals("demo")
                || appEnvironment.equals("test");
    }
}
