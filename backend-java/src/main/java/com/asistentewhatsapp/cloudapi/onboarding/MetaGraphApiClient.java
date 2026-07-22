package com.asistentewhatsapp.cloudapi.onboarding;

import com.asistentewhatsapp.shared.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MetaGraphApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(MetaGraphApiClient.class);
    private static final String GRAPH_BASE_URL = "https://graph.facebook.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MetaGraphApiClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(GRAPH_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public MetaTokenExchangeResponse exchangeCodeForToken(
            String code, String appId, String appSecret, String redirectUri) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v23.0/oauth/access_token")
                            .queryParam("client_id", appId)
                            .queryParam("client_secret", appSecret)
                            .queryParam("code", code)
                            .queryParam("grant_type", "authorization_code")
                            .queryParam("redirect_uri", redirectUri)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.error("Meta OAuth token exchange failed: HTTP {} body={}",
                                        response.getStatusCode(), errorBody);
                                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_OAUTH_FAILED",
                                        "Error al intercambiar codigo con Meta: " + response.getStatusCode());
                            })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            String accessToken = root.has("access_token") ? root.get("access_token").asText() : null;
            long expiresIn = root.has("expires_in") ? root.get("expires_in").asLong() : 0;

            if (accessToken == null || accessToken.isBlank()) {
                LOG.error("Meta OAuth response missing access_token: {}", body);
                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_OAUTH_NO_TOKEN",
                        "Meta no devolvio un token de acceso valido.");
            }

            OffsetDateTime expiresAt = expiresIn > 0
                    ? OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn)
                    : OffsetDateTime.now(ZoneOffset.UTC).plusDays(60);

            return new MetaTokenExchangeResponse(accessToken, expiresAt);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error exchanging code with Meta", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "META_OAUTH_ERROR",
                    "Error de comunicacion con Meta al intercambiar codigo.");
        }
    }

    public MetaWabaInfo fetchWabaInfo(String accessToken, String wabaId) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v23.0/{wabaId}")
                            .queryParam("fields", "id,name,currency,timezone_id,message_template_namespace")
                            .build(wabaId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.error("Meta WABA fetch failed: {} body={}", response.getStatusCode(), errorBody);
                                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_WABA_FETCH_FAILED",
                                        "Error al consultar WABA en Meta: " + response.getStatusCode());
                            })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            String id = root.has("id") ? root.get("id").asText() : null;
            String name = root.has("name") ? root.get("name").asText() : null;

            if (id == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_WABA_INVALID",
                        "Meta no devolvio un WABA valido.");
            }

            return new MetaWabaInfo(id, name);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error fetching WABA info from Meta", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "META_WABA_ERROR",
                    "Error de comunicacion con Meta al consultar WABA.");
        }
    }

    public MetaPhoneNumberInfo fetchPhoneNumber(String accessToken, String phoneNumberId) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v23.0/{phoneNumberId}")
                            .queryParam("fields", "id,display_phone_number,verified_name,quality_rating,code_verified")
                            .build(phoneNumberId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.error("Meta phone number fetch failed: {} body={}",
                                        response.getStatusCode(), errorBody);
                                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_PHONE_FETCH_FAILED",
                                        "Error al consultar numero en Meta: " + response.getStatusCode());
                            })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            String id = root.has("id") ? root.get("id").asText() : null;
            String displayPhone = root.has("display_phone_number") ? root.get("display_phone_number").asText() : null;
            String verifiedName = root.has("verified_name") ? root.get("verified_name").asText() : null;

            if (id == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_PHONE_INVALID",
                        "Meta no devolvio un Phone Number ID valido.");
            }

            String normalized = displayPhone != null
                    ? displayPhone.replaceAll("\\D", "")
                    : "";

            return new MetaPhoneNumberInfo(id, displayPhone, normalized, verifiedName);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error fetching phone number from Meta", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "META_PHONE_ERROR",
                    "Error de comunicacion con Meta al consultar numero.");
        }
    }

    public void subscribeAppToWaba(String accessToken, String wabaId) {
        try {
            restClient.post()
                    .uri("/v23.0/{wabaId}/subscribed_apps", wabaId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.error("Meta subscribe app to WABA failed: {} body={}",
                                        response.getStatusCode(), errorBody);
                                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_SUBSCRIBE_FAILED",
                                        "Error al suscribir aplicacion al WABA: " + response.getStatusCode());
                            })
                    .body(Void.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error subscribing app to WABA", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "META_SUBSCRIBE_ERROR",
                    "Error de comunicacion con Meta al suscribir aplicacion al WABA.");
        }
    }

    public void registerPhoneNumber(String accessToken, String phoneNumberId, String pin) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("pin", pin != null ? pin : "");
            restClient.post()
                    .uri("/v23.0/{phoneNumberId}/register", phoneNumberId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.error("Meta register phone failed: {} body={}",
                                        response.getStatusCode(), errorBody);
                                throw new ApiException(HttpStatus.BAD_GATEWAY, "META_PHONE_REGISTER_FAILED",
                                        "Error al registrar numero en Meta: " + response.getStatusCode());
                            })
                    .body(Void.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error registering phone number with Meta", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "META_PHONE_REGISTER_ERROR",
                    "Error de comunicacion con Meta al registrar numero.");
        }
    }

    public void setTwoStepPin(String accessToken, String phoneNumberId, String pin) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("pin", pin);
            restClient.post()
                    .uri("/v23.0/{phoneNumberId}/two_step_pin", phoneNumberId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.warn("Meta two-step PIN set failed (non-critical): {} body={}",
                                        response.getStatusCode(), errorBody);
                            })
                    .body(Void.class);
        } catch (Exception e) {
            LOG.warn("Error setting two-step PIN (non-critical)", e);
        }
    }

    public void unsubscribeAppFromWaba(String accessToken, String wabaId) {
        try {
            restClient.delete()
                    .uri("/v23.0/{wabaId}/subscribed_apps", wabaId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                LOG.warn("Meta unsubscribe app from WABA failed: {} body={}",
                                        response.getStatusCode(), errorBody);
                            })
                    .body(Void.class);
        } catch (Exception e) {
            LOG.warn("Error unsubscribing app from WABA", e);
        }
    }

    public static String normalizePhoneNumber(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\D", "");
    }

    public record MetaTokenExchangeResponse(String accessToken, OffsetDateTime expiresAt) {}

    public record MetaWabaInfo(String id, String name) {}

    public record MetaPhoneNumberInfo(String id, String displayPhoneNumber, String normalizedPhoneNumber, String verifiedName) {}
}
