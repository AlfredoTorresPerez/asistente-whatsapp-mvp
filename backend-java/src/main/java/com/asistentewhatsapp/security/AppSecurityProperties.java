package com.asistentewhatsapp.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

	private List<String> corsAllowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173",
			"http://localhost:4173", "http://127.0.0.1:4173");

	private List<String> corsAllowedOriginPatterns = List.of("https://*.trycloudflare.com");

	public List<String> getCorsAllowedOrigins() {
		return corsAllowedOrigins;
	}

	public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
		this.corsAllowedOrigins = corsAllowedOrigins;
	}

	public List<String> getCorsAllowedOriginPatterns() {
		return corsAllowedOriginPatterns;
	}

	public void setCorsAllowedOriginPatterns(List<String> corsAllowedOriginPatterns) {
		this.corsAllowedOriginPatterns = corsAllowedOriginPatterns;
	}
}
