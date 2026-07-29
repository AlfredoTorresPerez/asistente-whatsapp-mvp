package com.asistentewhatsapp.bookings.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking-payment")
public class BookingPaymentProperties {

	private String provider = "SIMULATED";
	private boolean webhookSignatureEnabled = true;
	private String webhookSecret = "";
	private long webhookToleranceSeconds = 300;
	private String checkoutPublicBaseUrl = "http://localhost:5173/reservas/pagar";
	private String webhookPublicUrl = "";
	private int checkoutExpirationMinutes = 30;
	private boolean dispatchWhatsApp = false;
	private boolean dispatchEmail = true;
	private boolean dispatchPostPaymentWhatsApp = false;
	private boolean dispatchPostPaymentEmail = true;
	private long expirationScanMs = 60000;
	private int expirationBatchSize = 100;
	private String externalCheckoutUrlTemplate = "";
	private MercadoPagoProperties mercadopago = new MercadoPagoProperties();

	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
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
	public String getWebhookPublicUrl() {
		return webhookPublicUrl;
	}
	public void setWebhookPublicUrl(String webhookPublicUrl) {
		this.webhookPublicUrl = webhookPublicUrl;
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
	public boolean isDispatchPostPaymentWhatsApp() {
		return dispatchPostPaymentWhatsApp;
	}
	public void setDispatchPostPaymentWhatsApp(boolean dispatchPostPaymentWhatsApp) {
		this.dispatchPostPaymentWhatsApp = dispatchPostPaymentWhatsApp;
	}
	public boolean isDispatchPostPaymentEmail() {
		return dispatchPostPaymentEmail;
	}
	public void setDispatchPostPaymentEmail(boolean dispatchPostPaymentEmail) {
		this.dispatchPostPaymentEmail = dispatchPostPaymentEmail;
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
	public MercadoPagoProperties getMercadopago() {
		return mercadopago;
	}
	public void setMercadopago(MercadoPagoProperties mercadopago) {
		this.mercadopago = mercadopago;
	}

	public static class MercadoPagoProperties {
		private String accessToken = "";
		private String webhookSecret = "";
		private String notificationUrl = "";

		public String getAccessToken() {
			return accessToken;
		}
		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}
		public String getWebhookSecret() {
			return webhookSecret;
		}
		public void setWebhookSecret(String webhookSecret) {
			this.webhookSecret = webhookSecret;
		}
		public String getNotificationUrl() {
			return notificationUrl;
		}
		public void setNotificationUrl(String notificationUrl) {
			this.notificationUrl = notificationUrl;
		}
	}
}