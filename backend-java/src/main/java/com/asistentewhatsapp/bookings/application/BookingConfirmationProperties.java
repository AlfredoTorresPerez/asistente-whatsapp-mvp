package com.asistentewhatsapp.bookings.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking-confirmation")
public class BookingConfirmationProperties {

	private int expirationMinutes = 60;
	private String publicBaseUrl = "http://localhost:5173/reservas/confirmar";
	private boolean dispatchWhatsApp = false;
	private int minMinutesAhead = 60;

	public int getExpirationMinutes() {
		return expirationMinutes;
	}
	public void setExpirationMinutes(int expirationMinutes) {
		this.expirationMinutes = expirationMinutes;
	}

	public String getPublicBaseUrl() {
		return sanitizePublicBaseUrl(publicBaseUrl);
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}
	public boolean isDispatchWhatsApp() {
		return dispatchWhatsApp;
	}
	public void setDispatchWhatsApp(boolean dispatchWhatsApp) {
		this.dispatchWhatsApp = dispatchWhatsApp;
	}
	public int getMinMinutesAhead() {
		return minMinutesAhead;
	}
	public void setMinMinutesAhead(int minMinutesAhead) {
		this.minMinutesAhead = minMinutesAhead;
	}

	private String sanitizePublicBaseUrl(String value) {
		if (value == null || value.isBlank()) {
			return "http://localhost:5173/reservas/confirmar";
		}
		String sanitized = value.trim();
		String[] accidentalEnvKeys = {"VITE_API_BASE_URL=", "APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES=", "TZ=",
				"JAVA_TOOL_OPTIONS=", "SPRING_JACKSON_TIME_ZONE=", "APP_TIME_ZONE="};
		for (String key : accidentalEnvKeys) {
			int index = sanitized.indexOf(key);
			if (index > 0) {
				sanitized = sanitized.substring(0, index);
			}
		}
		return sanitized.replaceAll("/+$", "");
	}
}
