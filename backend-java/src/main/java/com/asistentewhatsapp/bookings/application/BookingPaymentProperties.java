package com.asistentewhatsapp.bookings.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking-payment")
public class BookingPaymentProperties {

    private boolean webhookSignatureEnabled = false;
    private String webhookSecret = "";
    private long webhookToleranceSeconds = 300;
    private String checkoutPublicBaseUrl = "http://localhost:5173/reservas/pagar";
    private int checkoutExpirationMinutes = 30;
    private boolean dispatchWhatsApp = false;
    private boolean dispatchEmail = true;
    private long expirationScanMs = 60000;
    private int expirationBatchSize = 100;
    private String externalCheckoutUrlTemplate = "";

    public boolean isWebhookSignatureEnabled() {
        return webhookSignatureEnabled;
    }

    public void setWebhookSignatureEnabled(boolean webhookSignatureEnabled) {
        this.webhookSignatureEnabled = webhookSignatureEnabled;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public long getWebhookToleranceSeconds() {
        return webhookToleranceSeconds;
    }

    public void setWebhookToleranceSeconds(long webhookToleranceSeconds) {
        this.webhookToleranceSeconds = webhookToleranceSeconds;
    }

    public String getCheckoutPublicBaseUrl() {
        return checkoutPublicBaseUrl;
    }

    public void setCheckoutPublicBaseUrl(String checkoutPublicBaseUrl) {
        this.checkoutPublicBaseUrl = checkoutPublicBaseUrl;
    }

    public int getCheckoutExpirationMinutes() {
        return checkoutExpirationMinutes;
    }

    public void setCheckoutExpirationMinutes(int checkoutExpirationMinutes) {
        this.checkoutExpirationMinutes = checkoutExpirationMinutes;
    }

    public boolean isDispatchWhatsApp() {
        return dispatchWhatsApp;
    }

    public void setDispatchWhatsApp(boolean dispatchWhatsApp) {
        this.dispatchWhatsApp = dispatchWhatsApp;
    }

    public boolean isDispatchEmail() {
        return dispatchEmail;
    }

    public void setDispatchEmail(boolean dispatchEmail) {
        this.dispatchEmail = dispatchEmail;
    }

    public long getExpirationScanMs() {
        return expirationScanMs;
    }

    public void setExpirationScanMs(long expirationScanMs) {
        this.expirationScanMs = expirationScanMs;
    }

    public int getExpirationBatchSize() {
        return expirationBatchSize;
    }

    public void setExpirationBatchSize(int expirationBatchSize) {
        this.expirationBatchSize = expirationBatchSize;
    }

    public String getExternalCheckoutUrlTemplate() {
        return externalCheckoutUrlTemplate;
    }

    public void setExternalCheckoutUrlTemplate(String externalCheckoutUrlTemplate) {
        this.externalCheckoutUrlTemplate = externalCheckoutUrlTemplate;
    }
}
