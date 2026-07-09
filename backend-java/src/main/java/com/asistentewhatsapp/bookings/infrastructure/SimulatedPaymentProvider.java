package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.domain.BookingPaymentProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentProvider implements BookingPaymentProvider {

    public static final String NAME = "SIMULATED";

    @Override
    public String providerName() { return NAME; }

    @Override
    public CreateCheckoutResult createCheckout(
            UUID businessId, UUID bookingId, UUID paymentId,
            BigDecimal amount, String currency, String description,
            String returnUrl, String notificationUrl, int expirationMinutes) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null for simulated checkout");
        }
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(expirationMinutes);
        String checkoutUrl = notificationUrl != null && notificationUrl.contains("/webhook")
                ? notificationUrl.replace("/webhook", "/pagar/" + paymentId)
                : (returnUrl != null ? returnUrl : "http://localhost:5173/reservas/pagar") + "/" + paymentId;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("simulated", true);
        metadata.put("returnUrl", returnUrl);
        metadata.put("notificationUrl", notificationUrl);
        String externalRef = "sim-" + paymentId.toString();
        return new CreateCheckoutResult(checkoutUrl, paymentId.toString(), "sim-pref-" + paymentId, externalRef, metadata, expiresAt);
    }

    @Override
    public Optional<PaymentNotification> parseWebhook(String rawBody, Map<String, String> headers) {
        return Optional.empty();
    }

    @Override
    public RefundResult refund(UUID businessId, UUID bookingId, UUID paymentId,
            String providerPaymentId, BigDecimal amount, String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("simulated", true);
        metadata.put("reason", reason);
        return new RefundResult("refund-simulated-" + paymentId, metadata);
    }

    @Override
    public boolean supportsWebhook() { return false; }
}