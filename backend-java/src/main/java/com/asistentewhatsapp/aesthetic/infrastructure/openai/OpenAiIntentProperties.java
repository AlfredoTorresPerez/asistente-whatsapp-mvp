package com.asistentewhatsapp.aesthetic.infrastructure.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.openai")
public record OpenAiIntentProperties(boolean enabled, String baseUrl, String apiKey, String model, int timeoutSeconds) {

	public String resolvedBaseUrl() {
		return isBlank(baseUrl) ? "https://api.openai.com/v1/responses" : baseUrl;
	}

	public String resolvedModel() {
		return isBlank(model) ? "gpt-5.4-mini" : model;
	}

	public int resolvedTimeoutSeconds() {
		return timeoutSeconds <= 0 ? 30 : timeoutSeconds;
	}

	public boolean hasApiKey() {
		return !isBlank(apiKey);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
