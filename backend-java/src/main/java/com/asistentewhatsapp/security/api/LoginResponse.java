package com.asistentewhatsapp.security.api;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds,
		long refreshExpiresInSeconds, AuthUserResponse user) {
}
