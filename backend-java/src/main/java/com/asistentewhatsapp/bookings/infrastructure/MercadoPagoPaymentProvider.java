package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.domain.BookingPaymentProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.booking-payment.provider", havingValue = "MERCADOPAGO")
public class MercadoPagoPaymentProvider implements BookingPaymentProvider {

    private static final String NAME = "MERCADOPAGO";
    private static final String API_BASE = "https://api.mercadopago.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final boolean webhookSignatureEnabled;
    private final String webhookSecret;
    private final String notificationUrl;

    public MercadoPagoPaymentProvider(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.booking-payment.mercadopago.access-token:}") String accessToken,
            @Value("${app.booking-payment.webhook-signature-enabled:false}") boolean webhookSignatureEnabled,
            @Value("${app.booking-payment.webhook-secret:}") String webhookSecret,
            @Value("${app.booking-payment.mercadopago.notification-url:}") String notificationUrl) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.accessToken = accessToken;
        this.webhookSignatureEnabled = webhookSignatureEnabled;
        this.webhookSecret = webhookSecret;
        this.notificationUrl = notificationUrl;
    }

    @Override
    public String providerName() {
        return NAME;
    }

    @Override
    public CreateCheckoutResult createCheckout(UUID businessId, UUID bookingId, UUID paymentId, BigDecimal amount, String currency, String description, String returnUrl, String notificationUrl, int expirationMinutes) {
        String resolvedNotificationUrl = notificationUrl != null && !notificationUrl.isBlank()
                ? notificationUrl
                : this.notificationUrl;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("external_reference", paymentId.toString());
        payload.put("expires", true);
        payload.put("expiration_date_from", OffsetDateTime.now(ZoneOffset.UTC).toString());
        payload.put("expiration_date_to", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes).toString());
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("title", description);
        items.put("quantity", 1);
        items.put("unit_price", amount.doubleValue());
        items.put("currency_id", currency);
        payload.put("items", java.util.List.of(items));
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", "");
        payload.put("payer", payer);
        Map<String, Object> backUrls = new LinkedHashMap<>();
        backUrls.put("success", returnUrl);
        backUrls.put("failure", returnUrl);
        backUrls.put("pending", returnUrl);
        payload.put("back_urls", backUrls);
        payload.put("auto_return", "approved");
        if (resolvedNotificationUrl != null && !resolvedNotificationUrl.isBlank()) {
            payload.put("notification_url", resolvedNotificationUrl);
        }
        Map<String, Object> paymentMethods = new LinkedHashMap<>();
        paymentMethods.put("installments", 1);
        paymentMethods.put("default_installments", 1);
        paymentMethods.put("excluded_payment_types", java.util.List.of(Map.of("id", "ticket"), Map.of("id", "bank_transfer")));
        payload.put("payment_methods", paymentMethods);

        ResponseEntity<JsonNode> response = restClient.post()
                .uri(API_BASE + "/checkout/preferences")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error al crear preferencia en Mercado Pago: " + response.getStatusCode());
        }
        JsonNode body = response.getBody();
        String checkoutUrl = body.has("init_point") ? body.get("init_point").asText() : null;
        String preferenceId = body.has("id") ? body.get("id").asText() : null;
        String expiresAtStr = body.has("expiration_date_to") ? body.get("expiration_date_to").asText() : null;
        OffsetDateTime expiresAt = expiresAtStr != null ? OffsetDateTime.parse(expiresAtStr) : OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("preferenceId", preferenceId);
        metadata.put("mercadopagoResponse", body.toString());
        return new CreateCheckoutResult(checkoutUrl, preferenceId, metadata, expiresAt);
    }

    @Override
    public Optional<PaymentNotification> parseWebhook(String rawBody, Map<String, String> headers) {
        try {
            JsonNode body = objectMapper.readTree(rawBody);
            if (body == null) {
                return Optional.empty();
            }
            if (body.has("type") && "payment".equals(body.get("type").asText())) {
                long paymentId = body.get("data").get("id").asLong();
                return Optional.of(fetchPaymentInfo(paymentId));
            }
            if (body.has("action") && "payment.created".equals(body.get("action").asText())) {
                long paymentId = body.get("data").get("id").asLong();
                return Optional.of(fetchPaymentInfo(paymentId));
            }
            if (body.has("id")) {
                return Optional.of(normalizePaymentNotification(body));
            }
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private PaymentNotification fetchPaymentInfo(long paymentId) {
        ResponseEntity<JsonNode> response = restClient.get()
                .uri(API_BASE + "/v1/payments/" + paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toEntity(JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error al obtener pago de Mercado Pago: " + paymentId);
        }
        return normalizePaymentNotification(response.getBody());
    }

    private PaymentNotification normalizePaymentNotification(JsonNode payment) {
        String paymentId = payment.has("id") ? payment.get("id").asText() : null;
        String externalRef = payment.has("external_reference") ? payment.get("external_reference").asText() : null;
        BigDecimal amount = payment.has("transaction_amount") ? BigDecimal.valueOf(payment.get("transaction_amount").asDouble()) : BigDecimal.ZERO;
        String currency = payment.has("currency_id") ? payment.get("currency_id").asText() : "CLP";
        String status = switch (payment.has("status") ? payment.get("status").asText() : "") {
            case "approved" -> "APPROVED";
            case "rejected", "cancelled", "charged_back" -> "REJECTED";
            case "refunded" -> "REFUNDED";
            case "expired" -> "EXPIRED";
            default -> "PENDING";
        };
        String dateApprovedStr = payment.has("date_approved") && !payment.get("date_approved").isNull()
                ? payment.get("date_approved").asText() : null;
        OffsetDateTime occurredAt = dateApprovedStr != null
                ? OffsetDateTime.parse(dateApprovedStr)
                : OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("paymentId", paymentId);
        metadata.put("statusDetail", payment.has("status_detail") ? payment.get("status_detail").asText() : null);
        metadata.put("paymentMethodId", payment.has("payment_method_id") ? payment.get("payment_method_id").asText() : null);
        metadata.put("installments", payment.has("installments") ? payment.get("installments").asInt() : null);
        metadata.put("payerEmail", payment.has("payer") && payment.get("payer").has("email") ? payment.get("payer").get("email").asText() : null);
        String idempotencyKey = externalRef != null ? "mp:" + externalRef : "mp:" + paymentId;
        return new PaymentNotification(paymentId, idempotencyKey, amount, currency, status, occurredAt, metadata);
    }

    @Override
    public RefundResult refund(UUID businessId, UUID bookingId, UUID paymentId, String providerPaymentId, BigDecimal amount, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (amount != null) {
            payload.put("amount", amount.doubleValue());
        }
        if (reason != null && !reason.isBlank()) {
            payload.put("reason", reason);
        }
        ResponseEntity<JsonNode> response = restClient.post()
                .uri(API_BASE + "/v1/payments/" + providerPaymentId + "/refunds")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload.isEmpty() ? null : payload)
                .retrieve()
                .toEntity(JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error al reembolsar en Mercado Pago: " + response.getStatusCode());
        }
        JsonNode body = response.getBody();
        String refundId = body.has("id") ? body.get("id").asText() : null;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("refundId", refundId);
        metadata.put("mercadopagoResponse", body.toString());
        return new RefundResult(refundId, metadata);
    }

    @Override
    public boolean supportsWebhook() {
        return true;
    }
}
