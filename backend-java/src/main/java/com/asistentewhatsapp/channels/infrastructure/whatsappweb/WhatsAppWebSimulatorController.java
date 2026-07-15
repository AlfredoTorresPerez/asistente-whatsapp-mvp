package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import com.asistentewhatsapp.shared.api.StatusResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name = "app.whatsapp-web.simulator-enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping(value = "/api/v1/test", produces = MediaType.APPLICATION_JSON_VALUE)
public class WhatsAppWebSimulatorController {

    private final WhatsAppWebWebhookService whatsAppWebWebhookService;
    private final WhatsAppWebChannelJdbcRepository repository;
    private final WhatsAppWebClientProperties properties;
    private final ObjectMapper objectMapper;

    public WhatsAppWebSimulatorController(
            WhatsAppWebWebhookService whatsAppWebWebhookService,
            WhatsAppWebChannelJdbcRepository repository,
            WhatsAppWebClientProperties properties,
            ObjectMapper objectMapper) {
        this.whatsAppWebWebhookService = whatsAppWebWebhookService;
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/whatsapp-inbound")
    public StatusResponse simulateInbound(@RequestBody DemoIncomingMessageRequest request) {
        String sessionKey = resolveSessionKey(request);
        String from = request.from().startsWith("+") ? request.from() : "+" + request.from();

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("from", from);
            payload.put("body", request.body());
            payload.put("externalMessageId", request.externalMessageId() != null
                    ? request.externalMessageId() : "sim-" + UUID.randomUUID());
            payload.put("to", "");

            String deliveryId = "sim-" + UUID.randomUUID();
            String occurredAt = OffsetDateTime.now(ZoneOffset.UTC).toString();

            ObjectNode root = objectMapper.createObjectNode();
            root.put("eventType", "MESSAGE_RECEIVED");
            root.put("deliveryId", deliveryId);
            root.put("occurredAt", occurredAt);
            root.put("sessionKey", sessionKey);
            root.set("payload", payload);

            String rawBody = objectMapper.writeValueAsString(root);
            String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
            String signature = computeSignature(timestamp, rawBody);

            return whatsAppWebWebhookService.handleWebhook(rawBody, timestamp, signature, deliveryId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error simulando mensaje entrante: " + e.getMessage(), e);
        }
    }

    private String resolveSessionKey(DemoIncomingMessageRequest request) {
        if (request.sessionKey() != null && !request.sessionKey().isBlank()) {
            return request.sessionKey();
        }
        var channel = repository.findFirstChannelAccount();
        if (channel.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay canales activos para simular mensajes. Configure un canal WhatsApp Web primero.");
        }
        return channel.get().sessionKey();
    }

    private String computeSignature(String timestamp, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + java.util.HexFormat.of().formatHex(signature);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC", e);
        }
    }
}
