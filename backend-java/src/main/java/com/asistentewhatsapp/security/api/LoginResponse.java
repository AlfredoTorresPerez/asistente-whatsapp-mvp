package com.asistentewhatsapp.security.api;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthUserResponse user) {
}

