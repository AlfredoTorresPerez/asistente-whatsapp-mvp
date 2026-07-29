package com.asistentewhatsapp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

	private String secret;
	private long accessTokenExpiresInSeconds = 900;
	private long refreshTokenExpiresInSeconds = 2592000;
	private long resetTokenExpiresInMinutes = 30;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getAccessTokenExpiresInSeconds() {
		return accessTokenExpiresInSeconds;
	}

	public void setAccessTokenExpiresInSeconds(long accessTokenExpiresInSeconds) {
		this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
	}

	public long getRefreshTokenExpiresInSeconds() {
		return refreshTokenExpiresInSeconds;
	}

	public void setRefreshTokenExpiresInSeconds(long refreshTokenExpiresInSeconds) {
		this.refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds;
	}

	public long getResetTokenExpiresInMinutes() {
		return resetTokenExpiresInMinutes;
	}

	public void setResetTokenExpiresInMinutes(long resetTokenExpiresInMinutes) {
		this.resetTokenExpiresInMinutes = resetTokenExpiresInMinutes;
	}
}
