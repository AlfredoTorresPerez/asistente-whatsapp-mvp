package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.application.BookingPaymentProperties;
import com.asistentewhatsapp.bookings.domain.BookingPaymentProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.booking-payment.provider", havingValue = "MERCADOPAGO")
public class MercadoPagoPaymentProvider implements BookingPaymentProvider {

	public static final String NAME = "MERCADOPAGO";
	private static final String API_BASE = "https://api.mercadopago.com";
	private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoPaymentProvider.class);

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String accessToken;
	private final String webhookSecret;

	public MercadoPagoPaymentProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
			BookingPaymentProperties properties) {
		this.restClient = restClientBuilder.build();
		this.objectMapper = objectMapper;
		this.accessToken = properties.getMercadopago() != null ? properties.getMercadopago().getAccessToken() : "";
		this.webhookSecret = properties.getMercadopago() != null ? properties.getMercadopago().getWebhookSecret() : "";
	}

	@Override
	public String providerName() {
		return NAME;
	}

	@Override
	public CreateCheckoutResult createCheckout(UUID businessId, UUID bookingId, UUID paymentId, BigDecimal amount,
			String currency, String description, String returnUrl, String notificationUrl, int expirationMinutes) {
		if (paymentId == null) {
			throw new IllegalArgumentException("paymentId must not be null for Mercado Pago checkout");
		}
		if (notificationUrl == null || notificationUrl.isBlank()) {
			throw new IllegalArgumentException("notificationUrl must not be blank for Mercado Pago checkout");
		}
		String externalRef = paymentId.toString();

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("external_reference", externalRef);
		payload.put("expires", true);
		payload.put("expiration_date_from", OffsetDateTime.now(ZoneOffset.UTC).toString());
		payload.put("expiration_date_to", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes).toString());

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("title", description);
		item.put("quantity", 1);
		item.put("unit_price", amount.doubleValue());
		item.put("currency_id", currency);
		payload.put("items", java.util.List.of(item));

		Map<String, Object> payer = new LinkedHashMap<>();
		payer.put("email", "");
		payload.put("payer", payer);

		Map<String, Object> backUrls = new LinkedHashMap<>();
		backUrls.put("success", returnUrl);
		backUrls.put("failure", returnUrl);
		backUrls.put("pending", returnUrl);
		payload.put("back_urls", backUrls);
		payload.put("auto_return", "approved");

		payload.put("notification_url", notificationUrl);

		Map<String, Object> paymentMethods = new LinkedHashMap<>();
		paymentMethods.put("installments", 1);
		paymentMethods.put("default_installments", 1);
		paymentMethods.put("excluded_payment_types",
				java.util.List.of(Map.of("id", "ticket"), Map.of("id", "bank_transfer")));
		payload.put("payment_methods", paymentMethods);

		ResponseEntity<JsonNode> response = restClient.post().uri(API_BASE + "/checkout/preferences")
				.header("Authorization", "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON).body(payload)
				.retrieve().toEntity(JsonNode.class);
		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw new RuntimeException("Error al crear preferencia en Mercado Pago: " + response.getStatusCode());
		}
		JsonNode body = response.getBody();
		String checkoutUrl = body.has("init_point") ? body.get("init_point").asText() : null;
		String preferenceId = body.has("id") ? body.get("id").asText() : null;
		String expiresAtStr = body.has("expiration_date_to") ? body.get("expiration_date_to").asText() : null;
		OffsetDateTime expiresAt = expiresAtStr != null
				? OffsetDateTime.parse(expiresAtStr)
				: OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes);
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("mercadopagoPreferenceId", preferenceId);
		metadata.put("mercadopagoResponse", body.toString());
		return new CreateCheckoutResult(checkoutUrl, null, // providerPaymentId (se obtiene del webhook cuando se
															// aprueba)
				preferenceId, externalRef, metadata, expiresAt);
	}

	@Override
	public Optional<PaymentNotification> parseWebhook(String rawBody, Map<String, String> headers) {
		// Validar firma de Mercado Pago si está configurado
		if (webhookSecret != null && !webhookSecret.isBlank()) {
			String signature = headers.get("x-signature");
			String requestId = headers.get("x-request-id");
			if (signature == null || !verifyMercadoPagoSignature(rawBody, signature, requestId)) {
				LOGGER.warn("Mercado Pago webhook signature validation failed");
				return Optional.empty();
			}
		}

		try {
			JsonNode body = objectMapper.readTree(rawBody);
			if (body == null)
				return Optional.empty();

			String type = body.has("type") ? body.get("type").asText() : "";
			String action = body.has("action") ? body.get("action").asText() : "";

			// Notification type "payment" with action "payment.created" or
			// "payment.updated"
			if ("payment".equals(type) && body.has("data") && body.get("data").has("id")) {
				long mpPaymentId = body.get("data").get("id").asLong();
				return Optional.of(fetchPaymentInfo(mpPaymentId));
			}
			// Direct payment object (fallback)
			if (body.has("id")) {
				return Optional.of(normalizePaymentNotification(body));
			}
			return Optional.empty();
		} catch (Exception exception) {
			LOGGER.warn("MP parseWebhook error: {}", exception.getMessage());
			return Optional.empty();
		}
	}

	private boolean verifyMercadoPagoSignature(String payload, String signatureHeader, String requestId) {
		// Mercado Pago signature format: "ts=timestamp,v1=hash"
		// Expected: HMAC-SHA256(payload + "|" + requestId, webhookSecret)
		try {
			String[] parts = signatureHeader.split(",");
			String ts = null;
			String v1 = null;
			for (String part : parts) {
				if (part.startsWith("ts="))
					ts = part.substring(3);
				else if (part.startsWith("v1="))
					v1 = part.substring(3);
			}
			if (ts == null || v1 == null)
				return false;

			// Verify timestamp is recent (5 minutes tolerance)
			long timestamp = Long.parseLong(ts);
			long now = System.currentTimeMillis() / 1000;
			if (Math.abs(now - timestamp) > 300) {
				LOGGER.warn("Mercado Pago webhook timestamp out of tolerance");
				return false;
			}

			String manifest = payload + "|" + requestId;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
			byte[] hash = mac.doFinal(manifest.getBytes());
			String expected = bytesToHex(hash);
			return MessageDigest.isEqual(v1.getBytes(), expected.getBytes());
		} catch (Exception e) {
			LOGGER.warn("Mercado Pago signature verification error: {}", e.getMessage());
			return false;
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private PaymentNotification fetchPaymentInfo(long mpPaymentId) {
		ResponseEntity<JsonNode> response = restClient.get().uri(API_BASE + "/v1/payments/" + mpPaymentId)
				.header("Authorization", "Bearer " + accessToken).retrieve().toEntity(JsonNode.class);
		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw new RuntimeException("Error al obtener pago de Mercado Pago: " + mpPaymentId);
		}
		return normalizePaymentNotification(response.getBody());
	}

	private PaymentNotification normalizePaymentNotification(JsonNode payment) {
		String paymentId = payment.has("id") ? payment.get("id").asText() : null;
		String preferenceId = payment.has("preference_id") ? payment.get("preference_id").asText() : null;
		String externalRef = payment.has("external_reference") ? payment.get("external_reference").asText() : null;
		BigDecimal amount = payment.has("transaction_amount")
				? BigDecimal.valueOf(payment.get("transaction_amount").asDouble())
				: BigDecimal.ZERO;
		String currency = payment.has("currency_id") ? payment.get("currency_id").asText() : "CLP";
		String rawStatus = payment.has("status") ? payment.get("status").asText() : "";
		String statusDetail = payment.has("status_detail") ? payment.get("status_detail").asText() : null;
		String paymentMethodId = payment.has("payment_method_id") ? payment.get("payment_method_id").asText() : null;
		int installments = payment.has("installments") ? payment.get("installments").asInt() : 0;
		String payerEmail = payment.has("payer") && payment.get("payer").has("email")
				? payment.get("payer").get("email").asText()
				: null;

		String status = switch (rawStatus) {
			case "approved" -> "APPROVED";
			case "rejected", "cancelled", "charged_back" -> "REJECTED";
			case "refunded" -> "REFUNDED";
			case "expired" -> "EXPIRED";
			default -> "PENDING";
		};
		String dateApprovedStr = payment.has("date_approved") && !payment.get("date_approved").isNull()
				? payment.get("date_approved").asText()
				: null;
		OffsetDateTime occurredAt = dateApprovedStr != null
				? OffsetDateTime.parse(dateApprovedStr)
				: OffsetDateTime.now(ZoneOffset.UTC);

		String idempotencyKey = externalRef != null ? "mp:" + externalRef : "mp:" + paymentId;

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("mercadopagoPaymentId", paymentId);
		metadata.put("mercadopagoPreferenceId", preferenceId);
		metadata.put("statusDetail", statusDetail);
		metadata.put("paymentMethodId", paymentMethodId);
		metadata.put("installments", installments);
		metadata.put("payerEmail", payerEmail);

		return new PaymentNotification(paymentId, // providerPaymentId
				preferenceId, // providerPreferenceId
				externalRef, // providerExternalReference
				idempotencyKey, amount, currency, status, statusDetail, rawStatus, paymentMethodId, installments,
				payerEmail, occurredAt, metadata);
	}

	@Override
	public RefundResult refund(UUID businessId, UUID bookingId, UUID paymentId, String providerPaymentId,
			BigDecimal amount, String reason) {
		if (providerPaymentId == null || providerPaymentId.isBlank()) {
			throw new IllegalStateException(
					"No se puede reembolsar: falta el identificador real del pago en Mercado Pago. "
							+ "Solo se puede reembolsar pagos que tengan provider_payment_id asignado.");
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		if (amount != null)
			payload.put("amount", amount.doubleValue());
		if (reason != null && !reason.isBlank())
			payload.put("reason", reason);
		ResponseEntity<JsonNode> response = restClient.post()
				.uri(API_BASE + "/v1/payments/" + providerPaymentId + "/refunds")
				.header("Authorization", "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
				.body(payload.isEmpty() ? null : payload).retrieve().toEntity(JsonNode.class);
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