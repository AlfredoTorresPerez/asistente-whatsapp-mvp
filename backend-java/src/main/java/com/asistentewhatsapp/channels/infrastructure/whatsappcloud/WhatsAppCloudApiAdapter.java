package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppChannelProvider;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionAction;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionStatus;
import com.asistentewhatsapp.cloudapi.infrastructure.CloudApiTokenEncryptionService;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository.ChannelAccountRecord;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
public class WhatsAppCloudApiAdapter implements CanalWhatsApp {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppCloudApiAdapter.class);

    private final WhatsAppCloudApiProperties properties;
    private final RestClient restClient;
    private final WhatsAppCloudApiMetrics metrics;
    private final ObjectMapper objectMapper;
    private final MetaOnboardingRepository onboardingRepository;
    private final CloudApiTokenEncryptionService tokenEncryption;

    public WhatsAppCloudApiAdapter(
            RestClient.Builder restClientBuilder,
            WhatsAppCloudApiProperties properties,
            WhatsAppCloudApiMetrics metrics,
            ObjectMapper objectMapper,
            MetaOnboardingRepository onboardingRepository,
            CloudApiTokenEncryptionService tokenEncryption) {
        this.properties = properties;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.onboardingRepository = onboardingRepository;
        this.tokenEncryption = tokenEncryption;
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(properties.baseUrl()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(createRequestFactory())
                .build();
    }

    @Override
    public WhatsAppChannelProvider provider() {
        return WhatsAppChannelProvider.META_CLOUD_API;
    }

    @Override
    public WhatsAppSessionStatus getStatus() {
        String adapterMode = properties.dryRunEnabled() ? "WHATSAPP_CLOUD_API_DRY_RUN" : "META_CLOUD_API_CLOUD_API";
        return new WhatsAppSessionStatus(
                "cloud-api",
                "CONNECTED",
                properties.defaultPhoneNumber() != null ? properties.defaultPhoneNumber() : "Multi-tenant",
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
            metrics.incrementMessagesSent();
            return new ChannelDelivery(
                    MessageChannelType.WHATSAPP,
                    "cloud-dry-run-" + UUID.randomUUID(),
                    "SIMULATED",
                    Instant.now());
        }

        String recipient = normalizePhone(outboundMessage.recipientPhone());
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacio.");
        }
        if (outboundMessage.body() == null || outboundMessage.body().isBlank()) {
            throw new IllegalArgumentException("El cuerpo del mensaje no puede estar vacio.");
        }

        UUID businessId = outboundMessage.businessId();
        ChannelCredentials credentials = resolveCredentials(businessId);

        long startTime = System.currentTimeMillis();
        try {
            CloudApiSendMessageResponse response = restClient.post()
                    .uri("/{apiVersion}/{phoneNumberId}/messages",
                            properties.apiVersion(), credentials.phoneNumberId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials.accessToken())
                    .body(buildTextBody(recipient, outboundMessage.body(), null))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                        long elapsed = System.currentTimeMillis() - startTime;
                        metrics.recordApiCall(elapsed, false);
                        String errorBody = new String(resp.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        int statusCode = resp.getStatusCode().value();
                        String metaErrorCode = extractMetaErrorCode(errorBody);
                        String errorType = statusCode == 429 ? "rate_limited"
                                : statusCode == 401 ? "unauthorized"
                                : statusCode == 400 ? "bad_request"
                                : "client_error_" + statusCode;
                        metrics.incrementApiErrors(errorType);
                        LOG.warn("WhatsApp Cloud API {} error for business {}: HTTP {} metaCode={}",
                                errorType, businessId, statusCode, metaErrorCode != null ? metaErrorCode : "unknown");
                        if (statusCode == 429) {
                            throw new MessagingChannelUnavailableException(
                                    "Rate limit excedido por WhatsApp Cloud API. Reintentar mas tarde.");
                        }
                        throw new MessagingChannelUnavailableException(
                                "WhatsApp Cloud API rechazo el mensaje: HTTP " + statusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, resp) -> {
                        long elapsed = System.currentTimeMillis() - startTime;
                        metrics.recordApiCall(elapsed, false);
                        String errorBody = new String(resp.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        int statusCode = resp.getStatusCode().value();
                        metrics.incrementApiErrors("server_error_" + statusCode);
                        String retryAfter = resp.getHeaders().getFirst("Retry-After");
                        if (retryAfter != null) {
                            LOG.warn("WhatsApp Cloud API {} error for business {}, Retry-After: {}s", statusCode, businessId, retryAfter);
                        }
                        throw new MessagingChannelUnavailableException(
                                "Error del servidor de WhatsApp Cloud API. Reintentar mas tarde.");
                    })
                    .body(CloudApiSendMessageResponse.class);

            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordApiCall(elapsed, true);
            metrics.incrementMessagesSent();

            String externalMessageId = response != null && response.messages() != null && !response.messages().isEmpty()
                    ? response.messages().getFirst().id()
                    : null;

            if (externalMessageId == null || externalMessageId.isBlank()) {
                LOG.error("WhatsApp Cloud API responded without a message ID for business {}", businessId);
                metrics.incrementApiErrors("missing_message_id");
                throw new MessagingChannelUnavailableException(
                        "WhatsApp Cloud API no devolvio un identificador de mensaje valido.");
            }

            LOG.info("Message sent via Cloud API for business {}: externalMessageId={}",
                    businessId, externalMessageId);

            return new ChannelDelivery(
                    MessageChannelType.WHATSAPP,
                    externalMessageId,
                    "PROVIDER_ACCEPTED",
                    Instant.now());
        } catch (MessagingChannelUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordApiCall(elapsed, false);
            metrics.incrementApiErrors("unexpected_error");
            throw new MessagingChannelUnavailableException(
                    "No fue posible entregar el mensaje a WhatsApp Cloud API.");
        }
    }

    public String sendTemplate(
            UUID businessId,
            String recipientPhone,
            String templateName,
            String languageCode,
            List<TemplateComponent> components,
            String contextMessageId) {
        if (properties.dryRunEnabled()) {
            metrics.incrementMessagesSent();
            return "cloud-dry-run-template-" + UUID.randomUUID();
        }

        String recipient = normalizePhone(recipientPhone);
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacio.");
        }
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la plantilla no puede estar vacio.");
        }

        ChannelCredentials credentials = resolveCredentials(businessId);

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> body = buildTemplateBody(recipient, templateName,
                    languageCode != null ? languageCode : "es",
                    components, contextMessageId);

            CloudApiSendMessageResponse response = restClient.post()
                    .uri("/{apiVersion}/{phoneNumberId}/messages",
                            properties.apiVersion(), credentials.phoneNumberId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials.accessToken())
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                        long elapsed = System.currentTimeMillis() - startTime;
                        metrics.recordApiCall(elapsed, false);
                        metrics.incrementApiErrors("template_send_error");
                        throw new MessagingChannelUnavailableException(
                                "WhatsApp Cloud API rechazo la plantilla.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, resp) -> {
                        long elapsed = System.currentTimeMillis() - startTime;
                        metrics.recordApiCall(elapsed, false);
                        metrics.incrementApiErrors("template_server_error");
                        throw new MessagingChannelUnavailableException(
                                "Error del servidor al enviar plantilla.");
                    })
                    .body(CloudApiSendMessageResponse.class);

            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordApiCall(elapsed, true);
            metrics.incrementMessagesSent();

            if (response == null || response.messages() == null || response.messages().isEmpty()) {
                metrics.incrementApiErrors("missing_template_message_id");
                throw new MessagingChannelUnavailableException(
                        "WhatsApp Cloud API no devolvio un ID de mensaje de plantilla.");
            }

            LOG.info("Template sent via Cloud API for business {}: template={}", businessId, templateName);

            return response.messages().getFirst().id();
        } catch (MessagingChannelUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            long elapsed = System.currentTimeMillis() - startTime;
            metrics.recordApiCall(elapsed, false);
            metrics.incrementApiErrors("template_unexpected_error");
            throw new MessagingChannelUnavailableException(
                    "No fue posible enviar la plantilla a WhatsApp Cloud API.");
        }
    }

    private record ChannelCredentials(String phoneNumberId, String accessToken) {}

    private ChannelCredentials resolveCredentials(UUID businessId) {
        ChannelAccountRecord channel = onboardingRepository.findCloudApiChannel(businessId)
                .orElseThrow(() -> new MessagingChannelUnavailableException(
                        "No hay canal META_CLOUD_API activo configurado para la empresa " + businessId));

        if (channel.phoneNumberId() == null || channel.phoneNumberId().isBlank()) {
            throw new MessagingChannelUnavailableException(
                    "El canal META_CLOUD_API de la empresa " + businessId + " no tiene phone_number_id configurado.");
        }

        if (channel.encryptedAccessToken() == null || channel.encryptedAccessToken().isBlank()) {
            throw new MessagingChannelUnavailableException(
                    "El canal META_CLOUD_API de la empresa " + businessId + " no tiene token de acceso configurado.");
        }

        String decryptedToken;
        try {
            decryptedToken = tokenEncryption.decrypt(channel.encryptedAccessToken());
        } catch (Exception e) {
            LOG.error("Failed to decrypt access token for business {}", businessId);
            throw new MessagingChannelUnavailableException(
                    "No se pudo descifrar el token de acceso de la empresa " + businessId);
        }

        return new ChannelCredentials(channel.phoneNumberId(), decryptedToken);
    }

    private Map<String, Object> buildTextBody(String recipient, String text, String contextMessageId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", recipient);
        body.put("type", "text");
        body.put("text", Map.of("body", text));
        if (contextMessageId != null && !contextMessageId.isBlank()) {
            body.put("context", Map.of("message_id", contextMessageId));
        }
        return body;
    }

    private Map<String, Object> buildTemplateBody(
            String recipient,
            String templateName,
            String languageCode,
            List<TemplateComponent> components,
            String contextMessageId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", recipient);
        body.put("type", "template");

        Map<String, Object> template = new java.util.LinkedHashMap<>();
        template.put("name", templateName);

        Map<String, Object> language = new java.util.LinkedHashMap<>();
        language.put("code", languageCode);
        template.put("language", language);

        if (components != null && !components.isEmpty()) {
            List<Map<String, Object>> componentList = new ArrayList<>();
            for (TemplateComponent component : components) {
                Map<String, Object> comp = new java.util.LinkedHashMap<>();
                comp.put("type", component.type());
                if (component.parameters() != null && !component.parameters().isEmpty()) {
                    List<Map<String, Object>> params = new ArrayList<>();
                    for (TemplateParameter param : component.parameters()) {
                        Map<String, Object> p = new java.util.LinkedHashMap<>();
                        p.put("type", param.type());
                        switch (param.type()) {
                            case "text" -> p.put("text", param.value());
                            case "image" -> p.put("image", Map.of("id", param.value()));
                            case "document" -> p.put("document", Map.of("id", param.value()));
                            case "currency" -> {
                                Map<String, Object> currency = new java.util.LinkedHashMap<>();
                                currency.put("fallback_value", param.value());
                                currency.put("code", param.parameterName() != null ? param.parameterName() : "CLP");
                                currency.put("amount_1000", param.value());
                                p.put("currency", currency);
                            }
                            case "date_time" -> {
                                Map<String, Object> dateTime = new java.util.LinkedHashMap<>();
                                dateTime.put("fallback_value", param.value());
                                p.put("date_time", dateTime);
                            }
                            default -> p.put("text", param.value());
                        }
                        params.add(p);
                    }
                    comp.put("parameters", params);
                }
                componentList.add(comp);
            }
            template.put("components", componentList);
        }

        body.put("template", template);

        if (contextMessageId != null && !contextMessageId.isBlank()) {
            body.put("context", Map.of("message_id", contextMessageId));
        }

        return body;
    }

    private String extractMetaErrorCode(String errorBody) {
        try {
            JsonNode node = objectMapper.readTree(errorBody);
            JsonNode error = node.get("error");
            if (error != null && error.has("code")) {
                return error.get("code").asText();
            }
            if (error != null && error.has("error_subcode")) {
                return error.get("error_subcode").asText();
            }
        } catch (Exception exception) {
            // ignore parse errors
        }
        return null;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return hasText(baseUrl) ? baseUrl : "https://graph.facebook.com";
    }

    private static String normalizePhone(String rawPhone) {
        return rawPhone == null ? "" : rawPhone.replaceAll("\\D", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int connectTimeout = properties.connectTimeoutSeconds() > 0
                ? properties.connectTimeoutSeconds() * 1000
                : 5000;
        int readTimeout = properties.readTimeoutSeconds() > 0
                ? properties.readTimeoutSeconds() * 1000
                : 15000;
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        return factory;
    }

    public record TemplateComponent(
            String type,
            List<TemplateParameter> parameters) {
    }

    public record TemplateParameter(
            String type,
            String value,
            String parameterName) {
    }

    private record CloudApiSendMessageResponse(List<CloudApiMessage> messages) {
    }

    private record CloudApiMessage(String id) {
    }
}
